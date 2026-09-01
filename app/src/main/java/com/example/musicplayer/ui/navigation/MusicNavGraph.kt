package com.example.musicplayer.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.musicplayer.ui.components.MiniPlayer
import com.example.musicplayer.ui.screens.*
import com.example.musicplayer.viewmodel.MusicViewModel

sealed class Screen(val route: String, val label: String) {
    data object Library : Screen("library", "Library")
    data object Search : Screen("search", "Search")
    data object Playlists : Screen("playlists", "Playlists")
    data object Folders : Screen("folders", "Folders")
    data object Favorites : Screen("favorites", "Favorites")
    data object NowPlaying : Screen("now_playing", "Now Playing")
    data object Equalizer : Screen("equalizer", "Equalizer")
    data object Settings : Screen("settings", "Settings")
    data object VideoPlayer : Screen("video_player/{videoId}", "Video Player") {
        fun createRoute(videoId: Long) = "video_player/$videoId"
    }
}

private val bottomTabs = listOf(Screen.Library, Screen.Search, Screen.Playlists, Screen.Folders)

@Composable
fun MusicNavGraph(viewModel: MusicViewModel) {
    val navController = rememberNavController()
    val playbackState by viewModel.playbackState.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isNowPlaying = currentDestination?.hierarchy?.any { it.route == Screen.NowPlaying.route } == true
    val isVideoPlayer = currentDestination?.route?.startsWith("video_player/") == true

    Scaffold(
        bottomBar = {
            Column {
                // The audio MiniPlayer is deliberately not shown over full Now Playing
                // or the video player. It stays available on the normal library screens.
                if (playbackState.currentSong != null && !isNowPlaying && !isVideoPlayer) {
                    MiniPlayer(
                        viewModel = viewModel,
                        onExpand = { navController.navigate(Screen.NowPlaying.route) }
                    )
                }

                NavigationBar {
                    bottomTabs.forEach { screen ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                // Library is the real root. From ANY screen, including
                                // video/equalizer/settings, this returns to Home cleanly.
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Library.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(iconFor(screen), contentDescription = screen.label) },
                            label = { Text(screen.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Library.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Library.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onOpenEqualizer = { navController.navigate(Screen.Equalizer.route) },
                    onOpenSettings = { navController.navigate(Screen.Settings.route) },
                    onOpenFavorites = { navController.navigate(Screen.Favorites.route) },
                    onOpenVideo = { id -> navController.navigate(Screen.VideoPlayer.createRoute(id)) }
                )
            }

            composable(Screen.Search.route) { SearchScreen(viewModel) }
            composable(Screen.Playlists.route) { PlaylistScreen(viewModel) }
            composable(Screen.Folders.route) {
                FolderBrowserScreen(
                    viewModel = viewModel,
                    onOpenVideo = { id -> navController.navigate(Screen.VideoPlayer.createRoute(id)) }
                )
            }
            composable(Screen.Favorites.route) { FavoritesScreen(viewModel) }

            composable(Screen.NowPlaying.route) {
                NowPlayingScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenEqualizer = { navController.navigate(Screen.Equalizer.route) }
                )
            }

            composable(Screen.Equalizer.route) {
                EqualizerScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.Settings.route) {
                SettingsScreen(viewModel, onBack = { navController.popBackStack() })
            }

            composable(Screen.VideoPlayer.route) { entry ->
                val videoId = entry.arguments?.getString("videoId")?.toLongOrNull()
                if (videoId == null) {
                    navController.popBackStack()
                } else {
                    VideoPlayerScreen(
                        viewModel = viewModel,
                        videoId = videoId,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

private fun iconFor(screen: Screen) = when (screen) {
    Screen.Library -> Icons.Filled.LibraryMusic
    Screen.Search -> Icons.Filled.Search
    Screen.Playlists -> Icons.Filled.PlaylistPlay
    Screen.Folders -> Icons.Filled.Folder
    Screen.Favorites -> Icons.Filled.Favorite
    Screen.Equalizer -> Icons.Filled.Equalizer
    Screen.Settings -> Icons.Filled.Settings
    Screen.NowPlaying -> Icons.Filled.LibraryMusic
    Screen.VideoPlayer -> Icons.Filled.LibraryMusic
}
