package com.koshaq.music.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.koshaq.music.data.model.TrackEntity

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<TrackEntity>)


    @Query("SELECT * FROM TrackEntity ORDER BY title")
    suspend fun all(): List<TrackEntity>
}