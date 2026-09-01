package com.example.musicplayer.playback

import android.os.CountDownTimer
import androidx.media3.exoplayer.ExoPlayer

/**
 * Simple countdown that pauses playback (and optionally stops the service)
 * once the timer reaches zero. Cancel and restart to change the duration.
 */
class SleepTimer(
    private val player: ExoPlayer,
    private val onFinished: () -> Unit
) {
    private var timer: CountDownTimer? = null
    var remainingMillis: Long = 0L
        private set
    var isActive: Boolean = false
        private set

    fun start(minutes: Int) {
        cancel()
        val totalMillis = minutes * 60_000L
        remainingMillis = totalMillis
        isActive = true
        timer = object : CountDownTimer(totalMillis, 1_000L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMillis = millisUntilFinished
            }

            override fun onFinish() {
                player.pause()
                isActive = false
                remainingMillis = 0
                onFinished()
            }
        }.start()
    }

    fun cancel() {
        timer?.cancel()
        timer = null
        isActive = false
        remainingMillis = 0
    }
}
