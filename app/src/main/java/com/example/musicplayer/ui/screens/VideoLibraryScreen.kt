package com.example.musicplayer.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.musicplayer.ui.components.VideoRow
import com.example.musicplayer.viewmodel.MusicViewModel

@Composable
fun VideoLibraryScreen(
    viewModel: MusicViewModel,
    onVideoClick: (Long) -> Unit
) {
    val videos by viewModel.allVideos.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.scanVideos()
    }

    if (videos.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No videos found on this device.")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(videos, key = { it.id }) { video ->
                VideoRow(
                    video = video,
                    onClick = { onVideoClick(video.id) }
                )
            }
        }
    }
}
