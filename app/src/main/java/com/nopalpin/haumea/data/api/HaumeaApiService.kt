package com.nopalpin.haumea.data.api

import com.nopalpin.haumea.model.ChapterPagesResponse
import com.nopalpin.haumea.model.ComicDetailResponse
import com.nopalpin.haumea.model.ComicListResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface HaumeaApiService {

    @GET("api/pustaka")
    suspend fun getPustaka(
        @Query("tipe") tipe: String = "manga",
        @Query("page") page: Int = 1
    ): ComicListResponse

    @GET("api/search")
    suspend fun searchComics(
        @Query("q") query: String,
        @Query("tipe") tipe: String? = null
    ): ComicListResponse

    @GET("api/genre/{genre_slug}")
    suspend fun getComicsByGenre(
        @Path("genre_slug") genreSlug: String,
        @Query("page") page: Int = 1
    ): ComicListResponse

    @GET("api/daftar-komik")
    suspend fun getDaftarKomik(
        @Query("huruf") huruf: String? = null,
        @Query("tipe") tipe: String? = null,
        @Query("page") page: Int = 1
    ): ComicListResponse

    @GET("api/comic/{comic_id}")
    suspend fun getComicDetail(
        @Path("comic_id") comicId: String,
        @Query("tipe") tipe: String = "manga"
    ): ComicDetailResponse

    @GET("api/chapter/{chapter_id}")
    suspend fun getChapterPages(
        @Path("chapter_id") chapterId: String
    ): ChapterPagesResponse
}
