package com.example.musicplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.sp
import com.example.musicplayer.playback.EqualizerController
import com.example.musicplayer.playback.PlaybackService
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(onBack: () -> Unit) {
    val equalizer = remember { EqualizerController() }
    var enabled by remember { mutableStateOf(false) }
    var attached by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val sessionId = PlaybackService.instance?.player?.audioSessionId ?: 0
        if (sessionId != 0) {
            runCatching {
                equalizer.attach(sessionId)
                enabled = equalizer.isEnabled()
                attached = true
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { equalizer.release() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Equalizer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (!attached) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Play a song first")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            EqualizerGraph(equalizer)

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Equalizer", fontSize = 20.sp, modifier = Modifier.weight(1f))
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        equalizer.setEnabled(it)
                    }
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Presets",
                fontSize = 22.sp,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))

            equalizer.presetNames().forEachIndexed { index, preset ->
                TextButton(
                    onClick = { equalizer.applyPreset(index.toShort()) },
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    Text(
                        preset,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun EqualizerGraph(equalizer: EqualizerController) {
    val range = equalizer.bandLevelRange
    val minLevel = range[0].toFloat()
    val maxLevel = range[1].toFloat()
    val bands = equalizer.numberOfBands.toInt().coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(330.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            repeat(bands) { band ->
                EqualizerBand(
                    equalizer = equalizer,
                    band = band,
                    minLevel = minLevel,
                    maxLevel = maxLevel,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatDb(minLevel), fontSize = 11.sp)
            Text(formatDb(0f.coerceIn(minLevel, maxLevel)), fontSize = 11.sp)
            Text(formatDb(maxLevel), fontSize = 11.sp)
        }
    }
}

@Composable
private fun EqualizerBand(
    equalizer: EqualizerController,
    band: Int,
    minLevel: Float,
    maxLevel: Float,
    modifier: Modifier = Modifier
) {
    val bandShort = band.toShort()
    var level by remember(band) {
        mutableFloatStateOf(
            equalizer.getBandLevel(bandShort).toFloat().coerceIn(minLevel, maxLevel)
        )
    }

    val trackHeight = 230.dp
    val density = androidx.compose.ui.platform.LocalDensity.current
    val trackPx = with(density) { trackHeight.toPx() }

    val draggableState = rememberDraggableState { delta ->
        val next = (level - delta / trackPx * (maxLevel - minLevel))
            .coerceIn(minLevel, maxLevel)
        level = next
        equalizer.setBandLevel(bandShort, next.roundToInt().toShort())
    }

    val fraction = if (maxLevel > minLevel) {
        ((level - minLevel) / (maxLevel - minLevel)).coerceIn(0f, 1f)
    } else 0.5f

    Column(
        modifier = modifier.height(330.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(formatFrequency(equalizer.centerFrequency(bandShort)), fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .height(trackHeight)
                .fillMaxWidth()
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Vertical
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outline)
            )

            val thumbOffset = with(density) {
                ((1f - fraction) * (trackPx - 24.dp.toPx())).toDp()
            }

            Box(
                modifier = Modifier
                    .offset(y = thumbOffset)
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(formatDb(level), fontSize = 11.sp, maxLines = 1)
    }
}

private fun formatFrequency(hz: Int): String = when {
    hz >= 1000 && hz % 1000 == 0 -> "${hz / 1000}KHz"
    hz >= 1000 -> String.format("%.1fKHz", hz / 1000f)
    else -> "${hz}Hz"
}

private fun formatDb(millibels: Float): String = String.format("%.1f dB", millibels / 100f)
