package com.example.musicplayer.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.example.musicplayer.playback.VideoQuality
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
    var fullscreen by remember { mutableStateOf(true) }
    var controlsLocked by remember { mutableStateOf(false) }
    var showUnlockPrompt by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }

    val selectedVideo = videos.firstOrNull { it.id == videoId } ?: state.currentVideo

    fun touch() {
        controlsVisible = true
        interactionTick++
    }

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
        touch()
    }

    fun exitFullscreen() {
        fullscreen = false
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        applyImmersive(false)
        touch()
    }

    fun toggleControlsLock() {
        controlsLocked = !controlsLocked
        if (controlsLocked) {
            fullscreen = true
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            applyImmersive(true)
            controlsVisible = false
            showUnlockPrompt = false
            showSettings = false
        } else {
            touch()
        }
    }

    DisposableEffect(Unit) {
        enterFullscreen()
        onDispose {
            applyImmersive(false)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    LaunchedEffect(videoId, videos.size) {
        videos.firstOrNull { it.id == videoId }?.let(viewModel::playVideo)
    }

    LaunchedEffect(showUnlockPrompt) {
        if (showUnlockPrompt) {
            delay(2500)
            showUnlockPrompt = false
        }
    }

    LaunchedEffect(interactionTick, controlsLocked) {
        if (!controlsLocked) {
            delay(3500)
            controlsVisible = false
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.pollVideoPosition()
            delay(400)
        }
    }

    BackHandler {
        when {
            fullscreen -> exitFullscreen()
            else -> onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val player = viewModel.videoPlayer()
        if (player != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        this.player = player
                        keepScreenOn = true
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                update = { it.player = player },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            if (controlsLocked) {
                                controlsVisible = false
                                showUnlockPrompt = true
                                interactionTick++
                            } else {
                                touch()
                            }
                        }
                    }
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        if (controlsLocked && showUnlockPrompt) {
            Card(
                modifier = Modifier.align(Alignment.Center),
                colors = CardDefaults.cardColors(containerColor = Color(0xDD15171C))
            ) {
                TextButton(
                    onClick = { controlsLocked = false; showUnlockPrompt = false; touch() },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.LockOpen, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Unlock controls", color = Color.White)
                }
            }
        } else if (controlsVisible) {
            VideoControls(
                title = selectedVideo?.title ?: "Video",
                state = state,
                fullscreen = fullscreen,
                showSettings = showSettings,
                onBack = {
                    if (fullscreen) exitFullscreen() else onBack()
                },
                onMore = { showSettings = !showSettings; touch() },
                onSeek = { position -> viewModel.videoSeekTo(position); touch() },
                onPrevious = { viewModel.videoPrevious(); touch() },
                onPlayPause = { viewModel.videoPlayPause(); touch() },
                onNext = { viewModel.videoNext(); touch() },
                onShuffle = { viewModel.videoToggleShuffle(); touch() },
                onRepeat = { viewModel.videoCycleRepeatMode(); touch() },
                onVolume = {
                    viewModel.videoSetVolume(if (state.volume > 0f) 0f else 1f)
                    touch()
                },
                onSettings = { showSettings = !showSettings; touch() },
                onLock = ::toggleControlsLock,
                onFullscreen = {
                    if (fullscreen) exitFullscreen() else enterFullscreen()
                },
                onSelectQuality = { quality ->
                    viewModel.setVideoQuality(quality)
                    showSettings = false
                    touch()
                }
            )
        }

        if (state.errorMessage != null) {
            Card(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xEE15171C))
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(state.errorMessage!!, color = Color.White)
                    Spacer(Modifier.size(8.dp))
                    TextButton(onClick = viewModel.videoController::clearError) {
                        Text("OK", color = Color(0xFFFFB000))
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoControls(
    title: String,
    state: com.example.musicplayer.playback.VideoPlaybackUiState,
    fullscreen: Boolean,
    showSettings: Boolean,
    onBack: () -> Unit,
    onMore: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onVolume: () -> Unit,
    onSettings: () -> Unit,
    onLock: () -> Unit,
    onFullscreen: () -> Unit,
    onSelectQuality: (VideoQuality) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.58f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Text(
                    title,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onMore) {
                    Icon(Icons.Filled.MoreVert, "More", tint = Color.White)
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onSeek((state.positionMs - 10_000L).coerceAtLeast(0L)) }) {
                    Icon(Icons.Filled.Replay10, "Rewind 10 seconds", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Filled.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(38.dp))
                }
                FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFFF6A00))
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Filled.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(38.dp))
                }
                IconButton(onClick = { onSeek(state.positionMs + 10_000L) }) {
                    Icon(Icons.Filled.Forward10, "Forward 10 seconds", tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.68f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                val max = state.durationMs.coerceAtLeast(1L).toFloat()
                Slider(
                    value = state.positionMs.toFloat().coerceIn(0f, max),
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..max,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFF6A00),
                        activeTrackColor = Color(0xFFFF6A00)
                    )
                )
                Row(Modifier.fillMaxWidth()) {
                    Text(formatVideoMs(state.positionMs), color = Color.White, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.weight(1f))
                    Text(formatVideoMs(state.durationMs), color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onShuffle) {
                        Icon(Icons.Filled.Shuffle, "Shuffle", tint = if (state.shuffleEnabled) Color(0xFFFF6A00) else Color.White)
                    }
                    IconButton(onClick = onRepeat) {
                        Icon(Icons.Filled.Repeat, "Repeat", tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) Color(0xFFFF6A00) else Color.White)
                    }
                    IconButton(onClick = onVolume) {
                        Icon(
                            if (state.volume > 0f) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                            "Volume",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, "Video settings", tint = Color.White)
                    }
                    IconButton(onClick = onLock) {
                        Icon(Icons.Filled.Lock, "Lock controls", tint = Color.White)
                    }
                    IconButton(onClick = onFullscreen) {
                        Icon(
                            if (fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                            "Fullscreen",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        if (showSettings) {
            VideoSettingsMenu(
                state = state,
                onSelectQuality = onSelectQuality,
                onClose = onSettings
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.VideoSettingsMenu(
    state: com.example.musicplayer.playback.VideoPlaybackUiState,
    onSelectQuality: (VideoQuality) -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 56.dp, end = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xEE15171C))
    ) {
        Column(Modifier.padding(8.dp)) {
            Text("Video quality", color = Color.White, modifier = Modifier.padding(8.dp))
            VideoQuality.values().forEach { quality ->
                DropdownMenuItem(
                    text = {
                        Text(
                            if (state.videoQuality == quality.label) "✓ ${quality.label}" else quality.label,
                            color = Color.White
                        )
                    },
                    onClick = { onSelectQuality(quality) }
                )
            }
            DropdownMenuItem(
                text = { Text("Close", color = Color.White) },
                onClick = onClose
            )
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
