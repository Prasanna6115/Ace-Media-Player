package com.example.musicplayer.playback

import android.media.audiofx.LoudnessEnhancer

/** Optional post-volume gain applied to the shared Media3 audio session. */
class VolumeBoostController {
    private var enhancer: LoudnessEnhancer? = null
    private var targetGainMb = 0

    fun attach(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        runCatching {
            enhancer?.release()
            enhancer = LoudnessEnhancer(audioSessionId).apply {
                setTargetGain(targetGainMb)
                enabled = targetGainMb > 0
            }
        }.onFailure {
            enhancer = null
        }
    }

    fun setBoostPercent(percent: Int) {
        val safe = percent.coerceIn(100, 200)
        targetGainMb = when (safe) {
            100 -> 0
            125 -> 194
            150 -> 352
            175 -> 492
            else -> 602
        }
        runCatching {
            enhancer?.setTargetGain(targetGainMb)
            enhancer?.enabled = targetGainMb > 0
        }
    }

    fun boostPercent(): Int = when (targetGainMb) {
        0 -> 100
        194 -> 125
        352 -> 150
        492 -> 175
        else -> 200
    }

    fun release() {
        runCatching { enhancer?.release() }
        enhancer = null
    }
}
