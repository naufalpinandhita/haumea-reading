package com.nopalpin.haumea.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DownloadDatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "haumea_downloads.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_DOWNLOADS = "downloads"
        private const val COL_CHAPTER_ID = "chapter_id"
        private const val COL_COMIC_SLUG = "comic_slug"
        private const val COL_COMIC_TITLE = "comic_title"
        private const val COL_COMIC_COVER = "comic_cover"
        private const val COL_COMIC_TYPE = "comic_type"
        private const val COL_CHAPTER_NUMBER = "chapter_number"
        private const val COL_CHAPTER_TITLE = "chapter_title"
        private const val COL_STATUS = "status"
        private const val COL_PROGRESS_PAGES = "progress_pages"
        private const val COL_TOTAL_PAGES = "total_pages"
        private const val COL_LOCAL_DIRECTORY = "local_directory"
        private const val COL_TIMESTAMP = "timestamp"

        @Volatile
        private var INSTANCE: DownloadDatabaseHelper? = null

        fun getInstance(context: Context): DownloadDatabaseHelper {
            return INSTANCE ?: synchronized(this) {
                val instance = DownloadDatabaseHelper(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_DOWNLOADS (
                $COL_CHAPTER_ID TEXT PRIMARY KEY,
                $COL_COMIC_SLUG TEXT,
                $COL_COMIC_TITLE TEXT,
                $COL_COMIC_COVER TEXT,
                $COL_COMIC_TYPE TEXT,
                $COL_CHAPTER_NUMBER TEXT,
                $COL_CHAPTER_TITLE TEXT,
                $COL_STATUS TEXT,
                $COL_PROGRESS_PAGES INTEGER,
                $COL_TOTAL_PAGES INTEGER,
                $COL_LOCAL_DIRECTORY TEXT,
                $COL_TIMESTAMP INTEGER
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DOWNLOADS")
        onCreate(db)
    }

    fun getAllDownloads(): List<DownloadEntity> {
        val list = mutableListOf<DownloadEntity>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_DOWNLOADS ORDER BY $COL_TIMESTAMP DESC", null)
        cursor.use { c ->
            if (c.moveToFirst()) {
                do {
                    list.add(
                        DownloadEntity(
                            chapterId = c.getString(c.getColumnIndexOrThrow(COL_CHAPTER_ID)),
                            comicSlug = c.getString(c.getColumnIndexOrThrow(COL_COMIC_SLUG)) ?: "",
                            comicTitle = c.getString(c.getColumnIndexOrThrow(COL_COMIC_TITLE)) ?: "",
                            comicCover = c.getString(c.getColumnIndexOrThrow(COL_COMIC_COVER)) ?: "",
                            comicType = c.getString(c.getColumnIndexOrThrow(COL_COMIC_TYPE)) ?: "",
                            chapterNumber = c.getString(c.getColumnIndexOrThrow(COL_CHAPTER_NUMBER)) ?: "",
                            chapterTitle = c.getString(c.getColumnIndexOrThrow(COL_CHAPTER_TITLE)) ?: "",
                            status = c.getString(c.getColumnIndexOrThrow(COL_STATUS)) ?: DownloadStatus.NOT_DOWNLOADED.name,
                            progressPages = c.getInt(c.getColumnIndexOrThrow(COL_PROGRESS_PAGES)),
                            totalPages = c.getInt(c.getColumnIndexOrThrow(COL_TOTAL_PAGES)),
                            localDirectory = c.getString(c.getColumnIndexOrThrow(COL_LOCAL_DIRECTORY)) ?: "",
                            timestamp = c.getLong(c.getColumnIndexOrThrow(COL_TIMESTAMP))
                        )
                    )
                } while (c.moveToNext())
            }
        }
        return list
    }

    fun getDownloadByChapterId(chapterId: String): DownloadEntity? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_DOWNLOADS WHERE $COL_CHAPTER_ID = ? LIMIT 1", arrayOf(chapterId))
        cursor.use { c ->
            if (c.moveToFirst()) {
                return DownloadEntity(
                    chapterId = c.getString(c.getColumnIndexOrThrow(COL_CHAPTER_ID)),
                    comicSlug = c.getString(c.getColumnIndexOrThrow(COL_COMIC_SLUG)) ?: "",
                    comicTitle = c.getString(c.getColumnIndexOrThrow(COL_COMIC_TITLE)) ?: "",
                    comicCover = c.getString(c.getColumnIndexOrThrow(COL_COMIC_COVER)) ?: "",
                    comicType = c.getString(c.getColumnIndexOrThrow(COL_COMIC_TYPE)) ?: "",
                    chapterNumber = c.getString(c.getColumnIndexOrThrow(COL_CHAPTER_NUMBER)) ?: "",
                    chapterTitle = c.getString(c.getColumnIndexOrThrow(COL_CHAPTER_TITLE)) ?: "",
                    status = c.getString(c.getColumnIndexOrThrow(COL_STATUS)) ?: DownloadStatus.NOT_DOWNLOADED.name,
                    progressPages = c.getInt(c.getColumnIndexOrThrow(COL_PROGRESS_PAGES)),
                    totalPages = c.getInt(c.getColumnIndexOrThrow(COL_TOTAL_PAGES)),
                    localDirectory = c.getString(c.getColumnIndexOrThrow(COL_LOCAL_DIRECTORY)) ?: "",
                    timestamp = c.getLong(c.getColumnIndexOrThrow(COL_TIMESTAMP))
                )
            }
        }
        return null
    }

    fun insertOrUpdate(item: DownloadEntity) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_CHAPTER_ID, item.chapterId)
            put(COL_COMIC_SLUG, item.comicSlug)
            put(COL_COMIC_TITLE, item.comicTitle)
            put(COL_COMIC_COVER, item.comicCover)
            put(COL_COMIC_TYPE, item.comicType)
            put(COL_CHAPTER_NUMBER, item.chapterNumber)
            put(COL_CHAPTER_TITLE, item.chapterTitle)
            put(COL_STATUS, item.status)
            put(COL_PROGRESS_PAGES, item.progressPages)
            put(COL_TOTAL_PAGES, item.totalPages)
            put(COL_LOCAL_DIRECTORY, item.localDirectory)
            put(COL_TIMESTAMP, item.timestamp)
        }
        db.insertWithOnConflict(TABLE_DOWNLOADS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun updateProgress(chapterId: String, status: String, progressPages: Int, totalPages: Int) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_STATUS, status)
            put(COL_PROGRESS_PAGES, progressPages)
            put(COL_TOTAL_PAGES, totalPages)
        }
        db.update(TABLE_DOWNLOADS, values, "$COL_CHAPTER_ID = ?", arrayOf(chapterId))
    }

    fun deleteByChapterId(chapterId: String) {
        val db = writableDatabase
        db.delete(TABLE_DOWNLOADS, "$COL_CHAPTER_ID = ?", arrayOf(chapterId))
    }
}
