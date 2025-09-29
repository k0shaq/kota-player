package com.koshaq.music.data.repo

import com.koshaq.music.data.db.dao.PlaylistDao
import com.koshaq.music.data.model.PlaylistEntity
import com.koshaq.music.data.model.PlaylistTrackCrossRef

class PlaylistRepository(private val dao: PlaylistDao) {
    suspend fun create(name: String): Long =
        dao.insertPlaylist(PlaylistEntity(name = name))

    suspend fun rename(id: Long, name: String) =
        dao.updatePlaylist(PlaylistEntity(id, name))

    suspend fun delete(id: Long) =
        dao.deletePlaylist(PlaylistEntity(id, ""))

    suspend fun all() = dao.playlists()

    suspend fun getPlaylist(id: Long) = dao.getPlaylist(id)

    suspend fun add(playlistId: Long, trackId: Long, position: Int) =
        dao.addToPlaylist(PlaylistTrackCrossRef(playlistId, trackId, position))

    suspend fun remove(playlistId: Long, trackId: Long) =
        dao.removeFromPlaylist(playlistId, trackId)

    suspend fun nextPosition(playlistId: Long) = dao.nextPosition(playlistId)

    suspend fun tracksInPlaylist(playlistId: Long) = dao.tracksInPlaylist(playlistId)
}