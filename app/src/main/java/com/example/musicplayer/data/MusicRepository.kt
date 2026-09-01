package com.example.musicplayer.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

/**
 * Scans the device for audio files using MediaStore.
 *
 * MediaStore.Audio already indexes every common container the OS recognizes:
 * MP3, FLAC, WAV, AAC/M4A, OGG, OPUS. ALAC is muxed inside an M4A/MP4 container,
 * so it shows up here too, but whether it actually *plays* depends on whether the
 * device's decoder supports ALAC (Media3/ExoPlayer relies on the platform codec
 * for ALAC since there's no dedicated software decoder bundled) - hence "depends
 * on device" in the feature list.
 */
class MusicRepository(private val context: Context) {

    fun scanAllSongs(): List<Song> {
        val songs = mutableListOf<Song>()

        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE
        )

        // is_music = 1 filters out notification sounds / ringtones / call recordings etc.
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val path = cursor.getString(dataCol) ?: ""
                val folder = path.substringBeforeLast('/', "Unknown").substringAfterLast('/')
                val contentUri = ContentUris.withAppendedId(collection, id)

                songs += Song(
                    id = id,
                    title = cursor.getString(titleCol) ?: "Unknown",
                    artist = cursor.getString(artistCol) ?: "Unknown Artist",
                    album = cursor.getString(albumCol) ?: "Unknown Album",
                    albumId = cursor.getLong(albumIdCol),
                    duration = cursor.getLong(durationCol),
                    path = path,
                    uriString = contentUri.toString(),
                    mimeType = cursor.getString(mimeCol) ?: "",
                    folderName = folder
                )
            }
        }
        return songs
    }

    fun albumArtUri(albumId: Long) =
        ContentUris.withAppendedId(android.net.Uri.parse("content://media/external/audio/albumart"), albumId)
}
