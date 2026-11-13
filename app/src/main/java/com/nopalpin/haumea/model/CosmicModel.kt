package com.nopalpin.haumea.model

import com.google.gson.annotations.SerializedName

data class ComicListResponse(
    val status: String,
    val tipe: String?,
    val page: Int? = null,
    @SerializedName("has_next") val hasNext: Boolean? = null,
    val data: List<ComicItem>
)

data class ComicItem(
    val slug: String,
    val title: String,
    val thumbnail: String,
    val type: String,
    val description: String?,
    @SerializedName("latest_chapter") val latestChapter: String?
)

data class ComicDetailResponse(
    val status: String,
    val tipe: String,
    val data: ComicDetail
)

data class ComicDetail(
    @SerializedName("title_slug") val titleSlug: String,
    val title: String,
    val cover: String,
    val synopsis: String,
    val genres: List<String>,
    val type: String,
    val author: String,
    val status: String,
    val chapters: List<ChapterItem>
)

data class ChapterItem(
    @SerializedName("chapter_number") val chapterNumber: String,
    @SerializedName("chapter_id") val chapterId: String,
    val title: String,
    @SerializedName("release_date") val releaseDate: String
)

data class ChapterPagesResponse(
    val status: String,
    val data: ChapterPagesData
)

data class ChapterPagesData(
    @SerializedName("chapter_id") val chapterId: String,
    @SerializedName("total_pages") val totalPages: Int,
    val pages: List<String>
)