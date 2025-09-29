package com.koshaq.music.data.repo

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.koshaq.music.data.model.DeviceTrack
import com.koshaq.music.data.model.TrackEntity

class AudioRepository(private val context: Context) {
    fun queryDeviceTracks(): List<DeviceTrack> {
        val list = mutableListOf<DeviceTrack>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED
        )
        val selection = MediaStore.Audio.Media.IS_MUSIC + "!=0"
        context.contentResolver.query(
            collection, projection, selection, null,
            MediaStore.Audio.Media.DATE_ADDED + " DESC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val tCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val aCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val alCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val dCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val addCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                list += DeviceTrack(
                    id = id,
                    title = c.getString(tCol) ?: "Unknown",
                    artist = c.getString(aCol) ?: "Unknown",
                    album = c.getString(alCol) ?: "",
                    durationMs = c.getLong(dCol),
                    contentUri = uri,
                    dateAdded = c.getLong(addCol)
                )
            }
        }
        return list
    }


    fun toEntity(d: DeviceTrack) = TrackEntity(
        trackId = d.id,
        title = d.title,
        artist = d.artist,
        album = d.album,
        durationMs = d.durationMs,
        contentUri = d.contentUri.toString(),
        dateAdded = d.dateAdded
    )
}