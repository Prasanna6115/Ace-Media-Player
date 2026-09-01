package com.example.musicplayer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.musicplayer.ui.components.SongRow
import com.example.musicplayer.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    onOpenEqualizer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFavorites: () -> Unit
) {
    val songs by viewModel.allSongs.collectAsState()
    val favorites by viewModel.favoriteIds.collectAsState()

    LaunchedEffect(Unit) { viewModel.scanLibrary() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Music") },
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
        if (songs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Scanning device for music (mp3, flac, wav, aac, m4a, ogg, opus)...")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(songs, key = { it.id }) { song ->
                    SongRow(
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
}
