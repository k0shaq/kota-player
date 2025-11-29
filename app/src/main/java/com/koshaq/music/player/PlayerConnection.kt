package com.koshaq.music.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlayerConnection(context: Context) {
    private val sessionToken =
        SessionToken(context, ComponentName(context, PlayerService::class.java))
    val controller = MediaController.Builder(context, sessionToken).buildAsync()

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    init {
        controller.addListener(
            { _isConnected.value = true },
            ContextCompat.getMainExecutor(context)
        )
    }

    fun toMediaItem(uri: String, title: String, artist: String, album: String) =
        MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .build()
            )
            .build()
}
