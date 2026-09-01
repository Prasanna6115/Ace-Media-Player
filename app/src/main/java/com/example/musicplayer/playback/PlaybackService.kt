package com.example.musicplayer.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.musicplayer.MainActivity

/**
 * Runs as a foreground service so playback survives app backgrounding / screen off.
 * Because we attach a MediaSession, Android automatically renders:
 *  - the lock screen media controls
 *  - the persistent playback notification (with play/pause/next/prev actions)
 * We don't have to build either UI by hand.
 */
class PlaybackService : MediaSessionService() {

    companion object {
        // Local, in-process reference so screens (e.g. Equalizer) can reach the live
        // ExoPlayer/session directly. Safe because this service always runs in the
        // app's own process (no android:process=":remote" and not used by other apps).
        var instance: PlaybackService? = null
            private set
    }

    private var mediaSession: MediaSession? = null
    lateinit var player: ExoPlayer
        private set

    var sleepTimerJob: SleepTimer? = null
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        player = ExoPlayer.Builder(this)
            .setHandleAudioBecomingNoisy(true) // pause when headphones unplugged
            .build()

        player.repeatMode = Player.REPEAT_MODE_OFF

        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openAppIntent)
            .build()

        sleepTimerJob = SleepTimer(player) { stopSelf() }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // If nothing is playing when the task is swiped away, shut the service down
        // so it doesn't linger as an empty foreground notification.
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        sleepTimerJob?.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        instance = null
        super.onDestroy()
    }
}
