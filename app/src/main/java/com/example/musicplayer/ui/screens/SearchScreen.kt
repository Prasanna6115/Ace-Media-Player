package com.example.musicplayer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.musicplayer.ui.components.SongRow
import com.example.musicplayer.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: MusicViewModel) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val favorites by viewModel.favoriteIds.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::updateSearchQuery,
            label = { Text("Search title, artist, or album") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn {
            items(results, key = { it.id }) { song ->
                SongRow(
                    song = song,
                    albumArtUri = viewModel.albumArtUri(song.albumId),
                    isFavorite = favorites.contains(song.id),
                    onClick = { viewModel.playSongs(results, results.indexOf(song)) },
                    onFavoriteClick = { viewModel.toggleFavorite(song.id) }
                )
            }
        }
    }
}
