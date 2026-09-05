package com.example.musicplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.musicplayer.ui.components.SongRow
import com.example.musicplayer.ui.components.tvFocusable
import com.example.musicplayer.viewmodel.MusicViewModel

/** TV-first library: landscape layout, large touch targets and D-pad focus. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onOpenEqualizer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenVideo: (Long) -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.scanLibrary()
        viewModel.scanVideos()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ACE Media Player", style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    IconButton(onClick = onOpenFavorites, modifier = Modifier.tvFocusable()) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Favorites")
                    }
                    IconButton(onClick = onOpenEqualizer, modifier = Modifier.tvFocusable()) {
                        Icon(Icons.Filled.Equalizer, contentDescription = "Equalizer")
                    }
                    IconButton(onClick = onOpenSettings, modifier = Modifier.tvFocusable()) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                TvTab(
                    label = "🎵  Audios",
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                TvTab(
                    label = "🎬  Videos",
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                )
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color(0xFF00E5FF))
            )

            if (selectedTab == 0) {
                AudioLibraryContent(viewModel)
            } else {
                VideoLibraryScreen(viewModel = viewModel, onVideoClick = onOpenVideo)
            }
        }
    }
}

@Composable
private fun TvTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color(0x3321D4FF) else Color.Transparent)
            .clickable(onClick = onClick)
            .tvFocusable(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 18.dp),
            color = if (selected) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun AudioLibraryContent(viewModel: MusicViewModel) {
    val songs by viewModel.allSongs.collectAsState()
    val favorites by viewModel.favoriteIds.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (songs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Scanning device for music…", style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            items(songs, key = { it.id }) { song ->
                SongRow(
                    song = song,
                    albumArtUri = viewModel.albumArtUri(song.albumId),
                    isFavorite = favorites.contains(song.id),
                    onClick = { viewModel.playSongs(songs, songs.indexOf(song)) },
                    onFavoriteClick = { viewModel.toggleFavorite(song.id) }
                )
            }
        }
    }
}
