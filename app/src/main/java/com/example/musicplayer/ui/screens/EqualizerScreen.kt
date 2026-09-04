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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.playback.EqualizerController
import com.example.musicplayer.playback.PlaybackService
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    onBack: () -> Unit
) {
    val equalizer = remember { EqualizerController() }
    var enabled by remember { mutableStateOf(false) }
    var attached by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val player = PlaybackService.instance?.player
        val sessionId = player?.audioSessionId
        if (sessionId != null && sessionId != 0) {
            equalizer.attach(sessionId)
            enabled = equalizer.isEnabled()
            attached = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { equalizer.release() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Equalizer",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                modifier = Modifier.height(58.dp)
            )
        }
    ) { paddingValues ->
        if (!attached) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Play a song first")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                EqualizerGraph(equalizer)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Equalizer", fontSize = 18.sp, modifier = Modifier.weight(1f))
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            enabled = it
                            equalizer.setEnabled(it)
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    "Presets",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))

                equalizer.presetNames().forEachIndexed { index, preset ->
                    TextButton(
                        onClick = { equalizer.applyPreset(index.toShort()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .padding(horizontal = 8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text(
                            text = preset,
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
}

@Composable
private fun EqualizerGraph(equalizer: EqualizerController) {
    val bands = equalizer.numberOfBands.toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(315.dp)
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            for (band in 0 until bands) {
                EqualizerBand(equalizer = equalizer, band = band)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("-12 dB", fontSize = 10.sp)
            Text("0 dB", fontSize = 10.sp)
            Text("+12 dB", fontSize = 10.sp)
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun EqualizerBand(
    equalizer: EqualizerController,
    band: Int
) {
    val bandShort = band.toShort()

    // The platform Equalizer exposes levels in millibels. We deliberately expose
    // a normalized -12..+12 dB UI so the slider always has the full visible range.
    val actualRange = equalizer.bandLevelRange
    val actualMin = actualRange[0].toFloat()
    val actualMax = actualRange[1].toFloat()

    var dbLevel by remember(band, actualMin, actualMax) {
        mutableFloatStateOf(
            (equalizer.getBandLevel(bandShort).toFloat() / 100f)
                .coerceIn(-12f, 12f)
        )
    }

    val sliderHeight = 210.dp
    val sliderHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) {
        sliderHeight.toPx()
    }

    Column(
        modifier = Modifier.width(54.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = formatFrequency(equalizer.centerFrequency(bandShort)),
            fontSize = 11.sp,
            maxLines = 1
        )

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .width(54.dp)
                .height(235.dp),
            contentAlignment = Alignment.Center
        ) {
            // Full-height track with a clear center (0 dB) reference.
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(sliderHeight)
                    .background(MaterialTheme.colorScheme.outline)
            )

            Box(
                modifier = Modifier
                    .width(54.dp)
                    .height(sliderHeight)
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            // Drag upward raises dB, downward lowers dB.
                            val pixelsPerDb = sliderHeightPx / 24f
                            dbLevel = (dbLevel - delta / pixelsPerDb)
                                .coerceIn(-12f, 12f)

                            val actual = (dbLevel / 12f)
                                .let { normalized ->
                                    if (normalized >= 0f) {
                                        normalized * actualMax
                                    } else {
                                        normalized * -actualMin
                                    }
                                }
                                .roundToInt()
                                .toShort()

                            equalizer.setBandLevel(bandShort, actual)
                        }
                    )
            )

            // Thumb position mapped across the complete -12..+12 track.
            val normalized = ((dbLevel + 12f) / 24f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        y = with(androidx.compose.ui.platform.LocalDensity.current) {
                            ((0.5f - normalized) * sliderHeightPx).toDp()
                        }
                    )
                    .size(28.dp)
                    .background(Color(0xFF00E5FF), shape = MaterialTheme.shapes.extraLarge)
            )
        }

        Text(
            text = String.format("%.1f dB", dbLevel),
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

private fun formatFrequency(frequency: Int): String {
    return when {
        frequency >= 1000 -> {
            val khz = frequency / 1000f
            if (khz % 1f == 0f) "${khz.toInt()}KHz"
            else String.format("%.1fKHz", khz)
        }
        else -> "${frequency}Hz"
    }
}
