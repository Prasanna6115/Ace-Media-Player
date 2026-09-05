package com.example.musicplayer.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.musicplayer.playback.PlaybackService
import com.example.musicplayer.viewmodel.MusicViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MusicViewModel, onBack: () -> Unit) {
    var sleepMinutes by remember { mutableStateOf(15f) }
    var timerActive by remember { mutableStateOf(false) }
    var remainingLabel by remember { mutableStateOf("") }

    val darkTheme by viewModel.darkTheme.collectAsState()
    val autoPlayNext by viewModel.autoPlayNext.collectAsState()
    val resumePlayback by viewModel.resumePlayback.collectAsState()
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()
    val doubleTap by viewModel.gestureDoubleTap.collectAsState()
    val seekGesture by viewModel.gestureSeek.collectAsState()
    val volumeGesture by viewModel.gestureVolume.collectAsState()
    val brightnessGesture by viewModel.gestureBrightness.collectAsState()
    val longPress by viewModel.gestureLongPress.collectAsState()
    val zoomGesture by viewModel.gestureZoom.collectAsState()

    LaunchedEffect(timerActive) {
        while (timerActive) {
            val timer = PlaybackService.instance?.sleepTimerJob
            if (timer == null || !timer.isActive) {
                timerActive = false
            } else {
                val totalSeconds = timer.remainingMillis / 1000
                remainingLabel = "%d:%02d remaining".format(totalSeconds / 60, totalSeconds % 60)
            }
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Appearance", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            SettingSwitchRow(
                title = "Dark mode",
                subtitle = if (darkTheme) "Dark theme is active" else "Bright theme is active",
                icon = if (darkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                checked = darkTheme,
                onCheckedChange = viewModel::setDarkTheme
            )

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))

            Text("Video Playback", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            SettingSwitchRow(
                "Auto play next",
                "Play the next video automatically when the current one ends",
                Icons.Filled.PlayArrow,
                autoPlayNext,
                viewModel::setAutoPlayNext
            )
            SettingSwitchRow(
                "Resume playback",
                "Remember the last position of each video",
                Icons.Filled.Settings,
                resumePlayback,
                viewModel::setResumePlayback
            )
            SettingSwitchRow(
                "Keep screen on",
                "Prevent screen timeout while watching video",
                Icons.Filled.Settings,
                keepScreenOn,
                viewModel::setKeepScreenOn
            )

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))

            Text("Video Gestures", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            SettingSwitchRow(
                "Double-tap seek",
                "Double-tap left/right to rewind or forward 10 seconds",
                Icons.Filled.TouchApp,
                doubleTap,
                viewModel::setGestureDoubleTap
            )
            SettingSwitchRow(
                "Horizontal seek",
                "Swipe horizontally on the video to seek",
                Icons.Filled.TouchApp,
                seekGesture,
                viewModel::setGestureSeek
            )
            SettingSwitchRow(
                "Volume swipe",
                "Swipe vertically on the right side for volume",
                Icons.Filled.TouchApp,
                volumeGesture,
                viewModel::setGestureVolume
            )
            SettingSwitchRow(
                "Brightness swipe",
                "Swipe vertically on the left side for brightness",
                Icons.Filled.TouchApp,
                brightnessGesture,
                viewModel::setGestureBrightness
            )
            SettingSwitchRow(
                "Hold for 2×",
                "Hold the right side of the video for temporary 2× speed",
                Icons.Filled.TouchApp,
                longPress,
                viewModel::setGestureLongPress
            )
            SettingSwitchRow(
                "Pinch zoom",
                "Use two fingers to zoom and pan the video",
                Icons.Filled.TouchApp,
                zoomGesture,
                viewModel::setGestureZoom
            )

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))

            Text("Sleep Timer", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            if (timerActive) {
                Text(remainingLabel, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    PlaybackService.instance?.sleepTimerJob?.cancel()
                    timerActive = false
                }) { Text("Cancel timer") }
            } else {
                Text("Stop audio playback after ${sleepMinutes.toInt()} minutes")
                Slider(
                    value = sleepMinutes,
                    onValueChange = { sleepMinutes = it },
                    valueRange = 5f..120f,
                    steps = 22
                )
                TextButton(onClick = {
                    PlaybackService.instance?.sleepTimerJob?.start(sleepMinutes.toInt())
                    timerActive = true
                }) { Text("Start sleep timer") }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))
            Text("ACE Media Player", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("Version 2.1", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
