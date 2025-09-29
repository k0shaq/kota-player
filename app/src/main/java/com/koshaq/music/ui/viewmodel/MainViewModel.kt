package com.koshaq.music.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.koshaq.music.data.db.AppDatabase
import com.koshaq.music.data.model.PlaylistWithTracks
import com.koshaq.music.data.model.TrackEntity
import com.koshaq.music.data.repo.AudioRepository
import com.koshaq.music.data.repo.PlaylistRepository
import com.koshaq.music.player.PlayerConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val audioRepo = AudioRepository(app)
    private val playlistRepo = PlaylistRepository(db.playlistDao())


    val playerConn = PlayerConnection(app)


    private val _library = MutableStateFlow(listOf<TrackEntity>())
    val library = _library.asStateFlow()


    private val _playlists = MutableStateFlow(listOf<PlaylistWithTracks>())
    val playlists = _playlists.asStateFlow()


    fun scanAndPersist() = viewModelScope.launch(Dispatchers.IO) {
        val tracks = audioRepo.queryDeviceTracks().map { audioRepo.toEntity(it) }
        db.trackDao().upsertAll(tracks)
        _library.value = db.trackDao().all()
    }


    fun loadPlaylists() =
        viewModelScope.launch(Dispatchers.IO) { _playlists.value = playlistRepo.all() }


    fun createPlaylist(name: String) = viewModelScope.launch(Dispatchers.IO) {
        playlistRepo.create(name); loadPlaylists()
    }

    fun renamePlaylist(id: Long, name: String) = viewModelScope.launch(Dispatchers.IO) {
        playlistRepo.rename(id, name); loadPlaylists()
    }

    fun deletePlaylist(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        playlistRepo.delete(id); loadPlaylists()
    }

    fun clearPlaylist(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        playlistRepo.clear(id); loadPlaylists()
    }

    fun addToPlaylist(pid: Long, trackId: Long, pos: Int) = viewModelScope.launch(Dispatchers.IO) {
        playlistRepo.add(pid, trackId, pos); loadPlaylists()
    }


    fun playQueueFrom(ids: List<TrackEntity>, shuffle: Boolean) = viewModelScope.launch {
        val c = playerConn.controller.get()
        val items = ids.map { playerConn.toMediaItem(it.contentUri, it.title, it.artist, it.album) }
        c.setMediaItems(items)
        c.prepare()
        c.shuffleModeEnabled = shuffle
        c.playWhenReady = true
    }


    fun controls(action: (Player) -> Unit) = viewModelScope.launch {
        action(playerConn.controller.get())
    }
}