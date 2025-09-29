package com.koshaq.music.data.model

import android.net.Uri

data class DeviceTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: Uri,
    val dateAdded: Long
)