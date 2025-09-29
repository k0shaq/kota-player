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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val audioRepo = AudioRepository(app)

    val playerConn = PlayerConnection(app)

    private val _library = MutableStateFlow(listOf<TrackEntity>())
    val library = _library.asStateFlow()

    private val _playlists = MutableStateFlow(
        db.playlistDao().let { emptyList<PlaylistWithTracks>() })
    val playlists = _playlists.asStateFlow()

    fun scanAndPersist() = viewModelScope.launch(Dispatchers.IO) {
        val tracks = audioRepo.queryDeviceTracks().map { audioRepo.toEntity(it) }
        db.trackDao().upsertAll(tracks)
        _library.value = db.trackDao().all()
    }

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

    fun clearPlaylist(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        db.playlistDao().clearPlaylist(id)
        _playlists.value = db.playlistDao().playlists()
    }

    suspend fun createAndReturnId(name: String): Long = withContext(Dispatchers.IO) {
        val id = db.playlistDao().insertPlaylist(PlaylistEntity(name = name))
        _playlists.value = db.playlistDao().playlists()
        id
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: Long) =
        viewModelScope.launch(Dispatchers.IO) {
            val pos = db.playlistDao().nextPosition(playlistId)
            db.playlistDao().addToPlaylist(
                PlaylistTrackCrossRef(
                    playlistId = playlistId,
                    trackId = trackId,
                    position = pos
                )
            )
            _playlists.value = db.playlistDao().playlists()
        }

    fun playQueueFrom(list: List<TrackEntity>, shuffle: Boolean) = viewModelScope.launch {
        val c = playerConn.controller.get()
        val items =
            list.map { playerConn.toMediaItem(it.contentUri, it.title, it.artist, it.album) }
        c.setMediaItems(items)
        c.prepare()
        c.shuffleModeEnabled = shuffle
        c.playWhenReady = true
    }

    fun controls(action: (Player) -> Unit) = viewModelScope.launch {
        action(playerConn.controller.get())
    }
}