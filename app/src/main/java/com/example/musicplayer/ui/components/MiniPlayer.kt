package com.example.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.musicplayer.viewmodel.MusicViewModel

@Composable
fun MiniPlayer(
    viewModel: MusicViewModel,
    onExpand: () -> Unit,
    visible: Boolean = true
) {
    if (!visible) return

    val state by viewModel.playbackState.collectAsState()
    val song = state.currentSong ?: return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onExpand()
            },
        tonalElevation = 3.dp
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(
                    horizontal = 10.dp,
                    vertical = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ─────────────────────────────
            // ALBUM ART
            // ─────────────────────────────

            AsyncImage(
                model = viewModel.albumArtUri(song.albumId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(
                        RoundedCornerShape(8.dp)
                    )
                    .background(
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                    )
            )

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            // ─────────────────────────────
            // SONG INFO
            // ─────────────────────────────

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = song.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = song.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // ─────────────────────────────
            // PREVIOUS
            // ─────────────────────────────

            IconButton(
                onClick = {
                    viewModel.previous()
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous",
                    modifier = Modifier.size(26.dp)
                )
            }

            // ─────────────────────────────
            // PLAY / PAUSE
            // ─────────────────────────────

            IconButton(
                onClick = {
                    viewModel.playPause()
                }
            ) {
                Icon(
                    imageVector =
                        if (state.isPlaying)
                            Icons.Filled.Pause
                        else
                            Icons.Filled.PlayArrow,
                    contentDescription = "Play/Pause",
                    modifier = Modifier.size(30.dp)
                )
            }

            // ─────────────────────────────
            // NEXT
            // ─────────────────────────────

            IconButton(
                onClick = {
                    viewModel.next()
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
