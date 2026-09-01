package com.example.musicplayer.data

/**
 * Represents a single audio track scanned from the device via MediaStore.
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,       // milliseconds
    val path: String,         // absolute file path (for folder browsing)
    val uriString: String,    // content:// uri used by ExoPlayer
    val mimeType: String,
    val folderName: String
)
