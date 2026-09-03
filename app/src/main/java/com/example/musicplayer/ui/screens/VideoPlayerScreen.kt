package com.example.musicplayer.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ClosedCaption
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
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.musicplayer.playback.SubtitleOption
import com.example.musicplayer.viewmodel.MusicViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private enum class VideoScreenMode(val label: String) {
    FIT("Fit"),
    FILL("Fill"),
    CROP("Crop"),
    ORIGINAL("Original"),
    RATIO_16_9("16:9"),
    RATIO_4_3("4:3"),
    RATIO_1_1("1:1")
}

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
    val subtitleOptions by viewModel.videoSubtitleOptions().collectAsState()
    val selectedVideo = videos.firstOrNull { it.id == videoId } ?: state.currentVideo

    var controlsVisible by remember { mutableStateOf(true) }
    var fullscreen by remember { mutableStateOf(true) }
    var controlsLocked by remember { mutableStateOf(false) }
    var showUnlockPrompt by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var gestureFeedback by remember { mutableStateOf<String?>(null) }
    var gestureFeedbackTick by remember { mutableIntStateOf(0) }
    var previewPosition by remember { mutableLongStateOf(-1L) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var screenMode by remember { mutableStateOf(VideoScreenMode.FIT) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var zoomOffsetX by remember { mutableFloatStateOf(0f) }
    var zoomOffsetY by remember { mutableFloatStateOf(0f) }
    var brightness by remember { mutableFloatStateOf(1f) }
    var selectedSubtitle by remember(videoId) { mutableStateOf<SubtitleOption?>(null) }
    val originalBrightness = remember {
        activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 1f
    }

    fun touch() {
        controlsVisible = true
        interactionTick++
    }

    fun showGesture(message: String) {
        gestureFeedback = message
        gestureFeedbackTick++
        touch()
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
            activity?.window?.let { window ->
                window.attributes = window.attributes.apply {
                    screenBrightness = originalBrightness
                }
            }
        }
    }

    LaunchedEffect(videoId, videos.size) {
        videos.firstOrNull { it.id == videoId }?.let(viewModel::playVideo)
    }

    LaunchedEffect(state.currentVideo?.id) {
        selectedSubtitle = null
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

    LaunchedEffect(gestureFeedbackTick) {
        if (gestureFeedback != null) {
            delay(900)
            gestureFeedback = null
        }
    }

    LaunchedEffect(previewPosition) {
        val video = selectedVideo ?: return@LaunchedEffect
        if (previewPosition < 0L) return@LaunchedEffect
        delay(90)
        previewBitmap = withContext(Dispatchers.IO) {
            runCatching {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, Uri.parse(video.uriString))
                    retriever.getFrameAtTime(
                        previewPosition.coerceAtLeast(0L) * 1000L,
                        MediaMetadataRetriever.OPTION_CLOSEST
                    )
                } finally {
                    retriever.release()
                }
            }.getOrNull()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.pollVideoPosition()
            delay(400)
        }
    }

    BackHandler {
        if (fullscreen) exitFullscreen() else onBack()
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
                update = {
                    it.player = player
                    it.resizeMode = when (screenMode) {
                        VideoScreenMode.FIT, VideoScreenMode.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        VideoScreenMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        VideoScreenMode.CROP,
                        VideoScreenMode.RATIO_16_9,
                        VideoScreenMode.RATIO_4_3,
                        VideoScreenMode.RATIO_1_1 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        when (screenMode) {
                            VideoScreenMode.RATIO_16_9 -> Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .align(Alignment.Center)
                            VideoScreenMode.RATIO_4_3 -> Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f / 3f)
                                .align(Alignment.Center)
                            VideoScreenMode.RATIO_1_1 -> Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .align(Alignment.Center)
                            else -> Modifier
                        }
                    )
                    .graphicsLayer {
                        scaleX = zoomScale
                        scaleY = zoomScale
                        translationX = zoomOffsetX
                        translationY = zoomOffsetY
                    }
            )

            // Whole video tap surface: single tap toggles controls, double tap seeks.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(controlsLocked, state.positionMs, state.durationMs) {
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                if (controlsLocked) {
                                    showUnlockPrompt = true
                                    return@detectTapGestures
                                }
                                val width = size.width.toFloat().coerceAtLeast(1f)
                                val delta = when {
                                    offset.x < width * 0.35f -> -10_000L
                                    offset.x > width * 0.65f -> 10_000L
                                    else -> 0L
                                }
                                if (delta != 0L) {
                                    val target = (state.positionMs + delta)
                                        .coerceIn(0L, state.durationMs.coerceAtLeast(0L))
                                    viewModel.videoSeekTo(target)
                                    showGesture(if (delta < 0) "◀◀  -10s" else "+10s  ▶▶")
                                } else {
                                    viewModel.videoPlayPause()
                                    showGesture(if (state.isPlaying) "❚❚" else "▶")
                                }
                            },
                            onTap = {
                                if (controlsLocked) {
                                    showUnlockPrompt = true
                                } else if (controlsVisible) {
                                    controlsVisible = false
                                } else {
                                    touch()
                                }
                            }
                        )
                    }
            )

            // Gesture zones sit above the tap surface. A vertical drag adjusts only
            // brightness (left) or volume (right); a simple tap still falls through
            // to the normal tap handler because the drag detector only consumes drags.
            if (!controlsLocked) {
                Row(Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .pointerInput(brightness) {
                                detectVerticalDragGestures(
                                    onVerticalDrag = { _, dragAmount ->
                                        brightness = (brightness - dragAmount / 700f).coerceIn(0.05f, 1f)
                                        activity?.window?.let { window ->
                                            window.attributes = window.attributes.apply {
                                                screenBrightness = brightness
                                            }
                                        }
                                        showGesture("☀ ${((brightness * 100).toInt())}%")
                                    }
                                )
                            }
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .pointerInput(state.volume) {
                                detectVerticalDragGestures(
                                    onVerticalDrag = { _, dragAmount ->
                                        val next = (state.volume - dragAmount / 500f).coerceIn(0f, 1f)
                                        viewModel.videoSetVolume(next)
                                        showGesture("🔊 ${(next * 100).toInt()}%")
                                    }
                                )
                            }
                    )
                }
            }

        } else {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        if (gestureFeedback != null) {
            Card(
                modifier = Modifier.align(Alignment.Center),
                colors = CardDefaults.cardColors(containerColor = Color(0xCC111318))
            ) {
                Text(
                    gestureFeedback.orEmpty(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)
                )
            }
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
        } else if (controlsVisible && !controlsLocked) {
            VideoControls(
                title = selectedVideo?.title ?: "Video",
                uriString = selectedVideo?.uriString,
                state = state,
                fullscreen = fullscreen,
                screenMode = screenMode,
                zoomScale = zoomScale,
                showSettings = showSettings,
                subtitleOptions = subtitleOptions,
                selectedSubtitle = selectedSubtitle,
                previewBitmap = previewBitmap,
                onPreviewPosition = {
                    previewPosition = it
                    if (it < 0L) previewBitmap = null
                },
                onBack = { if (fullscreen) exitFullscreen() else onBack() },
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
                onSubtitle = { showSettings = true; touch() },
                onSelectSubtitle = { option ->
                    selectedSubtitle = option
                    viewModel.videoSelectSubtitle(option)
                    showSettings = true
                    touch()
                },
                onLock = ::toggleControlsLock,
                onFullscreen = { if (fullscreen) exitFullscreen() else enterFullscreen() },
                onMode = { mode ->
                    screenMode = mode
                    if (mode != VideoScreenMode.CROP) {
                        zoomScale = 1f
                        zoomOffsetX = 0f
                        zoomOffsetY = 0f
                    }
                    showSettings = false
                    touch()
                },
                onZoomChange = { scale, dx, dy ->
                    zoomScale = scale.coerceIn(1f, 3.5f)
                    zoomOffsetX += dx
                    zoomOffsetY += dy
                    touch()
                }
            )
        }

        if (state.errorMessage != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
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
    uriString: String?,
    state: com.example.musicplayer.playback.VideoPlaybackUiState,
    fullscreen: Boolean,
    screenMode: VideoScreenMode,
    zoomScale: Float,
    showSettings: Boolean,
    subtitleOptions: List<SubtitleOption>,
    selectedSubtitle: SubtitleOption?,
    previewBitmap: Bitmap?,
    onPreviewPosition: (Long) -> Unit,
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
    onSubtitle: () -> Unit,
    onSelectSubtitle: (SubtitleOption?) -> Unit,
    onLock: () -> Unit,
    onFullscreen: () -> Unit,
    onMode: (VideoScreenMode) -> Unit,
    onZoomChange: (Float, Float, Float) -> Unit
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
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
                    IconButton(onClick = { onSeek((state.positionMs + 10_000L).coerceAtMost(state.durationMs)) }) {
                        Icon(Icons.Filled.Forward10, "Forward 10 seconds", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                var sliderValue by remember(state.positionMs) { mutableFloatStateOf(state.positionMs.toFloat()) }
                val max = state.durationMs.coerceAtLeast(1L).toFloat()
                Slider(
                    value = sliderValue.coerceIn(0f, max),
                    onValueChange = {
                        sliderValue = it
                        onPreviewPosition(it.toLong())
                    },
                    onValueChangeFinished = {
                        onSeek(sliderValue.toLong())
                        onPreviewPosition(-1L)
                    },
                    valueRange = 0f..max,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFF6A00),
                        activeTrackColor = Color(0xFFFF6A00)
                    )
                )
                if (previewBitmap != null && uriString != null) {
                    androidx.compose.foundation.Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Seek preview",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(width = 160.dp, height = 90.dp)
                    )
                }
                Row(Modifier.fillMaxWidth()) {
                    Text(formatVideoMs(sliderValue.toLong()), color = Color.White, style = MaterialTheme.typography.labelSmall)
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
                        Icon(if (state.volume > 0f) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff, "Volume", tint = Color.White)
                    }
                    IconButton(onClick = onSubtitle) {
                        Icon(
                            Icons.Filled.ClosedCaption,
                            "Subtitles",
                            tint = if (selectedSubtitle != null) Color(0xFFFF6A00) else Color.White
                        )
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Tune, "Video settings", tint = Color.White)
                    }
                    IconButton(onClick = onLock) {
                        Icon(Icons.Filled.Lock, "Lock controls", tint = Color.White)
                    }
                    IconButton(onClick = onFullscreen) {
                        Icon(if (fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen, "Fullscreen", tint = Color.White)
                    }
                }
            }
        }

        if (showSettings) {
            VideoSettingsMenu(
                currentMode = screenMode,
                zoomScale = zoomScale,
                onMode = onMode,
                onZoomChange = onZoomChange,
                subtitleOptions = subtitleOptions,
                selectedSubtitle = selectedSubtitle,
                onSelectSubtitle = onSelectSubtitle,
                onClose = onSettings
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.VideoSettingsMenu(
    currentMode: VideoScreenMode,
    zoomScale: Float,
    onMode: (VideoScreenMode) -> Unit,
    onZoomChange: (Float, Float, Float) -> Unit,
    subtitleOptions: List<SubtitleOption>,
    selectedSubtitle: SubtitleOption?,
    onSelectSubtitle: (SubtitleOption?) -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 56.dp, end = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xEE15171C))
    ) {
        Column(Modifier.padding(8.dp)) {
            Text("Screen mode", color = Color.White, modifier = Modifier.padding(8.dp))
            VideoScreenMode.values().forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Text(
                            if (currentMode == mode) "✓ ${mode.label}" else mode.label,
                            color = Color.White
                        )
                    },
                    onClick = { onMode(mode) }
                )
            }
            Text("Subtitles", color = Color.White, modifier = Modifier.padding(8.dp))
            DropdownMenuItem(
                text = { Text(if (selectedSubtitle == null) "✓ Off" else "Off", color = Color.White) },
                onClick = { onSelectSubtitle(null) }
            )
            if (subtitleOptions.isEmpty()) {
                Text(
                    "No subtitles available",
                    color = Color.LightGray,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            } else {
                subtitleOptions.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (selectedSubtitle == option) "✓ ${option.label}" else option.label,
                                color = Color.White
                            )
                        },
                        onClick = { onSelectSubtitle(option) }
                    )
                }
            }
            Text("Zoom", color = Color.White, modifier = Modifier.padding(8.dp))
            Slider(
                value = zoomScale,
                onValueChange = { onZoomChange(it, 0f, 0f) },
                valueRange = 1f..3.5f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFF6A00),
                    activeTrackColor = Color(0xFFFF6A00)
                )
            )
            DropdownMenuItem(
                text = { Text("Reset zoom", color = Color.White) },
                onClick = { onZoomChange(1f, 0f, 0f) }
            )
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
