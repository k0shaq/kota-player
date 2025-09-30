package com.koshaq.music.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.koshaq.music.data.model.PlaylistEntity
import com.koshaq.music.data.model.PlaylistTrackCrossRef
import com.koshaq.music.data.model.PlaylistWithTracks
import com.koshaq.music.data.model.TrackEntity

@Dao
interface PlaylistDao {
    @Insert
    suspend fun insertPlaylist(p: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(p: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(p: PlaylistEntity)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToPlaylist(ref: PlaylistTrackCrossRef)


    @Query("DELETE FROM PlaylistTrackCrossRef WHERE playlistId=:playlistId AND trackId=:trackId")
    suspend fun removeFromPlaylist(playlistId: Long, trackId: Long)


    @Query("SELECT COALESCE(MAX(position)+1, 0) FROM PlaylistTrackCrossRef WHERE playlistId=:playlistId")
    suspend fun nextPosition(playlistId: Long): Int


    @Transaction
    @Query("SELECT * FROM PlaylistEntity")
    suspend fun playlists(): List<PlaylistWithTracks>


    @Query("SELECT * FROM PlaylistEntity WHERE playlistId=:id")
    suspend fun getPlaylist(id: Long): PlaylistEntity?


    @Query(
        """
SELECT t.* FROM PlaylistTrackCrossRef r
INNER JOIN TrackEntity t ON t.trackId = r.trackId
WHERE r.playlistId = :playlistId
ORDER BY r.position
"""
    )
    suspend fun tracksInPlaylist(playlistId: Long): List<TrackEntity>
}