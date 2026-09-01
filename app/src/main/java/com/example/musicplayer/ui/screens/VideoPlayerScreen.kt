package com.example.musicplayer.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.example.musicplayer.playback.PlaybackService
import com.example.musicplayer.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    viewModel: MusicViewModel,
    videoId: Long,
    onBack: () -> Unit
) {
    val video = viewModel.allVideos.collectAsState().value.firstOrNull { it.id == videoId }
    var error by remember(videoId) { mutableStateOf<String?>(null) }

    LaunchedEffect(videoId) {
        if (video == null) {
            error = "Video is no longer available."
        } else {
            error = null
            viewModel.playVideo(video)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPlayback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(video?.title ?: "Video") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.onBackground)
            } else {
                val player = PlaybackService.instance?.player
                if (player != null) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                        factory = { context ->
                            PlayerView(context).apply {
                                this.player = player
                                useController = true
                                controllerAutoShow = true
                                controllerHideOnTouch = true
                                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                            }
                        },
                        update = { it.player = player }
                    )
                } else {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
