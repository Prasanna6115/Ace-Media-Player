package com.example.musicplayer.playback

import android.content.Context
import android.media.AudioManager
import kotlin.math.roundToInt

/** Keeps ACE media playback volume aligned with Android's STREAM_MUSIC volume. */
class SystemVolumeController(context: Context) {
    private val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun fraction(): Float {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max.toFloat()
    }

    fun setFraction(value: Float) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val target = (value.coerceIn(0f, 1f) * max).roundToInt().coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
    }
}
