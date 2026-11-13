package com.nopalpin.haumea.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nopalpin.haumea.data.api.NetworkModule
import com.nopalpin.haumea.data.local.BookmarkItem
import com.nopalpin.haumea.data.local.DownloadEntity
import com.nopalpin.haumea.data.local.DownloadManagerHelper
import com.nopalpin.haumea.data.local.DownloadStatus
import com.nopalpin.haumea.data.local.HistoryItem
import com.nopalpin.haumea.data.local.LocalDataManager
import com.nopalpin.haumea.model.ChapterItem
import com.nopalpin.haumea.model.ChapterPagesData
import com.nopalpin.haumea.model.ComicDetail
import com.nopalpin.haumea.model.ComicItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

sealed interface PustakaUiState {
    object Loading : PustakaUiState
    data class Success(val comics: List<ComicItem>) : PustakaUiState
    data class Error(val message: String) : PustakaUiState
}

sealed interface DetailUiState {
    object Loading : DetailUiState
    data class Success(val detail: ComicDetail) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

sealed interface ReaderUiState {
    object Loading : ReaderUiState
    data class Success(
        val pagesData: ChapterPagesData,
        val chapterTitle: String = "",
        val prevChapterId: String? = null,
        val nextChapterId: String? = null
    ) : ReaderUiState
    data class Error(val message: String) : ReaderUiState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val apiService = NetworkModule.apiService
    private val localDataManager = LocalDataManager(application.applicationContext)
    private val downloadManagerHelper = DownloadManagerHelper(application.applicationContext)

    // Local Data States (Bookmarks & History)
    var bookmarksList: List<BookmarkItem> by mutableStateOf(emptyList())
        private set

    var historyList: List<HistoryItem> by mutableStateOf(emptyList())
        private set

    // Downloads State
    var downloadsList: List<DownloadEntity> by mutableStateOf(emptyList())
        private set

    // Active Comic Metadata for accurate History Tracking
    private var activeComicSlug: String = ""
    private var activeComicTitle: String = ""
    private var activeComicCover: String = ""
    private var activeComicType: String = ""

    // Pustaka / Home State
    var pustakaUiState: PustakaUiState by mutableStateOf(PustakaUiState.Loading)
        private set

    var searchQuery: String by mutableStateOf("")
        private set

    var selectedTipe: String by mutableStateOf("manga")
        private set

    var currentPage: Int by mutableStateOf(1)
        private set

    var hasNextPage: Boolean by mutableStateOf(false)
        private set

    var selectedGenre: String by mutableStateOf("Semua")
        private set

    var selectedAlphabet: String by mutableStateOf("Semua")
        private set

    val isFilterActive: Boolean
        get() = selectedGenre != "Semua" || selectedAlphabet != "Semua" || selectedTipe != "manga" || searchQuery.isNotBlank()

    // Auto-complete Search Suggestions
    var searchSuggestions: List<ComicItem> by mutableStateOf(emptyList())
        private set

    var isSearchingSuggestions: Boolean by mutableStateOf(false)
        private set

    private var searchJob: Job? = null

    // Detail State & Chapters List
    var detailUiState: DetailUiState by mutableStateOf(DetailUiState.Loading)
        private set

    var activeChapters: List<ChapterItem> by mutableStateOf(emptyList())
        private set

    // Reader State
    var readerUiState: ReaderUiState by mutableStateOf(ReaderUiState.Loading)
        private set

    init {
        loadLocalData()
        observeDownloads()
        fetchPustaka(1)
    }

    fun loadLocalData() {
        bookmarksList = localDataManager.getBookmarks()
        historyList = localDataManager.getHistory()
    }

