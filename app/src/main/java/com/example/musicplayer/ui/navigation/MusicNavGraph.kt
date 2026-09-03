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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
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
    data object VideoPlayer : Screen("video_player/{videoId}", "Video Player")

    fun videoRoute(videoId: Long): String = "video_player/$videoId"
}

private val bottomTabs = listOf(
    Screen.Library,
    Screen.Search,
    Screen.Playlists,
    Screen.Folders
)

@Composable
fun MusicNavGraph(viewModel: MusicViewModel) {
    val navController = rememberNavController()
    val playbackState by viewModel.playbackState.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val isVideoPlayer = destination?.route?.startsWith("video_player/") == true

    Scaffold(
        bottomBar = {
            if (!isVideoPlayer) {
                Column {
                    if (
                        playbackState.currentSong != null &&
                        destination?.hierarchy?.any { it.route == Screen.NowPlaying.route } != true
                    ) {
                        MiniPlayer(
                            viewModel = viewModel,
                            onExpand = { navController.navigate(Screen.NowPlaying.route) }
                        )
                    }

                    NavigationBar {
                        bottomTabs.forEach { screen ->
                            NavigationBarItem(
                                selected = destination?.hierarchy?.any { it.route == screen.route } == true,
                                onClick = {
                                    if (screen == Screen.Library) {
                                        navController.popBackStack(Screen.Library.route, false)
                                    } else {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(iconFor(screen), contentDescription = screen.label)
                                },
                                label = { Text(screen.label) }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        val hostModifier = if (isVideoPlayer) Modifier else Modifier.padding(padding)

        NavHost(
            navController = navController,
            startDestination = Screen.Library.route,
            modifier = hostModifier
        ) {
            composable(Screen.Library.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onOpenEqualizer = { navController.navigate(Screen.Equalizer.route) },
                    onOpenSettings = { navController.navigate(Screen.Settings.route) },
                    onOpenFavorites = { navController.navigate(Screen.Favorites.route) },
                    onOpenVideo = { videoId -> navController.navigate(Screen.VideoPlayer.videoRoute(videoId)) }
                )
            }

            composable(Screen.Search.route) { SearchScreen(viewModel = viewModel) }
            composable(Screen.Playlists.route) { PlaylistScreen(viewModel = viewModel) }
            composable(Screen.Folders.route) {
                FolderBrowserScreen(
                    viewModel = viewModel,
                    onOpenVideo = { videoId -> navController.navigate(Screen.VideoPlayer.videoRoute(videoId)) }
                )
            }
            composable(Screen.Favorites.route) { FavoritesScreen(viewModel = viewModel) }

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
                SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.VideoPlayer.route,
                arguments = listOf(navArgument("videoId") { type = NavType.LongType })
            ) { entry ->
                val videoId = entry.arguments?.getLong("videoId") ?: return@composable
                VideoPlayerScreen(
                    viewModel = viewModel,
                    videoId = videoId,
                    onBack = {
                        viewModel.closeVideoAndRestoreAudio()
                        navController.popBackStack()
                    }
                )
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
