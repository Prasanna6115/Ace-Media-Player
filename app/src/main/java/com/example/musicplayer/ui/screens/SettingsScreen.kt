package com.example.musicplayer.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
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
            Text(
                if (darkTheme) "Dark theme is active" else "Light theme is active",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (darkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                    contentDescription = null
                )
                Spacer(Modifier.weight(1f))
                Text("Dark mode", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = darkTheme,
                    onCheckedChange = viewModel::setDarkTheme
                )
            }

            Spacer(Modifier.height(6.dp))
            Text(
                "Turn this switch off for the bright theme. Your choice is saved for the next launch.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(28.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))

            Text("Sleep timer", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            if (timerActive) {
                Text(remainingLabel, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    PlaybackService.instance?.sleepTimerJob?.cancel()
                    timerActive = false
                }) {
                    Text("Cancel timer")
                }
            } else {
                Text("Stop playback after ${sleepMinutes.toInt()} minutes")
                Slider(
                    value = sleepMinutes,
                    onValueChange = { sleepMinutes = it },
                    valueRange = 5f..120f,
                    steps = 22
                )
                TextButton(onClick = {
                    PlaybackService.instance?.sleepTimerJob?.start(sleepMinutes.toInt())
                    timerActive = true
                }) {
                    Text("Start sleep timer")
                }
            }

            Spacer(Modifier.height(28.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))
            Text("ACE Media Player", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("Version 1.0", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

