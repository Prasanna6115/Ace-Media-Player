package com.example.musicplayer.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.musicplayer.ui.components.SongRow
import com.example.musicplayer.viewmodel.MusicViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(viewModel: MusicViewModel) {
    val allSongs by viewModel.allSongs.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val favoriteSongs = allSongs.filter { favoriteIds.contains(it.id) }

    Scaffold(
    topBar = {
        TopAppBar(
            title = { Text("Favorites") }
        )
    }
) { padding ->
        if (favoriteSongs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No favorites yet - tap the heart on any song")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(favoriteSongs, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        albumArtUri = viewModel.albumArtUri(song.albumId),
                        isFavorite = true,
                        onClick = { viewModel.playSongs(favoriteSongs, favoriteSongs.indexOf(song)) },
                        onFavoriteClick = { viewModel.toggleFavorite(song.id) }
                    )
                }
            }
        }
    }
}
