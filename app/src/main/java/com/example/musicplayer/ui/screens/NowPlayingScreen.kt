package com.example.musicplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.example.musicplayer.viewmodel.MusicViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onOpenEqualizer: () -> Unit
) {
    val state by viewModel.playbackState.collectAsState()
    val song = state.currentSong

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.pollPosition()
            delay(500)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Now Playing",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ExpandMore,
                            contentDescription = "Collapse"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenEqualizer) {
                        Icon(
                            Icons.Filled.Equalizer,
                            contentDescription = "Equalizer",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->

        if (song == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Nothing playing")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(14.dp))

            // ─────────────────────────────
            // ALBUM ART
            // ─────────────────────────────
            AsyncImage(
                model = viewModel.albumArtUri(song.albumId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(Modifier.height(22.dp))

            // ─────────────────────────────
            // TITLE + ARTIST
            // ─────────────────────────────
            Text(
                text = song.title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )

            Spacer(Modifier.height(14.dp))

            // ─────────────────────────────
            // PROGRESS BAR
            // ─────────────────────────────
            val duration = state.durationMs.coerceAtLeast(1L)

            Slider(
                value = state.positionMs
                    .toFloat()
                    .coerceIn(0f, duration.toFloat()),
                onValueChange = {
                    viewModel.seekTo(it.toLong())
                },
                valueRange = 0f..duration.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formatMs(state.positionMs),
                    style = MaterialTheme.typography.labelSmall
                )

                Text(
                    formatMs(state.durationMs),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(Modifier.height(10.dp))

            // ─────────────────────────────
            // MAIN CONTROLS
            // Shuffle | Previous | Play | Next | Repeat
            // ─────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = { viewModel.toggleShuffle() }
                ) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        modifier = Modifier.size(25.dp),
                        tint =
                            if (state.shuffleEnabled)
                                MaterialTheme.colorScheme.primary
                            else
                                LocalContentColor.current
                    )
                }

                IconButton(
                    onClick = { viewModel.previous() }
                ) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(38.dp)
                    )
                }

                FilledIconButton(
                    onClick = { viewModel.playPause() },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        if (state.isPlaying)
                            Icons.Filled.Pause
                        else
                            Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(34.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.next() }
                ) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(38.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.cycleRepeatMode() }
                ) {
                    Icon(
                        when (state.repeatMode) {
                            Player.REPEAT_MODE_ONE ->
                                Icons.Filled.RepeatOne

                            else ->
                                Icons.Filled.Repeat
                        },
                        contentDescription = "Repeat",
                        modifier = Modifier.size(25.dp),
                        tint =
                            if (state.repeatMode != Player.REPEAT_MODE_OFF)
                                MaterialTheme.colorScheme.primary
                            else
                                LocalContentColor.current
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ─────────────────────────────
            // LYRICS
            // ─────────────────────────────
            val lyrics = remember(song.path) {
                com.example.musicplayer.util.LyricsLoader
                    .loadPlainLyrics(song.path)
            }

            if (lyrics != null) {

                var expanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    onClick = {
                        expanded = !expanded
                    }
                ) {

                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Icon(
                                Icons.Filled.MusicNote,
                                contentDescription = null
                            )

                            Spacer(Modifier.width(8.dp))

                            Text(
                                "Lyrics",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(Modifier.weight(1f))

                            Icon(
                                if (expanded)
                                    Icons.Filled.ExpandLess
                                else
                                    Icons.Filled.ExpandMore,
                                contentDescription = "Lyrics"
                            )
                        }

                        if (expanded) {

                            Spacer(Modifier.height(10.dp))

                            Text(
                                lyrics,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "%d:%02d".format(minutes, seconds)
}
