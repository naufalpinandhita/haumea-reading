package com.nopalpin.haumea

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nopalpin.haumea.ui.BookmarksScreen
import com.nopalpin.haumea.ui.DetailScreen
import com.nopalpin.haumea.ui.DetailUiState
import com.nopalpin.haumea.ui.DownloadScreen
import com.nopalpin.haumea.ui.HistoryScreen
import com.nopalpin.haumea.ui.HomeScreen
import com.nopalpin.haumea.ui.MainViewModel
import com.nopalpin.haumea.ui.ReaderScreen
import com.nopalpin.haumea.ui.SplashScreen
import com.nopalpin.haumea.ui.theme.HaumeaTheme
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

val BookmarkNavIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "BookmarkNav",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF997861)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(17f, 3f)
            lineTo(7f, 3f)
            lineTo(5f, 5f)
            lineTo(5f, 21f)
            lineTo(12f, 17.5f)
            lineTo(19f, 21f)
            lineTo(19f, 5f)
            close()
        }
    }.build()
}

val DownloadNavIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "DownloadNav",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF997861)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 4f)
            lineTo(12f, 16f)
            moveTo(12f, 16f)
            lineTo(7f, 11f)
            moveTo(12f, 16f)
            lineTo(17f, 11f)
            moveTo(5f, 20f)
            lineTo(19f, 20f)
        }
    }.build()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HaumeaTheme {
                HaumeaApp()
            }
        }
    }
}

