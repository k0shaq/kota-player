package com.koshaq.music.player

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import com.koshaq.music.R
import com.koshaq.music.ui.MainActivity

@UnstableApi
class PlayerService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var session: MediaSession
    private lateinit var sessionCompat: MediaSessionCompat
    private var notificationManager: PlayerNotificationManager? = null

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            shuffleModeEnabled = false
        }

        session = MediaSession.Builder(this, player).build()

        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        sessionCompat = MediaSessionCompat(this, "MusifyCompat").apply {
            setSessionActivity(contentIntent)
            isActive = true
        }

        notificationManager = PlayerNotificationManager.Builder(this, 1, "musify_channel")
            .setMediaDescriptionAdapter(object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun getCurrentContentTitle(p: Player) =
                    p.mediaMetadata.title ?: "Musify"

                override fun createCurrentContentIntent(p: Player) = contentIntent

                override fun getCurrentContentText(p: Player) = p.mediaMetadata.artist

                override fun getCurrentLargeIcon(
                    p: Player,
                    cb: PlayerNotificationManager.BitmapCallback
                ) = null
            })
            .setSmallIconResourceId(R.drawable.ic_music)
            .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationPosted(id: Int, n: Notification, ongoing: Boolean) {
                    if (ongoing) startForeground(id, n)
                    else stopForeground(false)
                }

                override fun onNotificationCancelled(id: Int, dismissedByUser: Boolean) {
                    stopForeground(true)
                    stopSelf()
                }
            })
            .build().apply {
                setPlayer(player)
            }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    override fun onDestroy() {
        notificationManager?.setPlayer(null)
        sessionCompat.release()
        session.release()
        player.release()
        super.onDestroy()
    }

    fun setQueue(items: List<MediaItem>) {
        player.setMediaItems(items)
        player.prepare()
    }
}
