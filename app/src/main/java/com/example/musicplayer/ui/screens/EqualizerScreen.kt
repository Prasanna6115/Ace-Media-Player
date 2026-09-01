package com.example.musicplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.playback.EqualizerController
import com.example.musicplayer.playback.PlaybackService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    onBack: () -> Unit
) {
    val equalizer = remember {
        EqualizerController()
    }

    var enabled by remember {
        mutableStateOf(false)
    }

    var attached by remember {
        mutableStateOf(false)
    }

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
        onDispose {
            equalizer.release()
        }
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
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
                Text(
                    text = "Play a song first",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

        } else {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {

                // =========================================================
                // EQUALIZER GRAPH
                // =========================================================

                EqualizerGraph(
                    equalizer = equalizer
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                // =========================================================
                // ENABLE EQUALIZER
                // =========================================================

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .padding(
                            horizontal = 20.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "Equalizer",
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f)
                    )

                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            enabled = it
                            equalizer.setEnabled(it)
                        }
                    )
                }

                // =========================================================
                // PRESETS
                // =========================================================

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "Presets",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        horizontal = 20.dp
                    )
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(
                        horizontal = 20.dp
                    ),
                    thickness = 1.dp
                )

                val presets = equalizer.presetNames()

                presets.forEachIndexed { index, preset ->

                    TextButton(
                        onClick = {
                            equalizer.applyPreset(
                                index.toShort()
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .padding(
                                horizontal = 8.dp
                            ),
                        contentPadding = PaddingValues(
                            horizontal = 12.dp
                        )
                    ) {

                        Text(
                            text = preset,
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )
            }
        }
    }
}


// ========================================================================
// EQUALIZER GRAPH
// ========================================================================

@Composable
private fun EqualizerGraph(
    equalizer: EqualizerController
) {

    val range = equalizer.bandLevelRange

    val minLevel = range[0].toFloat()
    val maxLevel = range[1].toFloat()

    val bands = equalizer.numberOfBands

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp)
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(285.dp)
                .horizontalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 28.dp
                )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(
                        min = 330.dp
                    )
                    .fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceEvenly,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                for (band in 0 until bands) {

                    EqualizerBand(
                        equalizer = equalizer,
                        band = band,
                        minLevel = minLevel,
                        maxLevel = maxLevel
                    )
                }
            }
        }

        // ================================================================
        // RANGE
        // ================================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 32.dp
                ),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Text(
                text = "-12 dB",
                fontSize = 10.sp
            )

            Text(
                text = "0 dB",
                fontSize = 10.sp
            )

            Text(
                text = "+12 dB",
                fontSize = 10.sp
            )
        }
    }
}


// ========================================================================
// SINGLE BAND
// ========================================================================

@Composable
private fun EqualizerBand(
    equalizer: EqualizerController,
    band: Int,
    minLevel: Float,
    maxLevel: Float
) {

    val bandShort = band.toShort()

    var level by remember {
        mutableStateOf(
            equalizer
                .getBandLevel(bandShort)
                .toFloat()
        )
    }

    Column(
        modifier = Modifier
            .width(48.dp)
            .height(270.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        // ================================================================
        // FREQUENCY
        // ================================================================

        Text(
            text = formatFrequency(
                equalizer.centerFrequency(
                    bandShort
                )
            ),
            fontSize = 11.sp,
            maxLines = 1
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        // ================================================================
        // SLIDER
        // ================================================================

        Box(
            modifier = Modifier
                .width(48.dp)
                .height(205.dp),
            contentAlignment = Alignment.Center
        ) {

            // Background line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(180.dp)
                    .background(
                        MaterialTheme
                            .colorScheme
                            .outline
                    )
            )

            Slider(
                value = level,

                onValueChange = {
                    level = it

                    equalizer.setBandLevel(
                        bandShort,
                        it.toInt().toShort()
                    )
                },

                valueRange =
                    minLevel..maxLevel,

                modifier = Modifier
                    .width(205.dp)
                    .height(38.dp)
                    .rotate(270f)
            )
        }

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        // ================================================================
        // DB VALUE
        // ================================================================

        Text(
            text = String.format(
                "%.1f dB",
                level / 100f
            ),
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}


// ========================================================================
// FREQUENCY FORMAT
// ========================================================================

private fun formatFrequency(
    frequency: Int
): String {

    return when {

        frequency >= 1000 -> {
            val khz = frequency / 1000f

            if (khz % 1f == 0f) {
                "${khz.toInt()}KHz"
            } else {
                String.format(
                    "%.1fKHz",
                    khz
                )
            }
        }

        else -> {
            "${frequency}Hz"
        }
    }
}
