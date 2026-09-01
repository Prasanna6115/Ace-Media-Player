package com.example.musicplayer.util

import java.io.File

/**
 * Optional lyrics support: if a .lrc file with the same base name sits next to
 * the audio file (a common convention for locally-synced lyrics), show it as
 * plain text. Returns null when no such file exists or it can't be read -
 * lyrics are a nice-to-have, never a hard requirement for playback.
 */
object LyricsLoader {

    fun loadPlainLyrics(audioFilePath: String): String? {
        val audioFile = File(audioFilePath)
        val lrcFile = File(audioFile.parentFile, audioFile.nameWithoutExtension + ".lrc")
        if (!lrcFile.exists() || !lrcFile.canRead()) return null

        return try {
            lrcFile.readLines()
                .map { line -> line.replace(Regex("\\[\\d{2}:\\d{2}(\\.\\d{2,3})?]"), "").trim() }
                .filter { it.isNotBlank() }
                .joinToString("\n")
        } catch (e: Exception) {
            null
        }
    }
}
