package com.example.musicplayer.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import java.io.File

/** Scans device videos without changing the existing audio scanner. */
class VideoRepository(private val context: Context) {

    fun scanAllVideos(): List<Video> {
        val videos = mutableListOf<Video>()
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val projection = mutableListOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.SIZE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection += MediaStore.Video.Media.RELATIVE_PATH
        }

        val selection = "${MediaStore.Video.Media.DURATION} > 0"
        val sortOrder = "${MediaStore.Video.Media.TITLE} COLLATE NOCASE ASC"

        context.contentResolver.query(
            collection,
            projection.toTypedArray(),
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val relativePathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
            } else {
                -1
            }

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(collection, id)
                val path = if (dataCol >= 0 && !cursor.isNull(dataCol)) cursor.getString(dataCol) else ""
                val relativePath = if (relativePathCol >= 0 && !cursor.isNull(relativePathCol)) {
                    cursor.getString(relativePathCol)
                } else {
                    null
                }

                videos += Video(
                    id = id,
                    title = cursor.getString(titleCol).orEmpty().ifBlank { "Unknown Video" },
                    duration = cursor.getLong(durationCol),
                    path = path,
                    uriString = contentUri.toString(),
                    mimeType = cursor.getString(mimeCol).orEmpty(),
                    width = cursor.getInt(widthCol),
                    height = cursor.getInt(heightCol),
                    size = cursor.getLong(sizeCol),
                    folderName = folderName(relativePath, path)
                )
            }
        }

        return videos
    }

    private fun folderName(relativePath: String?, absolutePath: String): String {
        relativePath?.trimEnd('/')?.substringAfterLast('/')?.takeIf { it.isNotBlank() }?.let { return it }
        return absolutePath.substringBeforeLast(File.separator, "Videos")
            .substringAfterLast(File.separator)
            .ifBlank { "Videos" }
    }
}
