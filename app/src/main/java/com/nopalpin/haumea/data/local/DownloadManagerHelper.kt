package com.nopalpin.haumea.data.local

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nopalpin.haumea.worker.ChapterDownloadWorker
import java.io.File

class DownloadManagerHelper(private val context: Context) {
    val dbHelper by lazy { DownloadDatabaseHelper.getInstance(context.applicationContext) }

    fun startChapterDownload(
        chapterId: String,
        comicSlug: String,
        comicTitle: String,
        comicCover: String,
        comicType: String,
        chapterNumber: String,
        chapterTitle: String
    ) {
        try {
            val data = Data.Builder()
                .putString("chapter_id", chapterId)
                .putString("comic_slug", comicSlug)
                .putString("comic_title", comicTitle)
                .putString("comic_cover", comicCover)
                .putString("comic_type", comicType)
                .putString("chapter_number", chapterNumber)
                .putString("chapter_title", chapterTitle)
                .build()

            val request = OneTimeWorkRequestBuilder<ChapterDownloadWorker>()
                .setInputData(data)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueue(request)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteDownload(chapterId: String) {
        try {
            val entity = dbHelper.getDownloadByChapterId(chapterId)
            if (entity != null && entity.localDirectory.isNotBlank()) {
                val dir = File(entity.localDirectory)
                if (dir.exists()) {
                    dir.deleteRecursively()
                }
            }
            dbHelper.deleteByChapterId(chapterId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
