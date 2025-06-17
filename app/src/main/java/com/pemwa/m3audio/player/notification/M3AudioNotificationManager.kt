package com.pemwa.m3audio.player.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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

/**
 * A helper class responsible for creating and managing the playback notification
 * displayed during media playback. This notification enables playback controls and
 * allows the service to run in the foreground.
 *
 * It integrates with ExoPlayer and Media3's PlayerNotificationManager to show
 * media metadata and control buttons (play, pause, next, rewind, etc.) in the system notification area.
 *
 * @property applicationContext The application-level context injected by Hilt.
 * @property exoPlayer The [ExoPlayer] instance used for media playback.
 */
@UnstableApi
class M3AudioNotificationManager @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val exoPlayer: ExoPlayer,
) {
    private val notificationManager: NotificationManagerCompat =
        NotificationManagerCompat.from(applicationContext)
    private var playerNotificationManager: PlayerNotificationManager? = null

    init {
        createNotificationChannel()
    }

    /**
     * Initializes the notification system and starts the service in the foreground with a
     * blank notification. Once the [PlayerNotificationManager] is built, it automatically
     * updates the system notification with metadata and controls.
     *
     * @param mediaSessionService The [MediaSessionService] that is being run in the foreground.
     * @param mediaSession The active [MediaSession] used to link the notification to the media session.
     */
    @UnstableApi
    fun startNotificationService(
        mediaSessionService: MediaSessionService,
        mediaSession: MediaSession,
    ) {
        buildNotification(mediaSession)
        startForeGroundNotificationService(mediaSessionService)
    }

    /**
     * Starts the given [mediaSessionService] as a foreground service with a blank notification.
     * The real notification content is updated shortly after by the [PlayerNotificationManager].
     *
     * @param mediaSessionService The service to be promoted to foreground.
     */
    private fun startForeGroundNotificationService(mediaSessionService: MediaSessionService) {
        val notification =
            Notification.Builder(applicationContext, PLAYBACK_NOTIFICATION_CHANNEL_ID)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build()
        mediaSessionService.startForeground(PLAYBACK_NOTIFICATION_ID, notification)
    }

    /**
     * Builds the [PlayerNotificationManager] which handles showing the current media item,
     * artwork, and playback controls in the notification drawer.
     *
     * @param mediaSession The active [MediaSession] used for session control and media metadata.
     */
    @UnstableApi
    private fun buildNotification(mediaSession: MediaSession) {
        PlayerNotificationManager.Builder(
            applicationContext,
            PLAYBACK_NOTIFICATION_ID,
            PLAYBACK_NOTIFICATION_CHANNEL_ID
        )
            .setMediaDescriptionAdapter(
                M3AudioNotificationAdapter(
                    context = applicationContext,
                    pendingIntent = mediaSession.sessionActivity
                )
            )
            .setSmallIconResourceId(R.mipmap.ic_launcher)
            .build()
            .also {
                it.setUseFastForwardActionInCompactView(true)
                it.setUseRewindActionInCompactView(true)
                it.setUseNextActionInCompactView(true)
                it.setPriority(NotificationCompat.PRIORITY_LOW)
                it.setPlayer(exoPlayer)
                playerNotificationManager = it
            }
    }

    /**
     * Creates the notification channel used by the playback notification.
     * This is required for Android 8.0+ (API 26+).
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            PLAYBACK_NOTIFICATION_CHANNEL_ID,
            PLAYBACK_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Stops and releases the PlayerNotificationManager, removing the media playback notification.
     * Called when playback is stopped or service is destroyed.
     */
    fun stopNotification() {
        playerNotificationManager?.setPlayer(null)
        playerNotificationManager = null
    }
}
