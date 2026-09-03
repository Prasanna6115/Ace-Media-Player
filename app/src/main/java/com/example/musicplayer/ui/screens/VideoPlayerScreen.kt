package com.example.musicplayer.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.Brightness6
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
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.musicplayer.playback.SubtitleTrack
import com.example.musicplayer.viewmodel.MusicViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class GestureKind { NONE, TAP, HORIZONTAL_SEEK, VOLUME, BRIGHTNESS }
private enum class DisplayMode(val label: String, val resizeMode: Int) {
    FIT("Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("Fill", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    CROP("Crop", AspectRatioFrameLayout.RESIZE_MODE_ZOOM)
}

private data class GestureOverlayState(
    val kind: GestureKind = GestureKind.NONE,
    val value: String = "",
    val icon: ImageVector? = null
)

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
    val scope = rememberCoroutineScope()

    var controlsVisible by remember { mutableStateOf(true) }
    var fullscreen by remember { mutableStateOf(true) }
    var controlsLocked by remember { mutableStateOf(false) }
    var showUnlockPrompt by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var gestureOverlay by remember { mutableStateOf(GestureOverlayState()) }
    var displayMode by remember { mutableStateOf(DisplayMode.FIT) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewPosition by remember { mutableLongStateOf(0L) }
    var previewJob by remember { mutableStateOf<Job?>(null) }
    var draggingSeek by remember { mutableStateOf(false) }

    val selectedVideo = videos.firstOrNull { it.id == videoId } ?: state.currentVideo

    fun showControls() {
        controlsVisible = true
        interactionTick++
    }

    fun applyImmersive(enabled: Boolean) {
        activity?.window?.let { window ->
            if (enabled) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
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
        showControls()
    }

    fun exitFullscreen() {
        fullscreen = false
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        applyImmersive(false)
        showControls()
    }

    fun toggleLock() {
        controlsLocked = !controlsLocked
        showSettings = false
        if (controlsLocked) {
            fullscreen = true
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            applyImmersive(true)
            controlsVisible = false
            showUnlockPrompt = false
        } else {
            showControls()
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
            delay(2200)
            showUnlockPrompt = false
        }
    }

    LaunchedEffect(interactionTick, controlsLocked, draggingSeek) {
        if (!controlsLocked && !draggingSeek) {
            delay(3500)
            controlsVisible = false
            showSettings = false
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
            showSettings -> showSettings = false
            fullscreen -> exitFullscreen()
            else -> onBack()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val player = viewModel.videoPlayer()
        if (player != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        this.player = player
                        keepScreenOn = true
                        resizeMode = displayMode.resizeMode
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                update = {
                    it.player = player
                    it.resizeMode = displayMode.resizeMode
                },
                modifier = Modifier.fillMaxSize()
            )

            val latestState by androidx.compose.runtime.rememberUpdatedState(state)
            val latestActivity by androidx.compose.runtime.rememberUpdatedState(activity)

            // Gesture layer: horizontal drag = seek; left vertical = brightness; right vertical = volume.
            // These gestures deliberately hide the normal controls while the gesture overlay is shown at the top.
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp, bottom = 118.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { },
                            onDragCancel = { gestureOverlay = GestureOverlayState(); controlsVisible = false },
                            onDragEnd = {
                                interactionTick++
                                scope.launch {
                                    delay(700)
                                    gestureOverlay = GestureOverlayState()
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val w = size.width.toFloat().coerceAtLeast(1f)
                                val h = size.height.toFloat().coerceAtLeast(1f)
                                val x = change.position.x
                                val horizontal = abs(dragAmount.x) > abs(dragAmount.y)
                                if (horizontal) {
                                    val duration = latestState.durationMs.coerceAtLeast(1L)
                                    val delta = (dragAmount.x / w * duration * 0.65f).toLong()
                                    val target = (latestState.positionMs + delta).coerceIn(0L, duration)
                                    viewModel.videoSeekTo(target)
                                    gestureOverlay = GestureOverlayState(
                                        kind = GestureKind.HORIZONTAL_SEEK,
                                        value = "Seek ${formatVideoMs(target)}",
                                        icon = if (delta >= 0) Icons.Filled.Forward10 else Icons.Filled.Replay10
                                    )
                                } else if (x < w * 0.45f) {
                                    val window = latestActivity?.window
                                    if (window != null) {
                                        val current = window.attributes.screenBrightness.takeIf { it >= 0f } ?: 0.5f
                                        val next = (current - dragAmount.y / h * 1.25f).coerceIn(0.05f, 1f)
                                        val params = window.attributes
                                        params.screenBrightness = next
                                        window.attributes = params
                                        gestureOverlay = GestureOverlayState(
                                            kind = GestureKind.BRIGHTNESS,
                                            value = "Brightness ${((next * 100f).roundToInt())}%",
                                            icon = Icons.Filled.Brightness6
                                        )
                                    }
                                } else if (x > w * 0.55f) {
                                    val next = (latestState.volume - dragAmount.y / h * 1.25f).coerceIn(0f, 1f)
                                    viewModel.videoSetVolume(next)
                                    gestureOverlay = GestureOverlayState(
                                        kind = GestureKind.VOLUME,
                                        value = "Volume ${((next * 100f).roundToInt())}%",
                                        icon = if (next <= 0f) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp
                                    )
                                }
                                controlsVisible = false
                            }
                        )
                    }
            )

            // Tap layer is separate so gesture drags never make the control UI pop up.
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp, bottom = 118.dp)
                    .pointerInput(controlsLocked) {
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                if (controlsLocked) {
                                    showUnlockPrompt = true
                                    return@detectTapGestures
                                }
                                val w = size.width.toFloat().coerceAtLeast(1f)
                                val duration = latestState.durationMs.coerceAtLeast(1L)
                                when {
                                    offset.x < w * 0.35f -> {
                                        val target = (latestState.positionMs - 10_000L).coerceAtLeast(0L)
                                        viewModel.videoSeekTo(target)
                                        gestureOverlay = GestureOverlayState(GestureKind.HORIZONTAL_SEEK, "-10 sec • ${formatVideoMs(target)}", Icons.Filled.Replay10)
                                        controlsVisible = false
                                    }
                                    offset.x > w * 0.65f -> {
                                        val target = (latestState.positionMs + 10_000L).coerceAtMost(duration)
                                        viewModel.videoSeekTo(target)
                                        gestureOverlay = GestureOverlayState(GestureKind.HORIZONTAL_SEEK, "+10 sec • ${formatVideoMs(target)}", Icons.Filled.Forward10)
                                        controlsVisible = false
                                    }
                                    else -> {
                                        viewModel.videoPlayPause()
                                        controlsVisible = false
                                    }
                                }
                                interactionTick++
                                scope.launch {
                                    delay(850)
                                    gestureOverlay = GestureOverlayState()
                                }
                            },
                            onTap = {
                                if (controlsLocked) {
                                    controlsVisible = false
                                    showUnlockPrompt = true
                                } else {
                                    controlsVisible = !controlsVisible
                                    interactionTick++
                                }
                            }
                        )
                    }
            )
        } else {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }

        if (gestureOverlay.kind != GestureKind.NONE && !controlsLocked) {
            LaunchedEffect(gestureOverlay.kind, gestureOverlay.value, gestureOverlay.positionMs) {
                delay(650)
                gestureOverlay = GestureOverlayState()
            }
            GestureFeedback(gestureOverlay)
        }

        if (draggingSeek && previewBitmap != null) {
            SeekFramePreview(previewBitmap!!, previewPosition, Modifier.align(Alignment.BottomCenter).padding(bottom = 92.dp))
        }

        if (controlsLocked && showUnlockPrompt) {
            Card(
                Modifier.align(Alignment.Center),
                colors = CardDefaults.cardColors(containerColor = Color(0xDD15171C))
            ) {
                TextButton(
                    onClick = { toggleLock() },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.LockOpen, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Unlock controls", color = Color.White)
                }
            }
        } else if (controlsVisible && !controlsLocked) {
            VideoControls(
                title = selectedVideo?.title ?: "Video",
                state = state,
                fullscreen = fullscreen,
                showSettings = showSettings,
                onBack = { if (fullscreen) exitFullscreen() else onBack() },
                onMore = { showSettings = !showSettings; interactionTick++ },
                onSeek = { position ->
                    viewModel.videoSeekTo(position)
                },
                onSeekFinished = {
                    draggingSeek = false
                    previewJob?.cancel()
                    previewBitmap = null
                    showControls()
                },
                onSeekPreview = { position ->
                    previewPosition = position
                    draggingSeek = true
                    previewJob?.cancel()
                    previewJob = scope.launch {
                        delay(60)
                        val uri = selectedVideo?.uriString ?: return@launch
                        previewBitmap = extractVideoFrame(context, uri, position)
                    }
                },
                onPrevious = { viewModel.videoPrevious(); showControls() },
                onPlayPause = { viewModel.videoPlayPause(); showControls() },
                onNext = { viewModel.videoNext(); showControls() },
                onShuffle = { viewModel.videoToggleShuffle(); showControls() },
                onRepeat = { viewModel.videoCycleRepeatMode(); showControls() },
                onVolume = {
                    viewModel.videoSetVolume(if (state.volume > 0f) 0f else 1f)
                    showControls()
                },
                onSettings = { showSettings = !showSettings; interactionTick++ },
                onLock = ::toggleLock,
                onFullscreen = { if (fullscreen) exitFullscreen() else enterFullscreen() },
                displayMode = displayMode,
                onDisplayMode = { displayMode = it; showSettings = false; showControls() },
                subtitles = state.subtitleTracks,
                subtitlesEnabled = state.subtitlesEnabled,
                selectedSubtitleKey = state.selectedSubtitleKey,
                onSubtitlesEnabled = { viewModel.setSubtitlesEnabled(it); interactionTick++ },
                onSelectSubtitle = { viewModel.selectSubtitle(it); showSettings = false; showControls() }
            )
        }

        if (state.errorMessage != null) {
            Card(
                Modifier.align(Alignment.Center).padding(24.dp),
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
private fun androidx.compose.foundation.layout.BoxScope.GestureFeedback(state: GestureOverlayState) {
    val icon = state.icon ?: Icons.Filled.Settings
    Card(
        Modifier
            .align(Alignment.TopCenter)
            .padding(top = 22.dp, start = 28.dp, end = 28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xDD101216))
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Text(state.value, color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun SeekFramePreview(bitmap: Bitmap, position: Long, modifier: Modifier) {
    Card(modifier.padding(horizontal = 18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xEE111318))) {
        Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                bitmap.asImageBitmap(),
                contentDescription = "Seek preview",
                modifier = Modifier.size(width = 190.dp, height = 108.dp),
                contentScale = ContentScale.Crop
            )
            Text(formatVideoMs(position), color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
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
    onSeekFinished: () -> Unit,
    onSeekPreview: (Long) -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onVolume: () -> Unit,
    onSettings: () -> Unit,
    onLock: () -> Unit,
    onFullscreen: () -> Unit,
    displayMode: DisplayMode,
    onDisplayMode: (DisplayMode) -> Unit,
    subtitles: List<SubtitleTrack>,
    subtitlesEnabled: Boolean,
    selectedSubtitleKey: String?,
    onSubtitlesEnabled: (Boolean) -> Unit,
    onSelectSubtitle: (SubtitleTrack?) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.58f)).padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White) }
                Text(title, color = Color.White, maxLines = 1, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onMore) { Icon(Icons.Filled.MoreVert, "More", tint = Color.White) }
            }

            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.BottomCenter) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onSeek((state.positionMs - 10_000L).coerceAtLeast(0L)) }) { Icon(Icons.Filled.Replay10, "Rewind 10 seconds", tint = Color.White, modifier = Modifier.size(32.dp)) }
                    IconButton(onClick = onPrevious) { Icon(Icons.Filled.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(38.dp)) }
                    FilledIconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.size(72.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFFF6A00))
                    ) { Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play/Pause", tint = Color.White, modifier = Modifier.size(40.dp)) }
                    IconButton(onClick = onNext) { Icon(Icons.Filled.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(38.dp)) }
                    IconButton(onClick = { onSeek((state.positionMs + 10_000L).coerceAtMost(state.durationMs.coerceAtLeast(0L))) }) { Icon(Icons.Filled.Forward10, "Forward 10 seconds", tint = Color.White, modifier = Modifier.size(32.dp)) }
                }
            }

            Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.72f)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                val max = state.durationMs.coerceAtLeast(1L).toFloat()
                Slider(
                    value = state.positionMs.toFloat().coerceIn(0f, max),
                    onValueChange = { value ->
                        val position = value.toLong()
                        onSeekPreview(position)
                        onSeek(position)
                    },
                    valueRange = 0f..max,
                    onValueChangeFinished = onSeekFinished,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFF6A00), activeTrackColor = Color(0xFFFF6A00))
                )
                Row(Modifier.fillMaxWidth()) {
                    Text(formatVideoMs(state.positionMs), color = Color.White, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.weight(1f))
                    Text(formatVideoMs(state.durationMs), color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onShuffle) { Icon(Icons.Filled.Shuffle, "Shuffle", tint = if (state.shuffleEnabled) Color(0xFFFF6A00) else Color.White) }
                    IconButton(onClick = onRepeat) { Icon(Icons.Filled.Repeat, "Repeat", tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) Color(0xFFFF6A00) else Color.White) }
                    IconButton(onClick = onVolume) { Icon(if (state.volume > 0f) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff, "Volume", tint = Color.White) }
                    IconButton(onClick = onSettings) { Icon(Icons.Filled.Subtitles, "Subtitles", tint = if (subtitlesEnabled) Color(0xFFFFB000) else Color.White) }
                    IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, "Video settings", tint = Color.White) }
                    IconButton(onClick = onLock) { Icon(Icons.Filled.Lock, "Lock controls", tint = Color.White) }
                    IconButton(onClick = onFullscreen) { Icon(if (fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen, "Fullscreen", tint = Color.White) }
                }
            }
        }

        if (showSettings) {
            VideoSettingsMenu(
                displayMode = displayMode,
                onDisplayMode = onDisplayMode,
                subtitles = subtitles,
                subtitlesEnabled = subtitlesEnabled,
                selectedSubtitleKey = selectedSubtitleKey,
                onSubtitlesEnabled = onSubtitlesEnabled,
                onSelectSubtitle = onSelectSubtitle,
                onClose = onSettings
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.VideoSettingsMenu(
    displayMode: DisplayMode,
    onDisplayMode: (DisplayMode) -> Unit,
    subtitles: List<SubtitleTrack>,
    subtitlesEnabled: Boolean,
    selectedSubtitleKey: String?,
    onSubtitlesEnabled: (Boolean) -> Unit,
    onSelectSubtitle: (SubtitleTrack?) -> Unit,
    onClose: () -> Unit
) {
    Card(
        Modifier.align(Alignment.TopEnd).padding(top = 58.dp, end = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF015171C))
    ) {
        Column(Modifier.padding(vertical = 6.dp)) {
            Text("Display", color = Color(0xFFFFB000), modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            DisplayMode.values().forEach { mode ->
                DropdownMenuItem(
                    text = { Text(if (mode == displayMode) "✓ ${mode.label}" else mode.label, color = Color.White) },
                    onClick = { onDisplayMode(mode) }
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.White.copy(alpha = 0.14f))
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Subtitles, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Subtitles", color = Color.White, modifier = Modifier.weight(1f))
                TextButton(onClick = { onSubtitlesEnabled(!subtitlesEnabled) }) {
                    Text(if (subtitlesEnabled) "ON" else "OFF", color = Color(0xFFFFB000))
                }
            }
            if (subtitles.isEmpty()) {
                Text("No subtitles available", color = Color.LightGray, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
            } else {
                DropdownMenuItem(
                    text = { Text(if (selectedSubtitleKey == null) "✓ Off" else "Off", color = Color.White) },
                    onClick = { onSelectSubtitle(null) }
                )
                subtitles.forEach { track ->
                    DropdownMenuItem(
                        text = { Text(if (track.key == selectedSubtitleKey) "✓ ${track.label}" else track.label, color = Color.White) },
                        onClick = { onSelectSubtitle(track) }
                    )
                }
            }
            DropdownMenuItem(text = { Text("Close", color = Color.White) }, onClick = onClose)
        }
    }
}

private suspend fun extractVideoFrame(context: Context, uriString: String, positionMs: Long): Bitmap? = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, Uri.parse(uriString))
        retriever.getFrameAtTime(positionMs.coerceAtLeast(0L) * 1000L, MediaMetadataRetriever.OPTION_CLOSEST)
    } catch (_: Exception) {
        null
    } finally {
        runCatching { retriever.release() }
    }
}

private fun formatVideoMs(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0L) / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}
