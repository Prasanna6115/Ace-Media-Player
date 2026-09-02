package com.example.musicplayer.data

/**
 * Represents a video item discovered through Android MediaStore.
 *
 * The content URI is used for playback. The file path is retained for
 * compatibility with devices/apps that expose a real filesystem path.
 */
data class Video(
    val id: Long,
    val title: String,
    val duration: Long,
    val path: String,
    val uriString: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val size: Long
)
