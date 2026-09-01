package com.example.musicplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.musicplayer.ui.components.SongRow
import com.example.musicplayer.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderBrowserScreen(viewModel: MusicViewModel) {
    val songs by viewModel.allSongs.collectAsState()
    val favorites by viewModel.favoriteIds.collectAsState()
    var selectedFolder by remember { mutableStateOf<String?>(null) }

    val foldersMap = remember(songs) { songs.groupBy { it.folderName } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedFolder ?: "Folders") },
                navigationIcon = {
                    if (selectedFolder != null) {
                        IconButton(onClick = { selectedFolder = null }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (selectedFolder == null) {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(foldersMap.keys.sorted()) { folder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFolder = folder }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Icon(Icons.Filled.Folder, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(folder, style = MaterialTheme.typography.titleMedium)
                            Text("${foldersMap[folder]?.size ?: 0} songs", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        } else {
            val folderSongs = foldersMap[selectedFolder].orEmpty()
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(folderSongs, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        albumArtUri = viewModel.albumArtUri(song.albumId),
                        isFavorite = favorites.contains(song.id),
                        onClick = { viewModel.playSongs(folderSongs, folderSongs.indexOf(song)) },
                        onFavoriteClick = { viewModel.toggleFavorite(song.id) }
                    )
                }
            }
        }
    }
}
