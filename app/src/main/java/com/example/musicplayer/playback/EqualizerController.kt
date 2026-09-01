package com.example.musicplayer.playback

import android.media.audiofx.Equalizer

/**
 * Wraps the platform Equalizer effect and attaches it to ExoPlayer's audio session.
 * Get the session id from player.audioSessionId once playback has started.
 */
class EqualizerController {

    private var equalizer: Equalizer? = null

    val bandLevelRange: ShortArray
        get() = equalizer?.bandLevelRange ?: shortArrayOf(-1500, 1500)

    val numberOfBands: Short
        get() = equalizer?.numberOfBands ?: 5

    fun attach(audioSessionId: Int) {
        release()
        equalizer = Equalizer(0, audioSessionId).apply { enabled = true }
    }

    fun setEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
    }

    fun isEnabled(): Boolean = equalizer?.enabled ?: false

    fun centerFrequency(band: Short): Int = (equalizer?.getCenterFreq(band) ?: 0) / 1000 // Hz

    fun getBandLevel(band: Short): Short = equalizer?.getBandLevel(band) ?: 0

    fun setBandLevel(band: Short, level: Short) {
        equalizer?.setBandLevel(band, level)
    }

    fun applyPreset(presetIndex: Short) {
        equalizer?.usePreset(presetIndex)
    }

    fun presetNames(): List<String> {
        val eq = equalizer ?: return emptyList()
        return (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) }
    }

    fun release() {
        equalizer?.release()
        equalizer = null
    }
}
