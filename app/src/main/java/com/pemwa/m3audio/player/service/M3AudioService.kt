package com.pemwa.m3audio.player.service

import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.pemwa.m3audio.player.notification.M3AudioNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground Media Playback Service using Media3's MediaSessionService.
 *
 * This service is responsible for:
 * - Creating and exposing the [MediaSession] to the system and controllers.
 * - Managing foreground media playback with a persistent notification.
 * - Handling lifecycle events (start, stop, destroy) for clean player and session management.
 *
 * It is annotated with `@AndroidEntryPoint` to enable Hilt dependency injection for:
 * - [mediaSession] – Provides the session to be exposed.
 * - [notificationManager] – Manages the player notification UI.
 */
@UnstableApi
@AndroidEntryPoint
class M3AudioService : MediaSessionService() {
    @Inject
    lateinit var mediaSession: MediaSession

    @Inject
    lateinit var notificationManager: M3AudioNotificationManager

    /**
     * Called when the service is started.
     * Initializes the player notification and starts it in the foreground.
     */
    @OptIn(UnstableApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationManager.startNotificationService(
            mediaSessionService = this,
            mediaSession = mediaSession
        )
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * Provides the [MediaSession] instance to system components like the media controller.
     */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession =
        mediaSession

    /**
     * Called when the service is destroyed.
     * Cleans up by stopping the notification, releasing the media session and resetting the player.
     */
    override fun onDestroy() {
        // Stop and remove media notification
        notificationManager.stopNotification()

        // Release session and stop player properly
        mediaSession.apply {
            release()

            if (player.playbackState != Player.STATE_IDLE) {
                player.seekTo(0)
                player.playWhenReady = false
                player.stop()
            }
        }

        super.onDestroy()

    }
}
