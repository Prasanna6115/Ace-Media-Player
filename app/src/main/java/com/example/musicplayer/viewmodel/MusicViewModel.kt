package com.example.musicplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.AppDatabase
import com.example.musicplayer.data.MusicRepository
import com.example.musicplayer.data.Video
import com.example.musicplayer.data.VideoRepository
import com.example.musicplayer.data.PlaylistEntity
import com.example.musicplayer.data.PlaylistSongCrossRef
import com.example.musicplayer.data.Song
import com.example.musicplayer.playback.MusicController
import com.example.musicplayer.playback.PlaybackUiState
import com.example.musicplayer.playback.VideoPlaybackController
import com.example.musicplayer.playback.VideoPlaybackUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = MusicRepository(app)
    private val videoRepository = VideoRepository(app)
    private val db = AppDatabase.getInstance(app)
    private val dao = db.musicDao()
    val musicController = MusicController(app)
    val videoController = VideoPlaybackController(app)

    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    private val _allVideos = MutableStateFlow<List<Video>>(emptyList())
    val allVideos: StateFlow<List<Video>> = _allVideos.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<Song>> = combineSearch()

    val favoriteIds: StateFlow<List<Long>> = dao.getFavoriteIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = dao.getPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playbackState: StateFlow<PlaybackUiState> = musicController.state
    val videoPlaybackState: StateFlow<VideoPlaybackUiState> = videoController.state

    private val preferences = app.getSharedPreferences("ace_settings", android.content.Context.MODE_PRIVATE)
    private val _darkTheme = MutableStateFlow(preferences.getBoolean("dark_theme", true))
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    init {
        musicController.connect { /* controller ready */ }
        videoController.connect()
    }

    fun scanLibrary() {
        viewModelScope.launch {
            _allSongs.value = repository.scanAllSongs()
        }
    }

    fun scanVideos() {
        viewModelScope.launch {
            _allVideos.value = videoRepository.scanAllVideos()
        }
    }

    fun albumArtUri(albumId: Long) = repository.albumArtUri(albumId)

    private fun combineSearch(): StateFlow<List<Song>> {
        val result = MutableStateFlow<List<Song>>(emptyList())
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(_allSongs, _searchQuery) { songs, query ->
                if (query.isBlank()) emptyList()
                else songs.filter {
                    it.title.contains(query, true) ||
                        it.artist.contains(query, true) ||
                        it.album.contains(query, true)
                }
            }.collect { result.value = it }
        }
        return result.asStateFlow()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // ---- Playback ----
    fun playSongs(songs: List<Song>, startIndex: Int) = musicController.playQueue(songs, startIndex)
    fun playPause() = musicController.playPause()
    fun next() = musicController.next()
    fun previous() = musicController.previous()
    fun seekTo(positionMs: Long) = musicController.seekTo(positionMs)
    fun toggleShuffle() = musicController.toggleShuffle()
    fun cycleRepeatMode() = musicController.cycleRepeatMode()
    fun setVolume(volume: Float) = musicController.setVolume(volume)
    fun pollPosition() = musicController.pollPosition()

    // ---- Video Playback ----
    fun playVideo(video: Video) = videoController.playVideo(video, allVideos.value)
    fun videoPlayPause() = videoController.playPause()
    fun videoNext() = videoController.next()
    fun videoPrevious() = videoController.previous()
    fun videoSeekTo(positionMs: Long) = videoController.seekTo(positionMs)
    fun videoToggleShuffle() = videoController.toggleShuffle()
    fun videoCycleRepeatMode() = videoController.cycleRepeatMode()
    fun videoSetVolume(volume: Float) = videoController.setVolume(volume)
    fun videoSubtitleOptions() = videoController.subtitleOptions
    fun videoSelectSubtitle(option: com.example.musicplayer.playback.SubtitleOption?) = videoController.selectSubtitle(option)
    fun pollVideoPosition() = videoController.pollPosition()
    fun videoPlayer() = videoController.player()

    fun setDarkTheme(enabled: Boolean) {
        _darkTheme.value = enabled
        preferences.edit().putBoolean("dark_theme", enabled).apply()
    }

    // ---- Favorites ----
    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            if (favoriteIds.value.contains(songId)) dao.removeFavorite(songId)
            else dao.addFavorite(com.example.musicplayer.data.FavoriteEntity(songId))
        }
    }

    // ---- Playlists ----
    fun createPlaylist(name: String) {
        viewModelScope.launch { dao.createPlaylist(PlaylistEntity(name = name)) }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch { dao.deletePlaylist(playlistId) }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            val position = dao.nextPosition(playlistId)
            dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId, songId, position))
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { dao.removeSongFromPlaylist(playlistId, songId) }
    }

    fun songIdsForPlaylist(playlistId: Long) = dao.getSongIdsForPlaylist(playlistId)

    override fun onCleared() {
        videoController.release()
        musicController.release()
        super.onCleared()
    }
}
