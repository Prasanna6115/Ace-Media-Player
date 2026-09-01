package com.example.musicplayer.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.musicplayer.data.Song
import com.example.musicplayer.data.Video
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Playback UI state exposed to Compose screens. */
data class PlaybackUiState(
    val currentSong: Song? = null,
    val currentVideo: Video? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val volume: Float = 1f
)

/** Binds the Compose UI to the Media3 MediaController. */
class MusicController(private val context: Context) {

    private var controller: MediaController? = null
    private var audioQueue: List<Song> = emptyList()
    private var videoQueue: List<Video> = emptyList()
    private var playingVideo = false

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    fun connect(onReady: () -> Unit) {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener({
            runCatching { future.get() }.onSuccess {
                controller = it
                attachListener()
                onReady()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun attachListener() {
        controller?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (playingVideo) {
                    val video = videoQueue.find { it.id.toString() == mediaItem?.mediaId }
                    _state.value = _state.value.copy(
                        currentSong = null,
                        currentVideo = video,
                        durationMs = controller?.duration?.coerceAtLeast(0) ?: 0L,
                        positionMs = 0L
                    )
                } else {
                    val song = audioQueue.find { it.id.toString() == mediaItem?.mediaId }
                    _state.value = _state.value.copy(
                        currentSong = song,
                        currentVideo = null,
                        durationMs = controller?.duration?.coerceAtLeast(0) ?: 0L,
                        positionMs = 0L
                    )
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _state.value = _state.value.copy(shuffleEnabled = shuffleModeEnabled)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _state.value = _state.value.copy(repeatMode = repeatMode)
            }
        })
    }

    fun pollPosition() {
        val c = controller ?: return
        _state.value = _state.value.copy(
            positionMs = c.currentPosition.coerceAtLeast(0),
            durationMs = c.duration.coerceAtLeast(0)
        )
    }

    fun playQueue(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        playingVideo = false
        audioQueue = songs
        videoQueue = emptyList()

        val items = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.uriString)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .build()
                )
                .build()
        }

        controller?.apply {
            setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
            prepare()
            play()
        }

        _state.value = _state.value.copy(
            currentSong = songs.getOrNull(startIndex),
            currentVideo = null,
            positionMs = 0L,
            durationMs = songs.getOrNull(startIndex)?.duration ?: 0L
        )
    }

    fun playVideo(video: Video) {
        playingVideo = true
        audioQueue = emptyList()
        videoQueue = listOf(video)

        val item = MediaItem.Builder()
            .setMediaId(video.id.toString())
            .setUri(video.uriString)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(video.title)
                    .build()
            )
            .build()

        controller?.apply {
            setMediaItem(item, 0L)
            prepare()
            play()
        }

        _state.value = _state.value.copy(
            currentSong = null,
            currentVideo = video,
            positionMs = 0L,
            durationMs = video.duration
        )
    }

    fun playPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun next() = controller?.seekToNextMediaItem()
    fun previous() = controller?.seekToPreviousMediaItem()
    fun seekTo(positionMs: Long) = controller?.seekTo(positionMs.coerceAtLeast(0L))

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
        val safeVolume = volume.coerceIn(0f, 1f)
        controller?.volume = safeVolume
        _state.value = _state.value.copy(volume = safeVolume)
    }

    fun stop() {
        controller?.stop()
        controller?.clearMediaItems()
        audioQueue = emptyList()
        videoQueue = emptyList()
        playingVideo = false
        _state.value = PlaybackUiState()
    }

    fun release() {
        controller?.release()
        controller = null
    }
}
