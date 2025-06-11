package com.pemwa.m3audio.player.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import com.pemwa.m3audio.R
import com.pemwa.m3audio.constants.Constants.PLAYBACK_NOTIFICATION_CHANNEL_ID
import com.pemwa.m3audio.constants.Constants.PLAYBACK_NOTIFICATION_CHANNEL_NAME
import com.pemwa.m3audio.constants.Constants.PLAYBACK_NOTIFICATION_ID
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class M3AudioNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exoPlayer: ExoPlayer,
) {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }

    private val notificationManager: NotificationManagerCompat =
        NotificationManagerCompat.from(context)

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationChannel = NotificationChannel(
            PLAYBACK_NOTIFICATION_CHANNEL_ID,
            PLAYBACK_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )

        notificationManager.createNotificationChannel(notificationChannel)
    }

    @OptIn(UnstableApi::class)
    private fun buildNotification(mediaSession: MediaSession) {
        PlayerNotificationManager.Builder(
            context,
            PLAYBACK_NOTIFICATION_ID,
            PLAYBACK_NOTIFICATION_CHANNEL_ID
        ).setMediaDescriptionAdapter(M3AudioNotificationAdapter(
            context= context,
            pendingIntent = mediaSession.sessionActivity
        ))
            .setSmallIconResourceId(R.drawable.ic_microphone)
            .build()
            .also {
                it.setMediaSessionToken(mediaSession.platformToken)
                it.setUseFastForwardActionInCompactView(true)
                it.setUseRewindActionInCompactView(true)
                it.setUseNextActionInCompactView(true)
                it.setPriority(NotificationCompat.PRIORITY_LOW)
                it.setPlayer(exoPlayer)
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startForeGroundNotificationService(
        mediaSessionService: MediaSessionService
    ) {
        val notification = Notification.Builder(
            context,
            PLAYBACK_NOTIFICATION_CHANNEL_ID
        )
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()

        mediaSessionService.startForeground(PLAYBACK_NOTIFICATION_ID, notification)

    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startNotificationService(
        mediaSessionService: MediaSessionService,
        mediaSession: MediaSession
    ) {
        buildNotification(mediaSession = mediaSession)
        startForeGroundNotificationService(mediaSessionService)
    }
}
