package com.example.musicplayer.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.musicplayer.viewmodel.MusicViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onOpenEqualizer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenVideo: (Long) -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.scanLibrary()
        viewModel.scanVideos()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ACE Media Player") },
                actions = {
                    IconButton(onClick = onOpenFavorites) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Favorites")
                    }
                    IconButton(onClick = onOpenEqualizer) {
                        Icon(Icons.Filled.Equalizer, contentDescription = "Equalizer")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("🎵 Audios") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("🎬 Videos") },
                    icon = { Icon(Icons.Filled.VideoLibrary, contentDescription = null) }
                )
            }

            if (selectedTab == 0) {
                AudioLibraryContent(
                    viewModel = viewModel,
                    onOpenEqualizer = onOpenEqualizer,
                    onOpenSettings = onOpenSettings,
                    onOpenFavorites = onOpenFavorites
                )
            } else {
                VideoLibraryScreen(
                    viewModel = viewModel,
                    onVideoClick = onOpenVideo
                )
            }
        }
    }
}

@Composable
private fun AudioLibraryContent(
    viewModel: MusicViewModel,
    onOpenEqualizer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFavorites: () -> Unit
) {
    val songs by viewModel.allSongs.collectAsState()
    val favorites by viewModel.favoriteIds.collectAsState()

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        if (songs.isEmpty()) {
            item {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 120.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("Scanning device for music...")
                }
            }
        } else {
            items(songs, key = { it.id }) { song ->
                com.example.musicplayer.ui.components.SongRow(
                    song = song,
                    albumArtUri = viewModel.albumArtUri(song.albumId),
                    isFavorite = favorites.contains(song.id),
                    onClick = { viewModel.playSongs(songs, songs.indexOf(song)) },
                    onFavoriteClick = { viewModel.toggleFavorite(song.id) }
                )
            }
        }
    }
}