@Composable
fun HaumeaApp(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as Application)
    )

    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavRoutes = setOf("home", "bookmarks", "history", "download")
    val shouldShowBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = 4.dp
                ) {
                    // 1. Home Tab
                    NavigationBarItem(
                        selected = currentRoute == "home",
                        onClick = {
                            if (currentRoute != "home") {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        },
                        icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.secondary,
                            unselectedTextColor = MaterialTheme.colorScheme.secondary
                        )
                    )

                    // 2. Bookmarks Tab
                    NavigationBarItem(
                        selected = currentRoute == "bookmarks",
                        onClick = {
                            if (currentRoute != "bookmarks") {
                                navController.navigate("bookmarks") {
                                    popUpTo("home")
                                }
                            }
                        },
                        icon = { Icon(imageVector = BookmarkNavIcon, contentDescription = "Bookmarks") },
                        label = { Text("Bookmarks") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.secondary,
                            unselectedTextColor = MaterialTheme.colorScheme.secondary
                        )
                    )

                    // 3. History Tab
                    NavigationBarItem(
                        selected = currentRoute == "history",
                        onClick = {
                            if (currentRoute != "history") {
                                navController.navigate("history") {
                                    popUpTo("home")
                                }
                            }
                        },
                        icon = { Icon(imageVector = Icons.Default.Refresh, contentDescription = "History") },
                        label = { Text("History") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.secondary,
                            unselectedTextColor = MaterialTheme.colorScheme.secondary
                        )
                    )

                    // 4. Download Tab
                    NavigationBarItem(
                        selected = currentRoute == "download",
                        onClick = {
                            if (currentRoute != "download") {
                                navController.navigate("download") {
                                    popUpTo("home")
                                }
                            }
                        },
                        icon = { Icon(imageVector = DownloadNavIcon, contentDescription = "Unduhan") },
                        label = { Text("Unduhan") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.secondary,
                            unselectedTextColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 0. Splash Screen
            composable("splash") {
                SplashScreen(
                    onSplashFinished = {
                        navController.navigate("home") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                )
            }

            // 1. Home / Pustaka & Search Screen
            composable("home") {
                HomeScreen(
                    uiState = viewModel.pustakaUiState,
                    searchQuery = viewModel.searchQuery,
                    selectedTipe = viewModel.selectedTipe,
                    selectedGenre = viewModel.selectedGenre,
                    selectedAlphabet = viewModel.selectedAlphabet,
                    currentPage = viewModel.currentPage,
                    hasNextPage = viewModel.hasNextPage,
                    isFilterActive = viewModel.isFilterActive,
                    searchSuggestions = viewModel.searchSuggestions,
                    isSearchingSuggestions = viewModel.isSearchingSuggestions,
                    onSearchQueryChanged = { query -> viewModel.onSearchQueryChanged(query) },
                    onPerformSearch = { viewModel.performSearch() },
                    onClearSuggestions = { viewModel.clearSearchSuggestions() },
                    onTipeChanged = { tipe -> viewModel.onTipeChanged(tipe) },
                    onGenreChanged = { genre -> viewModel.onGenreChanged(genre) },
                    onAlphabetChanged = { letter -> viewModel.onAlphabetChanged(letter) },
                    onResetFilters = { viewModel.resetFilters() },
                    onPageChanged = { page -> viewModel.onPageChanged(page) },
                    onRetry = { viewModel.fetchPustaka() },
                    onComicClick = { slug, tipe ->
                        val encodedSlug = URLEncoder.encode(slug, StandardCharsets.UTF_8.name())
                        val encodedTipe = URLEncoder.encode(tipe.lowercase(), StandardCharsets.UTF_8.name())
                        navController.navigate("detail/$encodedSlug?tipe=$encodedTipe")
                    }
                )
            }

            // 2. Bookmarks Screen
            composable("bookmarks") {
                BookmarksScreen(
                    bookmarks = viewModel.bookmarksList,
                    onRemoveBookmark = { slug -> viewModel.removeBookmark(slug) },
                    onComicClick = { slug, tipe ->
                        val encodedSlug = URLEncoder.encode(slug, StandardCharsets.UTF_8.name())
                        val encodedTipe = URLEncoder.encode(tipe.lowercase(), StandardCharsets.UTF_8.name())
                        navController.navigate("detail/$encodedSlug?tipe=$encodedTipe")
                    },
                    onExploreClick = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
            }

            // 3. History Screen
            composable("history") {
                HistoryScreen(
                    historyList = viewModel.historyList,
                    onRemoveHistory = { slug -> viewModel.removeHistoryItem(slug) },
                    onClearAllHistory = { viewModel.clearAllHistory() },
                    onComicClick = { slug, tipe ->
                        val encodedSlug = URLEncoder.encode(slug, StandardCharsets.UTF_8.name())
                        val encodedTipe = URLEncoder.encode(tipe.lowercase(), StandardCharsets.UTF_8.name())
                        navController.navigate("detail/$encodedSlug?tipe=$encodedTipe")
                    },
                    onChapterClick = { chapterId, page ->
                        val encodedChapterId = URLEncoder.encode(chapterId, StandardCharsets.UTF_8.name())
                        navController.navigate("reader/$encodedChapterId?page=$page")
                    },
                    onExploreClick = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
            }

            // 4. Download Screen
            composable("download") {
                DownloadScreen(
                    downloadsList = viewModel.downloadsList,
                    onDeleteDownload = { chapterId -> viewModel.deleteDownload(chapterId) },
                    onComicClick = { slug, tipe ->
                        val encodedSlug = URLEncoder.encode(slug, StandardCharsets.UTF_8.name())
                        val encodedTipe = URLEncoder.encode(tipe.lowercase(), StandardCharsets.UTF_8.name())
                        navController.navigate("detail/$encodedSlug?tipe=$encodedTipe")
                    },
                    onChapterClick = { chapterId ->
                        val encodedChapterId = URLEncoder.encode(chapterId, StandardCharsets.UTF_8.name())
                        navController.navigate("reader/$encodedChapterId")
                    },
                    onExploreClick = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
            }

            // 5. Detail Komik Screen
            composable(
                route = "detail/{slug}?tipe={tipe}",
                arguments = listOf(
                    navArgument("slug") { type = NavType.StringType },
                    navArgument("tipe") { type = NavType.StringType; defaultValue = "manga" }
                )
            ) { backStackEntry ->
                val rawSlug = backStackEntry.arguments?.getString("slug") ?: ""
                val rawTipe = backStackEntry.arguments?.getString("tipe") ?: "manga"
                val slug = URLDecoder.decode(rawSlug, StandardCharsets.UTF_8.name())
                val tipe = URLDecoder.decode(rawTipe, StandardCharsets.UTF_8.name())

                val detailState = viewModel.detailUiState
                val isBookmarked = viewModel.isBookmarked(slug)

                DetailScreen(
                    slug = slug,
                    tipe = tipe,
                    uiState = viewModel.detailUiState,
                    isBookmarked = isBookmarked,
                    downloadsList = viewModel.downloadsList,
                    onFetchDetail = { s, t -> viewModel.fetchComicDetail(s, t) },
                    onToggleBookmark = {
                        if (detailState is DetailUiState.Success) {
                            val detail = detailState.detail
                            viewModel.toggleBookmark(
                                slug = detail.titleSlug.ifEmpty { slug },
                                title = detail.title,
                                thumbnail = detail.cover,
                                type = detail.type
                            )
                        }
                    },
                    onStartDownload = { chapter ->
                        if (detailState is DetailUiState.Success) {
                            val detail = detailState.detail
                            viewModel.startDownloadChapter(
                                chapterId = chapter.chapterId,
                                comicSlug = detail.titleSlug.ifEmpty { slug },
                                comicTitle = detail.title,
                                comicCover = detail.cover,
                                comicType = detail.type,
                                chapterNumber = chapter.chapterNumber,
                                chapterTitle = chapter.title
                            )
                        }
                    },
                    onBackClick = { navController.popBackStack() },
                    onChapterClick = { chapterId ->
                        val encodedChapterId = URLEncoder.encode(chapterId, StandardCharsets.UTF_8.name())
                        navController.navigate("reader/$encodedChapterId")
                    }
                )
            }

            // 6. Reader Screen
            composable(
                route = "reader/{chapterId}?page={page}",
                arguments = listOf(
                    navArgument("chapterId") { type = NavType.StringType },
                    navArgument("page") { type = NavType.IntType; defaultValue = 1 }
                )
            ) { backStackEntry ->
                val rawChapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
                val initialPage = backStackEntry.arguments?.getInt("page") ?: 1
                val chapterId = URLDecoder.decode(rawChapterId, StandardCharsets.UTF_8.name())

                ReaderScreen(
                    chapterId = chapterId,
                    initialPage = initialPage,
                    uiState = viewModel.readerUiState,
                    onFetchPages = { id -> viewModel.fetchChapterPages(id, initialPage) },
                    onPageChanged = { page -> viewModel.updateReadingPage(chapterId, page) },
                    onBackClick = { navController.popBackStack() },
                    onNavigateChapter = { targetChapterId ->
                        val encodedChapterId = URLEncoder.encode(targetChapterId, StandardCharsets.UTF_8.name())
                        navController.navigate("reader/$encodedChapterId") {
                            popUpTo("reader/{chapterId}?page={page}") { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
