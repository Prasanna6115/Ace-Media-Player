package com.example.musicplayer.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.musicplayer.data.Song
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Playback UI state exposed to Compose screens. */
data class PlaybackUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val volume: Float = 1f
)

/**
 * Thin wrapper that binds a [MediaController] to the running [PlaybackService]
 * and republishes player state as a StateFlow the UI can collect.
 */
class MusicController(private val context: Context) {

    private var controller: MediaController? = null
    private val systemVolumeController = SystemVolumeController(context)
    private var queue: List<Song> = emptyList()

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    fun connect(onReady: () -> Unit) {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, com.example.musicplayer.playback.PlaybackService::class.java)
        )
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener({
            controller = future.get()
            attachListener()
            onReady()
        }, MoreExecutors.directExecutor())
    }

    private fun attachListener() {
        controller?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val song = queue.find { it.id.toString() == mediaItem?.mediaId }
                _state.value = _state.value.copy(
                    currentSong = song,
                    durationMs = controller?.duration?.coerceAtLeast(0) ?: 0L
                )
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _state.value = _state.value.copy(shuffleEnabled = shuffleModeEnabled)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _state.value = _state.value.copy(repeatMode = repeatMode)
            }
        })
    }

    /** Call periodically (e.g. every 500ms) from the UI to update the seek bar position. */
    fun pollPosition() {
        val c = controller ?: return
        _state.value = _state.value.copy(
            positionMs = c.currentPosition.coerceAtLeast(0),
            durationMs = c.duration.coerceAtLeast(0),
            volume = systemVolumeController.fraction()
        )
    }


    data class PlaybackSnapshot(
        val songs: List<Song>,
        val index: Int,
        val positionMs: Long,
        val isPlaying: Boolean,
        val shuffleEnabled: Boolean,
        val repeatMode: Int,
        val volume: Float
    )

    fun snapshot(): PlaybackSnapshot? {
        val c = controller ?: return null
        val song = _state.value.currentSong ?: return null
        if (queue.isEmpty()) return null
        return PlaybackSnapshot(
            songs = queue.toList(),
            index = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0),
            positionMs = c.currentPosition.coerceAtLeast(0L),
            isPlaying = c.isPlaying,
            shuffleEnabled = c.shuffleModeEnabled,
            repeatMode = c.repeatMode,
            volume = c.volume
        )
    }

    fun restore(snapshot: PlaybackSnapshot) {
        val c = controller ?: return
        if (snapshot.songs.isEmpty()) return
        val items = snapshot.songs.map { song ->
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
        queue = snapshot.songs
        c.setMediaItems(items, snapshot.index.coerceIn(0, items.lastIndex), snapshot.positionMs)
        c.shuffleModeEnabled = snapshot.shuffleEnabled
        c.repeatMode = snapshot.repeatMode
        systemVolumeController.setFraction(snapshot.volume)
        c.volume = 1f
        c.prepare()
        if (snapshot.isPlaying) c.play() else c.pause()
        _state.value = _state.value.copy(
            currentSong = snapshot.songs.getOrNull(snapshot.index),
            positionMs = snapshot.positionMs,
            isPlaying = snapshot.isPlaying,
            shuffleEnabled = snapshot.shuffleEnabled,
            repeatMode = snapshot.repeatMode,
            volume = snapshot.volume
        )
    }

    fun playQueue(songs: List<Song>, startIndex: Int) {
        queue = songs
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
            setMediaItems(items, startIndex, 0L)
            prepare()
            play()
        }
        _state.value = _state.value.copy(currentSong = songs.getOrNull(startIndex))
    }

    fun playPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun next() = controller?.seekToNextMediaItem()
    fun previous() = controller?.seekToPreviousMediaItem()
    fun seekTo(positionMs: Long) = controller?.seekTo(positionMs)

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    /** Cycles OFF -> ALL -> ONE -> OFF */
    fun cycleRepeatMode() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setVolume(volume: Float) {
        val safe = volume.coerceIn(0f, 1f)
        systemVolumeController.setFraction(safe)
        controller?.volume = 1f
        _state.value = _state.value.copy(volume = systemVolumeController.fraction())
    }

    fun release() {
        controller?.release()
        controller = null
    }
}
