package com.example.musicplayer.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.musicplayer.data.Video
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Shared Media3 controller for video playback. Audio playback remains in MusicController. */
class VideoPlaybackController(private val context: Context) {

    private var controller: MediaController? = null
    private var queue: List<Video> = emptyList()

    private val _state = MutableStateFlow(VideoPlaybackUiState())
    val state: StateFlow<VideoPlaybackUiState> = _state.asStateFlow()

    fun connect(onReady: () -> Unit = {}) {
        if (controller != null) {
            onReady()
            return
        }

        val token = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            controller = future.get()
            controller?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updateState(isPlaying = isPlaying)
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val video = queue.find { it.id.toString() == mediaItem?.mediaId }
                    _state.value = _state.value.copy(
                        currentVideo = video,
                        durationMs = controller?.duration?.coerceAtLeast(0L) ?: 0L
                    )
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    _state.value = _state.value.copy(errorMessage = "Unable to play this video")
                }
            })
            onReady()
        }, MoreExecutors.directExecutor())
    }

    fun playVideo(video: Video, allVideos: List<Video>) {
        val c = controller ?: return
        queue = allVideos
        val startIndex = allVideos.indexOfFirst { it.id == video.id }.coerceAtLeast(0)
        val items = allVideos.map { item ->
            MediaItem.Builder()
                .setMediaId(item.id.toString())
                .setUri(item.uriString)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(item.title)
                        .build()
                )
                .build()
        }

        c.setMediaItems(items, startIndex, 0L)
        c.prepare()
        c.play()
        _state.value = _state.value.copy(
            currentVideo = video,
            isPlaying = true,
            errorMessage = null
        )
    }

    fun playPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeatMode() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setVolume(volume: Float) {
        controller?.volume = volume.coerceIn(0f, 1f)
        updateState(volume = volume.coerceIn(0f, 1f))
    }

    fun pollPosition() {
        val c = controller ?: return
        _state.value = _state.value.copy(
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = c.duration.coerceAtLeast(0L),
            isPlaying = c.isPlaying
        )
    }

    fun player(): Player? = controller

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun release() {
        controller?.release()
        controller = null
    }

    private fun updateState(
        isPlaying: Boolean = _state.value.isPlaying,
        volume: Float = _state.value.volume
    ) {
        _state.value = _state.value.copy(
            isPlaying = isPlaying,
            volume = volume
        )
    }
}

data class VideoPlaybackUiState(
    val currentVideo: Video? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 1f,
    val errorMessage: String? = null
)
