package com.example.musicplayer.data

/** A video discovered from Android MediaStore. */
data class Video(
    val id: Long,
    val title: String,
    val duration: Long,
    val path: String,
    val uriString: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val size: Long,
    val folderName: String
)
