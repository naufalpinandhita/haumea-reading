package com.nopalpin.haumea.data.local

enum class DownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

data class DownloadEntity(
    val chapterId: String,
    val comicSlug: String,
    val comicTitle: String,
    val comicCover: String,
    val comicType: String,
    val chapterNumber: String,
    val chapterTitle: String,
    val status: String = DownloadStatus.NOT_DOWNLOADED.name,
    val progressPages: Int = 0,
    val totalPages: Int = 0,
    val localDirectory: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
