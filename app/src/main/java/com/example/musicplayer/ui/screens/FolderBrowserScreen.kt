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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.musicplayer.data.Song
import com.example.musicplayer.data.Video
import com.example.musicplayer.ui.components.SongRow
import com.example.musicplayer.ui.components.VideoRow
import com.example.musicplayer.viewmodel.MusicViewModel

private data class FolderMedia(
    val key: String,
    val song: Song? = null,
    val video: Video? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderBrowserScreen(
    viewModel: MusicViewModel,
    onOpenVideo: (Long) -> Unit
) {
    val songs by viewModel.allSongs.collectAsState()
    val videos by viewModel.allVideos.collectAsState()
    val favorites by viewModel.favoriteIds.collectAsState()
    var selectedFolder by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.scanLibrary()
        viewModel.scanVideos()
    }

    val foldersMap = remember(songs, videos) {
        buildMap<String, List<FolderMedia>> {
            songs.groupBy { it.folderName }.forEach { (folder, folderSongs) ->
                put(folder, folderSongs.map { FolderMedia("a_${it.id}", song = it) })
            }
            videos.groupBy { it.folderName }.forEach { (folder, folderVideos) ->
                val existing = get(folder).orEmpty()
                put(folder, existing + folderVideos.map { FolderMedia("v_${it.id}", video = it) })
            }
        }
    }

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
            if (foldersMap.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("No audio or video files found.")
                }
            } else {
                LazyColumn(modifier = Modifier.padding(padding)) {
                    items(foldersMap.keys.sorted(), key = { it }) { folder ->
                        val count = foldersMap[folder].orEmpty().size
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFolder = folder }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Folder, contentDescription = null)
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(folder, style = MaterialTheme.typography.titleMedium)
                                Text("$count media", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        } else {
            val media = foldersMap[selectedFolder].orEmpty()
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(media, key = { it.key }) { item ->
                    item.song?.let { song ->
                        SongRow(
                            song = song,
                            albumArtUri = viewModel.albumArtUri(song.albumId),
                            isFavorite = favorites.contains(song.id),
                            onClick = {
                                val index = media.indexOfFirst { it.song?.id == song.id }
                                val audioOnly = media.mapNotNull { it.song }
                                viewModel.playSongs(audioOnly, audioOnly.indexOfFirst { it.id == song.id }.coerceAtLeast(0))
                            },
                            onFavoriteClick = { viewModel.toggleFavorite(song.id) }
                        )
                    }
                    item.video?.let { video ->
                        VideoRow(video = video, onClick = { onOpenVideo(video.id) })
                    }
                }
            }
        }
    }
}
