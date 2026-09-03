package com.example.musicplayer.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.C
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

/** Media3 controller dedicated to video playback. Audio playback stays untouched. */
class VideoPlaybackController(private val context: Context) {

    private var controller: MediaController? = null
    private var queue: List<Video> = emptyList()
    private var pendingVideo: Video? = null
    private var pendingQueue: List<Video> = emptyList()

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
            controller = runCatching { future.get() }.getOrNull()
            controller?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _state.value = _state.value.copy(
                        isPlaying = isPlaying,
                        positionMs = controller?.currentPosition?.coerceAtLeast(0L) ?: _state.value.positionMs,
                        durationMs = controller?.duration?.coerceAtLeast(0L) ?: _state.value.durationMs
                    )
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val video = queue.find { it.id.toString() == mediaItem?.mediaId }
                    _state.value = _state.value.copy(
                        currentVideo = video,
                        positionMs = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L,
                        durationMs = controller?.duration?.coerceAtLeast(0L) ?: (video?.duration ?: 0L)
                    )
                }

                override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                    updateSubtitleTracks(controller)
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    _state.value = _state.value.copy(errorMessage = "Unable to play this video")
                }
            })

            val queuedVideo = pendingVideo
            val queuedList = pendingQueue
            pendingVideo = null
            pendingQueue = emptyList()
            if (queuedVideo != null && queuedList.isNotEmpty()) {
                playVideoInternal(queuedVideo, queuedList)
            }
            onReady()
        }, MoreExecutors.directExecutor())
    }

    fun playVideo(video: Video, allVideos: List<Video>) {
        if (controller == null) {
            pendingVideo = video
            pendingQueue = allVideos
            connect()
            return
        }
        playVideoInternal(video, allVideos)
    }

    private fun playVideoInternal(video: Video, allVideos: List<Video>) {
        val c = controller ?: return
        queue = allVideos
        val startIndex = allVideos.indexOfFirst { it.id == video.id }.coerceAtLeast(0)
        val items = allVideos.map { item ->
            MediaItem.Builder()
                .setMediaId(item.id.toString())
                .setUri(item.uriString)
                .apply { if (item.mimeType.isNotBlank()) setMimeType(item.mimeType) }
                .setMediaMetadata(MediaMetadata.Builder().setTitle(item.title).build())
                .build()
        }

        if (items.isEmpty()) return

        c.setMediaItems(items, startIndex, 0L)
        c.prepare()
        c.play()
        _state.value = _state.value.copy(
            currentVideo = video,
            isPlaying = true,
            positionMs = 0L,
            durationMs = video.duration,
            subtitleTracks = emptyList(),
            subtitlesEnabled = true,
            errorMessage = null
        )
    }

    fun playPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun next() = controller?.seekToNextMediaItem()

    fun previous() = controller?.seekToPreviousMediaItem()

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun toggleShuffle() {
        controller?.let {
            it.shuffleModeEnabled = !it.shuffleModeEnabled
            _state.value = _state.value.copy(shuffleEnabled = it.shuffleModeEnabled)
        }
    }

    fun cycleRepeatMode() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _state.value = _state.value.copy(repeatMode = c.repeatMode)
    }

    fun setVolume(volume: Float) {
        val safe = volume.coerceIn(0f, 1f)
        controller?.volume = safe
        _state.value = _state.value.copy(volume = safe)
    }

    fun subtitleTracks(): List<SubtitleTrack> = _state.value.subtitleTracks

    fun setSubtitlesEnabled(enabled: Boolean) {
        val c = controller ?: return
        if (!c.isCommandAvailable(Player.COMMAND_SET_TRACK_SELECTION_PARAMETERS)) return
        c.trackSelectionParameters = c.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
            .build()
        _state.value = _state.value.copy(subtitlesEnabled = enabled)
    }

    fun selectSubtitle(track: SubtitleTrack?) {
        val c = controller ?: return
        if (!c.isCommandAvailable(Player.COMMAND_SET_TRACK_SELECTION_PARAMETERS)) return
        val builder = c.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, track == null)
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
        if (track != null) {
            val groups = c.currentTracks.groups
            val group = groups.getOrNull(track.groupIndex)
            if (group != null && group.type == C.TRACK_TYPE_TEXT) {
                builder.addOverride(
                    androidx.media3.common.TrackSelectionOverride(
                        group.mediaTrackGroup, listOf(track.trackIndex)
                    )
                )
            }
        }
        c.trackSelectionParameters = builder.build()
        _state.value = _state.value.copy(
            subtitlesEnabled = track != null,
            selectedSubtitleKey = track?.key
        )
    }

    private fun updateSubtitleTracks(c: MediaController?) {
        if (c == null) return
        val result = mutableListOf<SubtitleTrack>()
        c.currentTracks.groups.forEachIndexed { groupIndex, group ->
            if (group.type != C.TRACK_TYPE_TEXT) return@forEachIndexed
            for (trackIndex in 0 until group.length) {
                val format = group.getTrackFormat(trackIndex)
                val language = format.language?.takeUnless { it == "und" }
                val label = format.label?.takeIf { it.isNotBlank() }
                    ?: language?.uppercase()
                    ?: "Subtitle ${trackIndex + 1}"
                result += SubtitleTrack(
                    key = "$groupIndex:$trackIndex:${language.orEmpty()}:$label",
                    label = label,
                    language = language,
                    groupIndex = groupIndex,
                    trackIndex = trackIndex,
                    selected = group.isTrackSelected(trackIndex)
                )
            }
        }
        val selected = result.firstOrNull { it.selected }
        _state.value = _state.value.copy(
            subtitleTracks = result,
            subtitlesEnabled = selected != null,
            selectedSubtitleKey = selected?.key
        )
    }

    fun pollPosition() {
        val c = controller ?: return
        _state.value = _state.value.copy(
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = c.duration.takeIf { it > 0L } ?: (_state.value.currentVideo?.duration ?: 0L),
            isPlaying = c.isPlaying,
            shuffleEnabled = c.shuffleModeEnabled,
            repeatMode = c.repeatMode,
            volume = c.volume
        )
        updateSubtitleTracks(c)
    }

    fun player(): Player? = controller

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun release() {
        controller?.release()
        controller = null
        pendingVideo = null
        pendingQueue = emptyList()
    }
}


data class SubtitleTrack(
    val key: String,
    val label: String,
    val language: String?,
    val groupIndex: Int,
    val trackIndex: Int,
    val selected: Boolean
)

data class VideoPlaybackUiState(
    val currentVideo: Video? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 1f,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val subtitlesEnabled: Boolean = true,
    val selectedSubtitleKey: String? = null,
    val errorMessage: String? = null
)
