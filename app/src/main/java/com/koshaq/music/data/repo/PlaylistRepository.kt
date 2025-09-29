package com.koshaq.music.data.repo

import com.koshaq.music.data.db.dao.PlaylistDao
import com.koshaq.music.data.model.PlaylistEntity
import com.koshaq.music.data.model.PlaylistTrackCrossRef

class PlaylistRepository(private val dao: PlaylistDao) {
    suspend fun create(name: String) = dao.insertPlaylist(PlaylistEntity(name = name))
    suspend fun rename(id: Long, name: String) = dao.updatePlaylist(PlaylistEntity(id, name))
    suspend fun delete(id: Long) = dao.deletePlaylist(PlaylistEntity(id, ""))
    suspend fun add(playlistId: Long, trackId: Long, pos: Int) =
        dao.addToPlaylist(PlaylistTrackCrossRef(playlistId, trackId, pos))

    suspend fun remove(playlistId: Long, trackId: Long) =
        dao.removeFromPlaylist(playlistId, trackId)

    suspend fun clear(playlistId: Long) = dao.clearPlaylist(playlistId)
    suspend fun all() = dao.playlists()
}