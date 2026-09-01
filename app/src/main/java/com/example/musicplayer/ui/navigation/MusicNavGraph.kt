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

    val backStackEntry by navController
        .currentBackStackEntryAsState()

    val currentRoute = backStackEntry?.destination

    Scaffold(
        bottomBar = {

            Column {

                // Mini Player
                // Hidden on Now Playing screen
                if (
                    playbackState.currentSong != null &&
                    currentRoute?.hierarchy?.any {
                        it.route == Screen.NowPlaying.route
                    } != true
                ) {

                    MiniPlayer(
                        viewModel = viewModel,
                        onExpand = {
                            navController.navigate(
                                Screen.NowPlaying.route
                            )
                        }
                    )
                }

                // Bottom Navigation Bar
                NavigationBar {

                    bottomTabs.forEach { screen ->

                        NavigationBarItem(

                            selected =
                                currentRoute?.hierarchy?.any {
                                    it.route == screen.route
                                } == true,

                            onClick = {

                                navController.navigate(
                                    screen.route
                                ) {

                                    popUpTo(
                                        navController.graph
                                            .findStartDestination()
                                            .id
                                    ) {
                                        saveState = true
                                    }

                                    launchSingleTop = true

                                    restoreState = true
                                }
                            },

                            icon = {
                                Icon(
                                    imageVector = iconFor(screen),
                                    contentDescription = screen.label
                                )
                            },

                            label = {
                                Text(screen.label)
                            }
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

            // Library
            composable(Screen.Library.route) {

                LibraryScreen(
                    viewModel = viewModel,

                    onOpenEqualizer = {
                        navController.navigate(
                            Screen.Equalizer.route
                        )
                    },

                    onOpenSettings = {
                        navController.navigate(
                            Screen.Settings.route
                        )
                    },

                    onOpenFavorites = {
                        navController.navigate(
                            Screen.Favorites.route
                        )
                    }
                )
            }

            // Search
            composable(Screen.Search.route) {

                SearchScreen(
                    viewModel = viewModel
                )
            }

            // Playlists
            composable(Screen.Playlists.route) {

                PlaylistScreen(
                    viewModel = viewModel
                )
            }

            // Folders
            composable(Screen.Folders.route) {

                FolderBrowserScreen(
                    viewModel = viewModel
                )
            }

            // Favorites
            composable(Screen.Favorites.route) {

                FavoritesScreen(
                    viewModel = viewModel
                )
            }

            // Now Playing
            composable(Screen.NowPlaying.route) {

                NowPlayingScreen(
                    viewModel = viewModel,

                    onBack = {
                        navController.popBackStack()
                    },

                    onOpenEqualizer = {
                        navController.navigate(
                            Screen.Equalizer.route
                        )
                    }
                )
            }

            // Equalizer
            composable(Screen.Equalizer.route) {

                EqualizerScreen(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Settings
            composable(Screen.Settings.route) {

                SettingsScreen(
                    viewModel = viewModel,

                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

private fun iconFor(screen: Screen) = when (screen) {

    Screen.Library ->
        Icons.Filled.LibraryMusic

    Screen.Search ->
        Icons.Filled.Search

    Screen.Playlists ->
        Icons.Filled.PlaylistPlay

    Screen.Folders ->
        Icons.Filled.Folder

    Screen.Favorites ->
        Icons.Filled.Favorite

    Screen.Equalizer ->
        Icons.Filled.Equalizer

    Screen.Settings ->
        Icons.Filled.Settings

    Screen.NowPlaying ->
        Icons.Filled.LibraryMusic
}
