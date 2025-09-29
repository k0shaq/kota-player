package com.koshaq.music.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.koshaq.music.data.db.dao.PlaylistDao
import com.koshaq.music.data.db.dao.TrackDao
import com.koshaq.music.data.model.PlaylistEntity
import com.koshaq.music.data.model.PlaylistTrackCrossRef
import com.koshaq.music.data.model.TrackEntity

@Database(
    entities = [TrackEntity::class, PlaylistEntity::class, PlaylistTrackCrossRef::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun get(context: Context) = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext, AppDatabase::class.java, "musify.db"
            ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
        }
    }
}