package com.example.musicplayer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.musicplayer.playback.PlaybackService
import com.example.musicplayer.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MusicViewModel, onBack: () -> Unit) {
    var sleepMinutes by remember { mutableStateOf(15f) }
    var timerActive by remember { mutableStateOf(false) }
    var remainingLabel by remember { mutableStateOf("") }

    LaunchedEffect(timerActive) {
        while (timerActive) {
            val timer = PlaybackService.instance?.sleepTimerJob
            if (timer == null || !timer.isActive) {
                timerActive = false
            } else {
                val totalSeconds = timer.remainingMillis / 1000
                remainingLabel = "%d:%02d remaining".format(totalSeconds / 60, totalSeconds % 60)
            }
            kotlinx.coroutines.delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Sleep timer", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (timerActive) {
                Text(remainingLabel)
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    PlaybackService.instance?.sleepTimerJob?.cancel()
                    timerActive = false
                }) { Text("Cancel timer") }
            } else {
                Text("Stop playback after ${sleepMinutes.toInt()} minutes")
                Slider(
                    value = sleepMinutes,
                    onValueChange = { sleepMinutes = it },
                    valueRange = 5f..120f,
                    steps = 22
                )
                Button(onClick = {
                    PlaybackService.instance?.sleepTimerJob?.start(sleepMinutes.toInt())
                    timerActive = true
                }) { Text("Start sleep timer") }
            }

            Spacer(Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Dark mode follows your system theme automatically. " +
                    "Switch it in your device's display settings to change it here too.",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text("Android Auto support is planned for a future update.", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
