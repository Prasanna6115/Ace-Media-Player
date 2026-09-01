package com.example.musicplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.musicplayer.data.PlaylistEntity
import com.example.musicplayer.ui.components.SongRow
import com.example.musicplayer.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(viewModel: MusicViewModel) {
    val playlists by viewModel.playlists.collectAsState()
    var selectedPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddSongsDialog by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name -> viewModel.createPlaylist(name); showCreateDialog = false }
        )
    }

    if (selectedPlaylist == null) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Playlists") }) },
            floatingActionButton = {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "New playlist")
                }
            }
        ) { padding ->
            if (playlists.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("No playlists yet - tap + to create one")
                }
            } else {
                LazyColumn(modifier = Modifier.padding(padding)) {
                    items(playlists, key = { it.playlistId }) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPlaylist = playlist }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.PlaylistPlay, contentDescription = null)
                            Spacer(Modifier.width(16.dp))
                            Text(playlist.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.deletePlaylist(playlist.playlistId) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    } else {
        val playlist = selectedPlaylist!!
        val allSongs by viewModel.allSongs.collectAsState()
        val songIds by viewModel.songIdsForPlaylist(playlist.playlistId).collectAsState(initial = emptyList())
        val favorites by viewModel.favoriteIds.collectAsState()
        val playlistSongs = songIds.mapNotNull { id -> allSongs.find { it.id == id } }

        if (showAddSongsDialog) {
            AddSongsDialog(
                allSongs = allSongs,
                alreadyAdded = songIds.toSet(),
                onDismiss = { showAddSongsDialog = false },
                onAdd = { songId -> viewModel.addSongToPlaylist(playlist.playlistId, songId) }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(playlist.name) },
                    navigationIcon = {
                        IconButton(onClick = { selectedPlaylist = null }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddSongsDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add songs")
                }
            }
        ) { padding ->
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(playlistSongs, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        albumArtUri = viewModel.albumArtUri(song.albumId),
                        isFavorite = favorites.contains(song.id),
                        onClick = { viewModel.playSongs(playlistSongs, playlistSongs.indexOf(song)) },
                        onFavoriteClick = { viewModel.toggleFavorite(song.id) },
                        trailingContent = {
                            IconButton(onClick = { viewModel.removeSongFromPlaylist(playlist.playlistId, song.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New playlist") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name) }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddSongsDialog(
    allSongs: List<com.example.musicplayer.data.Song>,
    alreadyAdded: Set<Long>,
    onDismiss: () -> Unit,
    onAdd: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add songs") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(allSongs, key = { it.id }) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !alreadyAdded.contains(song.id)) { onAdd(song.id) }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            song.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (alreadyAdded.contains(song.id)) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}
