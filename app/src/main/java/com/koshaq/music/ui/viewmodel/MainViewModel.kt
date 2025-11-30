package com.koshaq.music.ui.viewmodel

import android.app.Application
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortBy { DATE_ADDED_ASC, DATE_ADDED_DESC, TITLE_ASC, TITLE_DESC }

class MainViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        const val RADIO_ROKS_MAIN_URL = "https://online.radioroks.ua/RadioROKS_HD"
        const val RADIO_ROKS_UA_ROCK_URL = "https://online.radioroks.ua/RadioROKS_Ukr_HD"
    }

    private val db = AppDatabase.get(app)
    private val audioRepo = AudioRepository(app)
    val playerConn = PlayerConnection(app)

    private val _library = MutableStateFlow(listOf<TrackEntity>())
    val library: StateFlow<List<TrackEntity>> = _library.asStateFlow()

    val query = MutableStateFlow("")
    private val sortBy = MutableStateFlow(SortBy.DATE_ADDED_DESC)

    val filteredLibrary: StateFlow<List<TrackEntity>> =
        combine(library, query, sortBy) { lib, q, sort ->
            var out = if (q.isBlank()) lib else {
                val ql = q.trim().lowercase()
                lib.filter { t ->
                    t.title.lowercase().contains(ql) ||
                            t.artist.lowercase().contains(ql) ||
                            t.album.lowercase().contains(ql)
                }
            }
            when (sort) {
                SortBy.DATE_ADDED_ASC -> out.sortedBy { it.dateAdded }
                SortBy.DATE_ADDED_DESC -> out.sortedByDescending { it.dateAdded }
                SortBy.TITLE_ASC -> out.sortedBy { it.title.lowercase() }
                SortBy.TITLE_DESC -> out.sortedByDescending { it.title.lowercase() }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setSort(s: SortBy) {
        sortBy.value = s
    }

    fun scanAndPersist() = viewModelScope.launch(Dispatchers.IO) {
        val tracks = audioRepo.queryDeviceTracks().map { audioRepo.toEntity(it) }
        db.trackDao().upsertAll(tracks)
        _library.value = db.trackDao().all()
    }

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
        _playlists.value = db.playlistDao().playlists()
        id
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: Long) =
        viewModelScope.launch(Dispatchers.IO) {
            val pos = db.playlistDao().nextPosition(playlistId)
            db.playlistDao().addToPlaylist(PlaylistTrackCrossRef(playlistId, trackId, pos))
            _playlists.value = db.playlistDao().playlists()
        }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) =
        viewModelScope.launch(Dispatchers.IO) {
            db.playlistDao().removeTrackFromPlaylist(playlistId, trackId)
            _playlists.value = db.playlistDao().playlists()
        }

    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    fun smartPrevious() = controls { p ->
        if (p.currentPosition > 3000L) {
            p.seekTo(0)
        } else {
            p.seekToPrevious()
        }
        p.playWhenReady = true
    }

    fun playFromListAt(
        list: List<TrackEntity>,
        index: Int,
        shuffle: Boolean,
        resetHistory: Boolean = true
    ) = viewModelScope.launch {
        val items = list.map {
            playerConn.toMediaItem(it.contentUri, it.title, it.artist, it.album)
        }

        controls { p ->
            if (resetHistory) _history.value = emptyList()
            if (shuffle) {
                val shuffled = items.shuffled()
                p.setMediaItems(shuffled)
            } else {
                val reordered = if (index in items.indices) {
                    val first = items[index]
                    val rest = items.filterIndexed { i, _ -> i != index }
                    buildList {
                        add(first)
                        addAll(rest)
                    }
                } else {
                    items
                }
                p.setMediaItems(reordered)
            }
            p.prepare()
            p.shuffleModeEnabled = false
            p.playWhenReady = true
        }
    }

    fun addToQueueNext(track: TrackEntity) = controls { p ->
        val item = playerConn.toMediaItem(
            track.contentUri,
            track.title,
            track.artist,
            track.album
        )
        val currentIndex = p.currentMediaItemIndex
        val insertIndex = if (currentIndex < 0) {
            p.mediaItemCount
        } else {
            (currentIndex + 1).coerceAtMost(p.mediaItemCount)
        }
        p.addMediaItem(insertIndex, item)
    }

    fun reshuffleQueuePreserveCurrent() = controls { p ->
        val count = p.mediaItemCount
        if (count <= 1) return@controls
        val currentIndex = p.currentMediaItemIndex
        if (currentIndex < 0) return@controls

        val currentItem = p.getMediaItemAt(currentIndex)
        val currentPosition = p.currentPosition

        val before = (0 until currentIndex).map { idx ->
            p.getMediaItemAt(idx)
        }
        val after = (currentIndex + 1 until count).map { idx ->
            p.getMediaItemAt(idx)
        }.shuffled()

        val newItems = buildList {
            addAll(before)
            add(currentItem)
            addAll(after)
        }
        val newIndex = before.size

        p.setMediaItems(newItems, newIndex, currentPosition)
        p.playWhenReady = true
    }

    fun toggleRadioMain() = controls { p ->
        val currentUri = p.currentMediaItem?.localConfiguration?.uri?.toString()
        if (currentUri == RADIO_ROKS_MAIN_URL && p.playWhenReady) {
            p.pause()
        } else {
            val item = playerConn.toMediaItem(
                RADIO_ROKS_MAIN_URL,
                "Radio ROKS",
                "Рок. Тільки рок",
                "Online stream"
            )
            p.setMediaItem(item)
            p.prepare()
            p.playWhenReady = true
        }
    }

    fun toggleRadioUaRock() = controls { p ->
        val currentUri = p.currentMediaItem?.localConfiguration?.uri?.toString()
        if (currentUri == RADIO_ROKS_UA_ROCK_URL && p.playWhenReady) {
            p.pause()
        } else {
            val item = playerConn.toMediaItem(
                RADIO_ROKS_UA_ROCK_URL,
                "Radio ROKS Український рок",
                "Український рок",
                "Online stream"
            )
            p.setMediaItem(item)
            p.prepare()
            p.playWhenReady = true
        }
    }

    fun renameTrack(id: Long, newTitle: String?, newArtist: String?) =
        viewModelScope.launch(Dispatchers.IO) {
            val dao = db.trackDao()
            val current = dao.get(id) ?: return@launch

            val titleClean = newTitle?.trim().orEmpty()
            val artistClean = newArtist?.trim().orEmpty()

            val values = ContentValues()
            if (titleClean.isNotBlank()) {
                values.put(MediaStore.Audio.Media.TITLE, titleClean)
                values.put(MediaStore.Audio.Media.DISPLAY_NAME, titleClean)
            }
            if (artistClean.isNotBlank()) {
                values.put(MediaStore.Audio.Media.ARTIST, artistClean)
            }

            val resolver = getApplication<Application>().contentResolver
            try {
                if (values.size() > 0) {
                    resolver.update(Uri.parse(current.contentUri), values, null, null)
                }
            } catch (e: Exception) {
            }

            val updated = current.copy(
                title = if (titleClean.isNotBlank()) titleClean else current.title,
                artist = if (artistClean.isNotBlank()) artistClean else current.artist
            )

            dao.update(updated)
            _library.value = dao.all()
            _playlists.value = db.playlistDao().playlists()
        }

    suspend fun currentItemTrack(): TrackEntity? {
        val controller = playerConn.controller.get()
        val uri = withContext(Dispatchers.Main) {
            controller.currentMediaItem?.localConfiguration?.uri?.toString()
        } ?: return null

        return withContext(Dispatchers.IO) {
            db.trackDao().findByContentUri(uri)
        }
    }

    private suspend fun cleanupDeletedTrack(track: TrackEntity) {
        db.playlistDao().removeTrackEverywhere(track.trackId)
        db.trackDao().deleteById(track.trackId)
        _library.value = db.trackDao().all()
        _playlists.value = db.playlistDao().playlists()

        withContext(Dispatchers.Main) {
            val player = playerConn.controller.get()
            val indices = (0 until player.mediaItemCount).filter { idx ->
                player.getMediaItemAt(idx).localConfiguration?.uri.toString() == track.contentUri
            }
            indices.sortedDescending().forEach { idx ->
                player.removeMediaItem(idx)
            }
            if (player.mediaItemCount == 0) {
                player.stop()
            }
        }
    }

    fun handleTrackDeletedFromSystem(track: TrackEntity) =
        viewModelScope.launch(Dispatchers.IO) {
            cleanupDeletedTrack(track)
        }

    fun deleteTrackLegacy(track: TrackEntity) =
        viewModelScope.launch(Dispatchers.IO) {
            val resolver = getApplication<Application>().contentResolver
            try {
                resolver.delete(Uri.parse(track.contentUri), null, null)
            } catch (e: Exception) {
            }
            cleanupDeletedTrack(track)
        }

    fun controls(action: (Player) -> Unit) = viewModelScope.launch {
        action(playerConn.controller.get())
    }
}
