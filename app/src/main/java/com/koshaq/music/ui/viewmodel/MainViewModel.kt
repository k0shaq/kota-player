package com.koshaq.music.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.koshaq.music.data.db.AppDatabase
import com.koshaq.music.data.model.PlaylistEntity
import com.koshaq.music.data.model.PlaylistTrackCrossRef
import com.koshaq.music.data.model.PlaylistWithTracks
import com.koshaq.music.data.model.TrackEntity
import com.koshaq.music.data.repo.AudioRepository
import com.koshaq.music.player.PlayerConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortBy { DATE_ADDED_ASC, DATE_ADDED_DESC, TITLE_ASC, TITLE_DESC }

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val audioRepo = AudioRepository(app)
    val playerConn = PlayerConnection(app)

    // Library (сирі дані)
    private val _library = MutableStateFlow(listOf<TrackEntity>())
    val library: StateFlow<List<TrackEntity>> = _library.asStateFlow()

    // Пошук + сортування
    val query = MutableStateFlow("")
    private val sortBy = MutableStateFlow(SortBy.DATE_ADDED_DESC)

    // Те, що потрібно відображати у списку
    val filteredLibrary: StateFlow<List<TrackEntity>> = combine(library, query, sortBy) { lib, q, sort ->
        var out = if (q.isBlank()) lib else {
            val ql = q.trim().lowercase()
            lib.filter { t ->
                t.title.lowercase().contains(ql) ||
                        t.artist.lowercase().contains(ql) ||
                        t.album.lowercase().contains(ql)
            }
        }
        when (sort) {
            SortBy.DATE_ADDED_ASC  -> out.sortedBy        { it.dateAdded }
            SortBy.DATE_ADDED_DESC -> out.sortedByDescending { it.dateAdded }
            SortBy.TITLE_ASC       -> out.sortedBy        { it.title.lowercase() }
            SortBy.TITLE_DESC      -> out.sortedByDescending { it.title.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setSort(s: SortBy) { sortBy.value = s }

    fun scanAndPersist() = viewModelScope.launch(Dispatchers.IO) {
        val tracks = audioRepo.queryDeviceTracks().map { audioRepo.toEntity(it) }
        db.trackDao().upsertAll(tracks)
        _library.value = db.trackDao().all()
    }

    // Playlists
    private val _playlists = MutableStateFlow(emptyList<PlaylistWithTracks>())
    val playlists: StateFlow<List<PlaylistWithTracks>> = _playlists.asStateFlow()

    fun loadPlaylists() = viewModelScope.launch(Dispatchers.IO) {
        _playlists.value = db.playlistDao().playlists()
    }
    fun createPlaylist(name: String) = viewModelScope.launch(Dispatchers.IO) {
        db.playlistDao().insertPlaylist(PlaylistEntity(name = name))
        _playlists.value = db.playlistDao().playlists()
    }
    fun renamePlaylist(id: Long, name: String) = viewModelScope.launch(Dispatchers.IO) {
        db.playlistDao().updatePlaylist(PlaylistEntity(id, name))
        _playlists.value = db.playlistDao().playlists()
    }
    fun deletePlaylist(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        db.playlistDao().deletePlaylist(PlaylistEntity(id, ""))
        _playlists.value = db.playlistDao().playlists()
    }
    suspend fun createAndReturnId(name: String): Long = withContext(Dispatchers.IO) {
        val id = db.playlistDao().insertPlaylist(PlaylistEntity(name = name))
        _playlists.value = db.playlistDao().playlists(); id
    }
    fun addTrackToPlaylist(playlistId: Long, trackId: Long) = viewModelScope.launch(Dispatchers.IO) {
        val pos = db.playlistDao().nextPosition(playlistId)
        db.playlistDao().addToPlaylist(PlaylistTrackCrossRef(playlistId, trackId, pos))
        _playlists.value = db.playlistDao().playlists()
    }
    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) = viewModelScope.launch(Dispatchers.IO) {
        db.playlistDao().removeFromPlaylist(playlistId, trackId)
        _playlists.value = db.playlistDao().playlists()
    }

    // Playback
    fun playQueueFrom(list: List<TrackEntity>, shuffle: Boolean) = viewModelScope.launch {
        val c = playerConn.controller.get()
        val items = list.map { playerConn.toMediaItem(it.contentUri, it.title, it.artist, it.album) }
        c.setMediaItems(items)
        c.prepare()
        c.shuffleModeEnabled = shuffle
        c.playWhenReady = true
    }
    fun controls(action: (Player) -> Unit) = viewModelScope.launch { action(playerConn.controller.get()) }
}
