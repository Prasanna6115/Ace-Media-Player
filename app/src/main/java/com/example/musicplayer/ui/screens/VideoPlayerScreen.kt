package com.example.musicplayer.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.example.musicplayer.viewmodel.MusicViewModel
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerScreen(
    viewModel: MusicViewModel,
    videoId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val state by viewModel.videoPlaybackState.collectAsState()
    val videos by viewModel.allVideos.collectAsState()

    var controlsVisible by remember { mutableStateOf(true) }
    var fullscreen by remember { mutableStateOf(false) }
    var orientationLocked by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }

    val selectedVideo = videos.firstOrNull { it.id == videoId } ?: state.currentVideo

    fun applyImmersive(enabled: Boolean) {
        activity?.window?.let { window ->
            if (enabled) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    )
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    fun enterFullscreen() {
        fullscreen = true
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        applyImmersive(true)
        controlsVisible = true
    }

    fun exitFullscreen() {
        fullscreen = false
        orientationLocked = false
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        applyImmersive(false)
        controlsVisible = true
    }

    fun toggleOrientationLock() {
        if (!fullscreen) enterFullscreen()
        orientationLocked = !orientationLocked
        activity?.requestedOrientation = if (orientationLocked) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            applyImmersive(false)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    LaunchedEffect(videoId, videos.size) {
        if (videos.isNotEmpty()) {
            viewModel.playVideo(videos.firstOrNull { it.id == videoId } ?: return@LaunchedEffect)
        }
    }

    LaunchedEffect(interactionTick) {
        delay(3500)
        controlsVisible = false
    }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.pollVideoPosition()
            delay(500)
        }
    }

    BackHandler {
        if (fullscreen) exitFullscreen() else onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures {
                    controlsVisible = true
                    interactionTick++
                }
            }
    ) {
        val player = viewModel.videoPlayer()
        if (player != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        this.player = player
                        keepScreenOn = true
                    }
                },
                update = { it.player = player },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (controlsVisible) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (fullscreen) exitFullscreen() else onBack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Text(
                        text = selectedVideo?.title ?: "Video",
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showSettings = !showSettings }) {
                        Icon(Icons.Filled.MoreVert, "More", tint = Color.White)
                    }
                }

                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.videoSeekTo((state.positionMs - 10_000L).coerceAtLeast(0L)) }) {
                        Icon(Icons.Filled.Replay10, "Rewind 10 seconds", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = viewModel::videoPrevious) {
                        Icon(Icons.Filled.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(38.dp))
                    }
                    FilledIconButton(
                        onClick = viewModel::videoPlayPause,
                        modifier = Modifier.size(68.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFFFF6A00)
                        )
                    ) {
                        Icon(
                            if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    IconButton(onClick = viewModel::videoNext) {
                        Icon(Icons.Filled.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(38.dp))
                    }
                    IconButton(onClick = { viewModel.videoSeekTo(state.positionMs + 10_000L) }) {
                        Icon(Icons.Filled.Forward10, "Forward 10 seconds", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }

                Spacer(Modifier.weight(1f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Slider(
                        value = state.positionMs.toFloat().coerceIn(0f, state.durationMs.coerceAtLeast(1L).toFloat()),
                        onValueChange = { viewModel.videoSeekTo(it.toLong()) },
                        valueRange = 0f..state.durationMs.coerceAtLeast(1L).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFF6A00),
                            activeTrackColor = Color(0xFFFF6A00)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(formatVideoMs(state.positionMs), color = Color.White, style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.weight(1f))
                        Text(formatVideoMs(state.durationMs), color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = viewModel::videoToggleShuffle) {
                            Icon(Icons.Filled.Shuffle, "Shuffle", tint = Color.White)
                        }
                        IconButton(onClick = viewModel::videoCycleRepeatMode) {
                            Icon(Icons.Filled.Repeat, "Repeat", tint = Color.White)
                        }
                        IconButton(onClick = {
                            viewModel.videoSetVolume(if (state.volume > 0f) 0f else 1f)
                        }) {
                            Icon(
                                if (state.volume > 0f) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                                "Volume",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { showSettings = !showSettings }) {
                            Icon(Icons.Filled.Settings, "Settings", tint = Color.White)
                        }
                        IconButton(onClick = ::toggleOrientationLock) {
                            Icon(
                                if (orientationLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                "Orientation lock",
                                tint = if (orientationLocked) Color(0xFFFF6A00) else Color.White
                            )
                        }
                        IconButton(onClick = {
                            if (fullscreen) exitFullscreen() else enterFullscreen()
                        }) {
                            Icon(
                                if (fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                "Fullscreen",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        if (state.errorMessage != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(state.errorMessage!!)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = viewModel.videoController::clearError) {
                        Text("OK")
                    }
                }
            }
        }

        if (showSettings && controlsVisible) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 52.dp, end = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xEE15171C)
                )
            ) {
                Column(Modifier.padding(8.dp)) {
                    Text("Playback", color = Color.White, modifier = Modifier.padding(8.dp))
                    TextButton(onClick = {
                        viewModel.videoSeekTo((state.positionMs - 10_000L).coerceAtLeast(0L))
                        showSettings = false
                    }) { Text("Rewind 10 seconds", color = Color.White) }
                    TextButton(onClick = {
                        viewModel.videoSeekTo(state.positionMs + 10_000L)
                        showSettings = false
                    }) { Text("Forward 10 seconds", color = Color.White) }
                }
            }
        }
    }
}

private fun formatVideoMs(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0L) / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