    private fun observeDownloads() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            while (true) {
                var hasActiveDownloading = false
                try {
                    val list = downloadManagerHelper.dbHelper.getAllDownloads()
                    hasActiveDownloading = list.any { it.status == DownloadStatus.DOWNLOADING.name }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        downloadsList = list
                    }
                } catch (_: Exception) {}
                delay(if (hasActiveDownloading) 800L else 4000L)
            }
        }
    }

    fun startDownloadChapter(
        chapterId: String,
        comicSlug: String,
        comicTitle: String,
        comicCover: String,
        comicType: String,
        chapterNumber: String,
        chapterTitle: String
    ) {
        downloadManagerHelper.startChapterDownload(
            chapterId = chapterId,
            comicSlug = comicSlug,
            comicTitle = comicTitle,
            comicCover = comicCover,
            comicType = comicType,
            chapterNumber = chapterNumber,
            chapterTitle = chapterTitle
        )
        // Instant state refresh for immediate UI feedback
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            delay(200)
            val list = downloadManagerHelper.dbHelper.getAllDownloads()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                downloadsList = list
            }
        }
    }

    fun deleteDownload(chapterId: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            downloadManagerHelper.deleteDownload(chapterId)
            val list = downloadManagerHelper.dbHelper.getAllDownloads()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                downloadsList = list
            }
        }
    }

    fun isBookmarked(slug: String): Boolean {
        return bookmarksList.any { it.slug == slug }
    }

    fun toggleBookmark(slug: String, title: String, thumbnail: String, type: String): Boolean {
        val item = BookmarkItem(slug = slug, title = title, thumbnail = thumbnail, type = type)
        val isAdded = localDataManager.toggleBookmark(item)
        bookmarksList = localDataManager.getBookmarks()
        return isAdded
    }

    fun removeBookmark(slug: String) {
        localDataManager.removeBookmark(slug)
        bookmarksList = localDataManager.getBookmarks()
    }

    fun addToHistory(
        slug: String,
        title: String,
        thumbnail: String,
        type: String,
        lastChapterId: String = "",
        lastChapterTitle: String = "",
        lastPage: Int = 1
    ) {
        val item = HistoryItem(
            slug = slug,
            title = title,
            thumbnail = thumbnail,
            type = type,
            lastChapterId = lastChapterId,
            lastChapterTitle = lastChapterTitle,
            lastPage = lastPage
        )
        localDataManager.addOrUpdateHistory(item)
        historyList = localDataManager.getHistory()
    }

    fun removeHistoryItem(slug: String) {
        localDataManager.removeHistory(slug)
        historyList = localDataManager.getHistory()
    }

    fun clearAllHistory() {
        localDataManager.clearHistory()
        historyList = emptyList()
    }

    fun onTipeChanged(newTipe: String) {
        if (selectedTipe != newTipe) {
            selectedTipe = newTipe
            currentPage = 1
            applyFilterOrFetch()
        }
    }

    fun onGenreChanged(newGenre: String) {
        if (selectedGenre != newGenre) {
            selectedGenre = newGenre
            currentPage = 1
            applyFilterOrFetch()
        }
    }

    fun onAlphabetChanged(newAlphabet: String) {
        if (selectedAlphabet != newAlphabet) {
            selectedAlphabet = newAlphabet
            currentPage = 1
            applyFilterOrFetch()
        }
    }

    fun resetFilters() {
        selectedGenre = "Semua"
        selectedAlphabet = "Semua"
        selectedTipe = "manga"
        searchQuery = ""
        currentPage = 1
        fetchPustaka(1)
    }

    fun onPageChanged(newPage: Int) {
        if (newPage >= 1) {
            currentPage = newPage
            applyFilterOrFetch()
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery = query

        searchJob?.cancel()
        if (query.isBlank()) {
            searchSuggestions = emptyList()
            isSearchingSuggestions = false
            applyFilterOrFetch()
        } else {
            isSearchingSuggestions = true
            searchJob = viewModelScope.launch {
                delay(350)
                try {
                    val response = apiService.searchComics(query = query, tipe = selectedTipe)
                    searchSuggestions = response.data
                } catch (_: Exception) {
                    searchSuggestions = emptyList()
                } finally {
                    isSearchingSuggestions = false
                }
            }
        }
    }

    fun clearSearchSuggestions() {
        searchSuggestions = emptyList()
        isSearchingSuggestions = false
    }

    fun applyFilterOrFetch() {
        performSearch()
    }

    fun performSearch() {
        viewModelScope.launch {
            pustakaUiState = PustakaUiState.Loading
            clearSearchSuggestions()
            try {
                when {
                    searchQuery.isNotBlank() -> {
                        val response = apiService.searchComics(query = searchQuery, tipe = selectedTipe)
                        var list = response.data

                        if (selectedGenre != "Semua") {
                            list = list.filter { comic ->
                                (comic.description ?: "").contains(selectedGenre, ignoreCase = true) ||
                                    comic.title.contains(selectedGenre, ignoreCase = true)
                            }
                        }

                        if (selectedAlphabet != "Semua") {
                            list = list.filter { comic ->
                                comic.title.startsWith(selectedAlphabet, ignoreCase = true)
                            }
                        }

                        pustakaUiState = PustakaUiState.Success(list)
                        hasNextPage = false
                    }

                    selectedGenre != "Semua" -> {
                        val genreSlug = selectedGenre.lowercase().trim().replace(" ", "-")
                        val response = apiService.getComicsByGenre(genreSlug = genreSlug, page = currentPage)
                        var list = response.data

                        if (selectedTipe.isNotBlank()) {
                            val typeMatches = list.filter { comic ->
                                comic.type.equals(selectedTipe, ignoreCase = true)
                            }
                            if (typeMatches.isNotEmpty()) {
                                list = typeMatches
                            }
                        }

                        if (selectedAlphabet != "Semua") {
                            list = list.filter { comic ->
                                comic.title.startsWith(selectedAlphabet, ignoreCase = true)
                            }
                        }

                        pustakaUiState = PustakaUiState.Success(list)
                        hasNextPage = response.hasNext ?: false
                    }

                    selectedAlphabet != "Semua" -> {
                        val response = apiService.getDaftarKomik(
                            huruf = selectedAlphabet,
                            tipe = selectedTipe,
                            page = currentPage
                        )
                        pustakaUiState = PustakaUiState.Success(response.data)
                        hasNextPage = response.hasNext ?: false
                    }

                    else -> {
                        val response = apiService.getPustaka(tipe = selectedTipe, page = currentPage)
                        pustakaUiState = PustakaUiState.Success(response.data)
                        hasNextPage = response.hasNext ?: false
                    }
                }
            } catch (e: Exception) {
                pustakaUiState = PustakaUiState.Error(
                    e.localizedMessage ?: "Terjadi kesalahan saat memuat komik."
                )
            }
        }
    }

    fun fetchPustaka(page: Int = currentPage) {
        currentPage = page
        applyFilterOrFetch()
    }

    fun fetchComicDetail(slug: String, tipe: String = selectedTipe) {
        viewModelScope.launch {
            detailUiState = DetailUiState.Loading
            try {
                val response = apiService.getComicDetail(comicId = slug, tipe = tipe.lowercase())
                val detail = response.data
                detailUiState = DetailUiState.Success(detail)
                activeChapters = detail.chapters

                activeComicSlug = detail.titleSlug.ifEmpty { slug }
                activeComicTitle = detail.title
                activeComicCover = detail.cover
                activeComicType = detail.type

                val existing = historyList.find { it.slug == activeComicSlug }
                addToHistory(
                    slug = activeComicSlug,
                    title = activeComicTitle,
                    thumbnail = activeComicCover,
                    type = activeComicType,
                    lastChapterId = existing?.lastChapterId ?: "",
                    lastChapterTitle = existing?.lastChapterTitle ?: "",
                    lastPage = existing?.lastPage ?: 1
                )
            } catch (e: Exception) {
                detailUiState = DetailUiState.Error(
                    e.localizedMessage ?: "Gagal memuat detail komik."
                )
            }
        }
    }

    fun fetchChapterPages(chapterId: String, initialPage: Int = 1) {
        viewModelScope.launch {
            readerUiState = ReaderUiState.Loading
            try {
                val currentIdx = activeChapters.indexOfFirst { it.chapterId == chapterId }
                val chapterTitle = if (currentIdx != -1) activeChapters[currentIdx].title else "Chapter $chapterId"

                val prevChapterId = if (currentIdx > 0) activeChapters[currentIdx - 1].chapterId else null
                val nextChapterId = if (currentIdx != -1 && currentIdx < activeChapters.size - 1) activeChapters[currentIdx + 1].chapterId else null

                // Check if chapter downloaded locally offline
                val downloadedEntity = downloadManagerHelper.dbHelper.getDownloadByChapterId(chapterId)
                if (downloadedEntity != null && downloadedEntity.status == DownloadStatus.COMPLETED.name && downloadedEntity.localDirectory.isNotBlank()) {
                    val localDir = File(downloadedEntity.localDirectory)
                    if (localDir.exists()) {
                        val imageFiles = localDir.listFiles()?.filter { it.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp") }?.sortedBy { it.name }
                        if (!imageFiles.isNullOrEmpty()) {
                            val localPages = imageFiles.map { it.absolutePath }
                            val pagesData = ChapterPagesData(
                                chapterId = chapterId,
                                totalPages = localPages.size,
                                pages = localPages
                            )
                            readerUiState = ReaderUiState.Success(
                                pagesData = pagesData,
                                chapterTitle = chapterTitle,
                                prevChapterId = prevChapterId,
                                nextChapterId = nextChapterId
                            )

                            updateHistoryForChapter(chapterId, chapterTitle, initialPage)
                            return@launch
                        }
                    }
                }

                // If not downloaded offline, fetch from network stream
                val response = apiService.getChapterPages(chapterId = chapterId)
                readerUiState = ReaderUiState.Success(
                    pagesData = response.data,
                    chapterTitle = chapterTitle,
                    prevChapterId = prevChapterId,
                    nextChapterId = nextChapterId
                )

                updateHistoryForChapter(chapterId, chapterTitle, initialPage)
            } catch (e: Exception) {
                readerUiState = ReaderUiState.Error(
                    e.localizedMessage ?: "Gagal memuat halaman chapter."
                )
            }
        }
    }

    private fun updateHistoryForChapter(chapterId: String, chapterTitle: String, pageNumber: Int) {
        val existing = historyList.find { it.lastChapterId == chapterId || it.slug == activeComicSlug }
        val slug = activeComicSlug.ifEmpty { existing?.slug ?: "" }
        val title = activeComicTitle.ifEmpty { existing?.title ?: "" }
        val cover = activeComicCover.ifEmpty { existing?.thumbnail ?: "" }
        val type = activeComicType.ifEmpty { existing?.type ?: "manga" }

        if (slug.isNotBlank() && title.isNotBlank()) {
            addToHistory(
                slug = slug,
                title = title,
                thumbnail = cover,
                type = type,
                lastChapterId = chapterId,
                lastChapterTitle = chapterTitle,
                lastPage = pageNumber
            )
        }
    }

    fun updateReadingPage(chapterId: String, pageNumber: Int) {
        val currentIdx = activeChapters.indexOfFirst { it.chapterId == chapterId }
        val chapterTitle = if (currentIdx != -1) activeChapters[currentIdx].title else "Chapter $chapterId"
        updateHistoryForChapter(chapterId, chapterTitle, pageNumber)
    }
}
