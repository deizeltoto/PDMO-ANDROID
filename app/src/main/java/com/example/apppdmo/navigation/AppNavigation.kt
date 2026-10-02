package com.example.apppdmo.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.apppdmo.ui.server.ServerScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.apppdmo.data.repository.BibleRepository
import com.example.apppdmo.data.repository.CommunityRepository
import com.example.apppdmo.data.repository.SongsRepository
import com.example.apppdmo.notification.NotificationManager
import com.example.apppdmo.ui.about.AboutScreen
import com.example.apppdmo.ui.bible.BibleChaptersScreen
import com.example.apppdmo.ui.bible.BibleFavoritesScreen
import com.example.apppdmo.ui.bible.BibleReaderScreen
import com.example.apppdmo.ui.bible.BibleScreen
import com.example.apppdmo.ui.bible.BibleSearchScreen
import com.example.apppdmo.ui.contents.ContentDetailScreen
import com.example.apppdmo.ui.contents.ContentsScreen
import com.example.apppdmo.ui.home.HomeScreen
import com.example.apppdmo.ui.songs.SongDetailScreen
import com.example.apppdmo.ui.songs.SongsScreen

@Composable
fun AppNavigation(
    communityRepository: CommunityRepository,
    bibleRepository: BibleRepository,
    songsRepository: SongsRepository,
    notificationManager: NotificationManager,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TextButton(
                modifier = Modifier.statusBarsPadding(),
                onClick = { navController.navigate("server") { launchSingleTop = true } }
            ) {
                Text("Servidor")
            }
        },
        bottomBar = {
            NavigationBar {
                BottomNavItem.items.forEach { item ->
                    val selected = currentRoute == item.route ||
                            (item == BottomNavItem.Bible && currentRoute?.startsWith("bible") == true) ||
                            (item == BottomNavItem.Songs && currentRoute?.startsWith("songs") == true) ||
                            (item == BottomNavItem.Contents && currentRoute?.startsWith("content") == true)
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(text = item.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("server") {
                ServerScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    repository = communityRepository,
                    onNavigateToBible = {
                        navController.navigate(Screen.Bible.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToSongs = {
                        navController.navigate(Screen.Songs.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToContents = {
                        navController.navigate(Screen.Contents.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToContentDetail = { contentId ->
                        navController.navigate(Screen.ContentDetail.createRoute(contentId))
                    },
                    onNavigateToAbout = {
                        navController.navigate(Screen.About.route)
                    },
                    onNotifyMessage = { message, ref ->
                        notificationManager.showDailyMessageNotification("Mensagem do dia", "\"$message\" - $ref")
                    }
                )
            }

            composable(Screen.Bible.route) {
                BibleScreen(
                    bibleRepository = bibleRepository,
                    onNavigateToBook = { bookId ->
                        navController.navigate(Screen.BibleBook.createRoute(bookId))
                    },
                    onNavigateToRead = { bookId, chapter ->
                        navController.navigate(Screen.BibleReader.createRoute(bookId, chapter))
                    },
                    onNavigateToSearch = {
                        navController.navigate(Screen.BibleSearch.route)
                    },
                    onNavigateToFavorites = {
                        navController.navigate(Screen.BibleFavorites.route)
                    }
                )
            }

            composable(
                route = Screen.BibleBook.route,
                arguments = listOf(navArgument("bookId") { type = NavType.IntType })
            ) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getInt("bookId") ?: 1
                BibleChaptersScreen(
                    bibleRepository = bibleRepository,
                    bookId = bookId,
                    onNavigateBack = { navController.popBackStack() },
                    onChapterSelected = { selectedBookId, chapterNum ->
                        navController.navigate(Screen.BibleReader.createRoute(selectedBookId, chapterNum))
                    }
                )
            }

            composable(
                route = Screen.BibleReader.route,
                arguments = listOf(
                    navArgument("bookId") { type = NavType.IntType },
                    navArgument("chapter") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getInt("bookId") ?: 1
                val chapter = backStackEntry.arguments?.getInt("chapter") ?: 1
                BibleReaderScreen(
                    bibleRepository = bibleRepository,
                    bookId = bookId,
                    chapter = chapter,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.BibleSearch.route) {
                BibleSearchScreen(
                    bibleRepository = bibleRepository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRead = { bookId, chapter ->
                        navController.navigate(Screen.BibleReader.createRoute(bookId, chapter))
                    }
                )
            }

            composable(Screen.BibleFavorites.route) {
                BibleFavoritesScreen(
                    bibleRepository = bibleRepository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRead = { bookId, chapter ->
                        navController.navigate(Screen.BibleReader.createRoute(bookId, chapter))
                    }
                )
            }

            composable(Screen.Songs.route) {
                SongsScreen(
                    songsRepository = songsRepository,
                    onSongSelected = { songId ->
                        navController.navigate(Screen.SongDetail.createRoute(songId))
                    }
                )
            }

            composable(
                route = Screen.SongDetail.route,
                arguments = listOf(navArgument("songId") { type = NavType.IntType })
            ) { backStackEntry ->
                val songId = backStackEntry.arguments?.getInt("songId") ?: 1
                SongDetailScreen(
                    songsRepository = songsRepository,
                    songId = songId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Contents.route) {
                ContentsScreen(
                    communityRepository = communityRepository,
                    onContentSelected = { contentId ->
                        navController.navigate(Screen.ContentDetail.createRoute(contentId))
                    }
                )
            }

            composable(
                route = Screen.ContentDetail.route,
                arguments = listOf(navArgument("contentId") { type = NavType.LongType })
            ) { backStackEntry ->
                val contentId = backStackEntry.arguments?.getLong("contentId") ?: 1L
                ContentDetailScreen(
                    communityRepository = communityRepository,
                    contentId = contentId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.About.route) {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
