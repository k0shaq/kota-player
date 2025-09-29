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
import com.koshaq.music.data.repo.PlaylistRepository
import com.koshaq.music.player.PlayerConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


enum class SortBy { TITLE, ARTIST, ALBUM, DATE_ADDED }


class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val audioRepo = AudioRepository(app)
    val playerConn = PlayerConnection(app)


    // ── Library source
    private val _library = MutableStateFlow(listOf<TrackEntity>())
    val library: StateFlow<List<TrackEntity>> = _library.asStateFlow()


    // ── Playlists
    private val _playlists =
        MutableStateFlow(emptyList<PlaylistWithTracks>())
    val playlists: StateFlow<List<PlaylistWithTracks>> =
        _playlists.asStateFlow()


    // ── Search / Filters / Sort
    val query = MutableStateFlow("")
    val filterArtist = MutableStateFlow<String?>(null)
    val filterAlbum = MutableStateFlow<String?>(null)
    val sortBy = MutableStateFlow(SortBy.DATE_ADDED)


    val filteredLibrary: StateFlow<List<TrackEntity>> = combine(
        library, query, filterArtist, filterAlbum, sortBy
    ) { lib, q, fArtist, fAlbum, sort ->
        var out = lib
        if (q.isNotBlank()) {
            val ql = q.trim().lowercase()
            out = out.filter { t ->
                t.title.lowercase().contains(ql) ||
                        t.artist.lowercase().contains(ql) ||
                        t.album.lowercase().contains(ql)
            }
        }
        fArtist?.let { fa -> out = out.filter { it.artist == fa } }
        fAlbum?.let { fb -> out = out.filter { it.album == fb } }
        out.sortedWith(
            when (sort) {
                SortBy.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
                SortBy.ARTIST -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.artist }
                SortBy.ALBUM -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.album }
                SortBy.DATE_ADDED -> compareByDescending<TrackEntity> { it.dateAdded }
            }
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Library load
    fun scanAndPersist() = viewModelScope.launch(Dispatchers.IO) {
        val tracks = audioRepo.queryDeviceTracks().map { audioRepo.toEntity(it) }
        db.trackDao().upsertAll(tracks)
        _library.value = db.trackDao().all()
    }


    // ── Playlists CRUD
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

    fun addTrackToPlaylist(playlistId: Long, trackId: Long) =
        viewModelScope.launch(Dispatchers.IO) {
            val pos = db.playlistDao().nextPosition(playlistId)
            db.playlistDao().addToPlaylist(PlaylistTrackCrossRef(playlistId, trackId, pos))
            _playlists.value = db.playlistDao().playlists()
        }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) =
        viewModelScope.launch(Dispatchers.IO) {
            db.playlistDao().removeFromPlaylist(playlistId, trackId)
            _playlists.value = db.playlistDao().playlists()
        }


    // ── Playback / Queue
    fun playQueueFrom(list: List<TrackEntity>, shuffle: Boolean) = viewModelScope.launch {
        val c = playerConn.controller.get()
        val items =
            list.map { playerConn.toMediaItem(it.contentUri, it.title, it.artist, it.album) }
        c.setMediaItems(items)
        c.prepare()
        c.shuffleModeEnabled = shuffle
        c.playWhenReady = true
    }

    fun controls(action: (Player) -> Unit) =
        viewModelScope.launch { action(playerConn.controller.get()) }
}