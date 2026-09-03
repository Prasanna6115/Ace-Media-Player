package com.example.musicplayer.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.musicplayer.data.Video
import java.io.File
import java.util.Locale
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
    private val _subtitleOptions = MutableStateFlow<List<SubtitleOption>>(emptyList())
    val subtitleOptions: StateFlow<List<SubtitleOption>> = _subtitleOptions.asStateFlow()
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
                    refreshSubtitleOptions()
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
                .apply {
                    val subtitles = findExternalSubtitleFiles(item)
                    if (subtitles.isNotEmpty()) {
                        setSubtitleConfigurations(subtitles.map { file ->
                            MediaItem.SubtitleConfiguration.Builder(android.net.Uri.fromFile(file))
                                .setMimeType(subtitleMimeType(file))
                                .setLanguage(languageFromSubtitleName(file))
                                .setLabel(subtitleLabel(file))
                                .setSelectionFlags(0)
                                .build()
                        })
                    }
                }
                .build()
        }

        if (items.isEmpty()) return

        c.setMediaItems(items, startIndex, 0L)
        c.prepare()
        c.trackSelectionParameters = c.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .build()
        c.play()
        _state.value = _state.value.copy(
            currentVideo = video,
            isPlaying = true,
            positionMs = 0L,
            durationMs = video.duration,
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
    }

    fun player(): Player? = controller

    fun selectSubtitle(option: SubtitleOption?) {
        val c = controller ?: return
        val builder = c.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, option == null)
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
        if (option != null) {
            val group = c.currentTracks.groups.getOrNull(option.groupIndex)?.mediaTrackGroup
            if (group != null) {
                builder.addOverride(TrackSelectionOverride(group, listOf(option.trackIndex)))
            }
        }
        c.trackSelectionParameters = builder.build()
        refreshSubtitleOptions()
    }

    private fun refreshSubtitleOptions() {
        val c = controller ?: return
        val options = mutableListOf<SubtitleOption>()
        c.currentTracks.groups.forEachIndexed { groupIndex, group ->
            if (group.type != C.TRACK_TYPE_TEXT) return@forEachIndexed
            for (trackIndex in 0 until group.length) {
                val format = group.getTrackFormat(trackIndex)
                val label = format.label?.takeIf { it.isNotBlank() }
                    ?: format.language?.takeIf { it.isNotBlank() }?.uppercase(Locale.getDefault())
                    ?: "Subtitle ${options.size + 1}"
                options += SubtitleOption(
                    groupIndex = groupIndex,
                    trackIndex = trackIndex,
                    label = label,
                    language = format.language
                )
            }
        }
        _subtitleOptions.value = options.distinctBy { "${it.label}|${it.language}|${it.groupIndex}|${it.trackIndex}" }
    }

    private fun findExternalSubtitleFiles(video: Video): List<File> {
        val path = video.path.takeIf { it.isNotBlank() } ?: return emptyList()
        val videoFile = File(path)
        val parent = videoFile.parentFile ?: return emptyList()
        if (!parent.isDirectory) return emptyList()
        val base = videoFile.nameWithoutExtension
        val allowed = setOf("srt", "vtt", "ass", "ssa", "ttml", "xml")
        return parent.listFiles()?.filter { file ->
            file.isFile && file.extension.lowercase(Locale.ROOT) in allowed &&
                file.nameWithoutExtension.equals(base, ignoreCase = true)
        }?.sortedBy { it.name.lowercase(Locale.ROOT) }.orEmpty()
    }

    private fun subtitleMimeType(file: File): String = when (file.extension.lowercase(Locale.ROOT)) {
        "srt" -> "application/x-subrip"
        "vtt" -> "text/vtt"
        "ass", "ssa" -> "text/x-ssa"
        "ttml", "xml" -> "application/ttml+xml"
        else -> "text/vtt"
    }

    private fun languageFromSubtitleName(file: File): String? {
        val parts = file.nameWithoutExtension.split('.', '_', '-', ' ')
        return parts.drop(1).firstOrNull { it.length in 2..3 && it.all(Char::isLetter) }?.lowercase(Locale.ROOT)
    }

    private fun subtitleLabel(file: File): String {
        val suffix = file.nameWithoutExtension.substringAfterLast('.', "")
        return if (suffix.isNotBlank()) suffix.replaceFirstChar { it.uppercase() } else "Subtitle"
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun release() {
        controller?.release()
        controller = null
        _subtitleOptions.value = emptyList()
        pendingVideo = null
        pendingQueue = emptyList()
    }
}


data class SubtitleOption(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String?
)

data class VideoPlaybackUiState(
    val currentVideo: Video? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 1f,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val errorMessage: String? = null
)
