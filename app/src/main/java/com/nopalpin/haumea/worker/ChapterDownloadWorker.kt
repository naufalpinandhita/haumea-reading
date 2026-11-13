package com.nopalpin.haumea.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nopalpin.haumea.data.api.NetworkModule
import com.nopalpin.haumea.data.local.DownloadDatabaseHelper
import com.nopalpin.haumea.data.local.DownloadEntity
import com.nopalpin.haumea.data.local.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class ChapterDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val dbHelper = DownloadDatabaseHelper.getInstance(appContext)
    // Reuse shared OkHttpClient with connection pooling
    private val okHttpClient = NetworkModule.okHttpClient

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val chapterId = inputData.getString("chapter_id") ?: return@withContext Result.failure()
        val comicSlug = inputData.getString("comic_slug") ?: ""
        val comicTitle = inputData.getString("comic_title") ?: ""
        val comicCover = inputData.getString("comic_cover") ?: ""
        val comicType = inputData.getString("comic_type") ?: ""
        val chapterNumber = inputData.getString("chapter_number") ?: ""
        val chapterTitle = inputData.getString("chapter_title") ?: ""

        // Use universal Android external files directory accessible via File Manager apps:
        // Path: Internal Storage / Android / data / com.nopalpin.haumea / files / downloads / {comicSlug} / {chapterId}
        val baseDir = applicationContext.getExternalFilesDir("downloads") ?: applicationContext.filesDir
        val targetDir = File(baseDir, "$comicSlug/$chapterId")
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val initialEntity = DownloadEntity(
            chapterId = chapterId,
            comicSlug = comicSlug,
            comicTitle = comicTitle,
            comicCover = comicCover,
            comicType = comicType,
            chapterNumber = chapterNumber,
            chapterTitle = chapterTitle,
            status = DownloadStatus.DOWNLOADING.name,
            progressPages = 0,
            totalPages = 0,
            localDirectory = targetDir.absolutePath
        )
        dbHelper.insertOrUpdate(initialEntity)

        try {
            val pagesResponse = NetworkModule.apiService.getChapterPages(chapterId)
            val pages = pagesResponse.data.pages

            if (pages.isEmpty()) {
                dbHelper.insertOrUpdate(initialEntity.copy(status = DownloadStatus.FAILED.name))
                return@withContext Result.failure()
            }

            val total = pages.size
            dbHelper.updateProgress(chapterId, DownloadStatus.DOWNLOADING.name, 0, total)

            // Concurrency: 3 parallel downloads for high speed with low network/memory footprint
            val semaphore = Semaphore(3)
            val progressCounter = AtomicInteger(0)

            val downloadJobs = pages.mapIndexed { index, imageUrl ->
                async {
                    if (isStopped) return@async false
                    semaphore.withPermit {
                        if (isStopped) return@withPermit false

                        val fileName = String.format("%03d.jpg", index + 1)
                        val file = File(targetDir, fileName)

                        // If already downloaded and valid, skip re-downloading
                        if (file.exists() && file.length() > 0) {
                            val current = progressCounter.incrementAndGet()
                            dbHelper.updateProgress(chapterId, DownloadStatus.DOWNLOADING.name, current, total)
                            return@withPermit true
                        }

                        val request = Request.Builder()
                            .url(imageUrl)
                            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                            .addHeader("Referer", "https://komiku.org/")
                            .build()

                        try {
                            okHttpClient.newCall(request).execute().use { response ->
                                if (response.isSuccessful) {
                                    val body = response.body
                                    if (body != null) {
                                        // Stream directly using small memory buffer (keringanan / lightweight)
                                        body.byteStream().use { inputStream ->
                                            file.outputStream().use { outputStream ->
                                                inputStream.copyTo(outputStream)
                                            }
                                        }
                                        val current = progressCounter.incrementAndGet()
                                        dbHelper.updateProgress(chapterId, DownloadStatus.DOWNLOADING.name, current, total)
                                        return@withPermit true
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        return@withPermit false
                    }
                }
            }

            val results = downloadJobs.awaitAll()
            if (isStopped) {
                return@withContext Result.retry()
            }

            val successfulCount = results.count { it }
            if (successfulCount == 0) {
                val failedEntity = initialEntity.copy(status = DownloadStatus.FAILED.name)
                dbHelper.insertOrUpdate(failedEntity)
                return@withContext Result.failure()
            }

            val completedEntity = initialEntity.copy(
                status = DownloadStatus.COMPLETED.name,
                progressPages = total,
                totalPages = total,
                localDirectory = targetDir.absolutePath
            )
            dbHelper.insertOrUpdate(completedEntity)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            val failedEntity = initialEntity.copy(status = DownloadStatus.FAILED.name)
            dbHelper.insertOrUpdate(failedEntity)
            Result.failure()
        }
    }
}
