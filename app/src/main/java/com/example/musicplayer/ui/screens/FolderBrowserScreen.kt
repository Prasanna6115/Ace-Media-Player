package com.example.musicplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.musicplayer.data.Song
import com.example.musicplayer.data.Video
import com.example.musicplayer.ui.components.SongRow
import com.example.musicplayer.ui.components.VideoRow
import com.example.musicplayer.viewmodel.MusicViewModel

private sealed interface FolderMedia {
    val key: String
    val title: String

    data class Audio(val song: Song) : FolderMedia {
        override val key: String = "audio:${song.id}"
        override val title: String = song.title
    }

    data class Video(val video: com.example.musicplayer.data.Video) : FolderMedia {
        override val key: String = "video:${video.id}"
        override val title: String = video.title
    }
}

@Composable
fun FolderBrowserScreen(
    viewModel: MusicViewModel,
    onOpenVideo: (Long) -> Unit
) {
    val songs by viewModel.allSongs.collectAsState()
    val videos by viewModel.allVideos.collectAsState()
    val favorites by viewModel.favoriteIds.collectAsState()
    var selectedFolder by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.scanLibrary()
        viewModel.scanVideos()
    }

    val folders = remember(songs, videos) {
        (songs.map { it.folderName } + videos.map { it.folderName })
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val folderMedia = remember(songs, videos, selectedFolder) {
        val folder = selectedFolder ?: return@remember emptyList<FolderMedia>()
        buildList {
            songs.filter { it.folderName == folder }.forEach { add(FolderMedia.Audio(it)) }
            videos.filter { it.folderName == folder }.forEach { add(FolderMedia.Video(it)) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedFolder ?: "Folders") },
                navigationIcon = {
                    if (selectedFolder != null) {
                        androidx.compose.material3.IconButton(onClick = { selectedFolder = null }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (selectedFolder == null) {
            if (folders.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(padding).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No media folders found.")
                }
            } else {
                LazyColumn(modifier = Modifier.padding(padding)) {
                    items(folders, key = { it }) { folder ->
                        val audioCount = songs.count { it.folderName == folder }
                        val videoCount = videos.count { it.folderName == folder }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFolder = folder }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Folder, contentDescription = null)
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(folder, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = folderCountLabel(audioCount, videoCount),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            if (videoCount > 0) {
                                Icon(Icons.Filled.VideoLibrary, contentDescription = "Contains videos")
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(folderMedia, key = { it.key }) { item ->
                    when (item) {
                        is FolderMedia.Audio -> {
                            val song = item.song
                            SongRow(
                                song = song,
                                albumArtUri = viewModel.albumArtUri(song.albumId),
                                isFavorite = favorites.contains(song.id),
                                onClick = {
                                    val audioOnly = folderMedia.mapNotNull {
                                        (it as? FolderMedia.Audio)?.song
                                    }
                                    val index = audioOnly.indexOfFirst { it.id == song.id }
                                    viewModel.playSongs(audioOnly, index.coerceAtLeast(0))
                                },
                                onFavoriteClick = { viewModel.toggleFavorite(song.id) }
                            )
                        }
                        is FolderMedia.Video -> {
                            VideoRow(
                                video = item.video,
                                onClick = { onOpenVideo(item.video.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun folderCountLabel(audio: Int, video: Int): String {
    val parts = buildList {
        if (audio > 0) add("$audio ${if (audio == 1) "song" else "songs"}")
        if (video > 0) add("$video ${if (video == 1) "video" else "videos"}")
    }
    return parts.joinToString(" • ").ifBlank { "Empty" }
}
