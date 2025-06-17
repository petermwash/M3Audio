package com.pemwa.m3audio.player.notification

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerNotificationManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.pemwa.m3audio.R

/**
 * Adapter class that provides media metadata to be displayed in the system media notification.
 * This is used by [PlayerNotificationManager] to show the current audio title, subtitle, and album artwork.
 *
 * @param context The application context used for loading images.
 * @param pendingIntent A PendingIntent that will be triggered when the user taps the notification.
 */
@UnstableApi
class M3AudioNotificationAdapter(
    private val context: Context,
    private val pendingIntent: PendingIntent?
) : PlayerNotificationManager.MediaDescriptionAdapter {

    /**
     * Provides the title shown in the notification (usually the album title or track name).
     * Fallbacks to "Unknown" if metadata is missing.
     */
    override fun getCurrentContentTitle(player: Player): CharSequence =
        player.mediaMetadata.albumTitle ?: "Unknown"

    /**
     * Provides the PendingIntent triggered when the user taps the notification.
     * This typically opens the app or resumes the UI.
     */
    override fun createCurrentContentIntent(player: Player): PendingIntent? =
        pendingIntent

    /**
     * Provides the subtitle or description shown below the title in the notification.
     * Fallbacks to "Unknown" if not available.
     */
    override fun getCurrentContentText(player: Player): CharSequence =
        player.mediaMetadata.displayTitle ?: "Unknown"

    /**
     * Loads the album artwork asynchronously and notifies the notification manager with the result.
     * This ensures album art appears in the notification once it's ready.
     */
    override fun getCurrentLargeIcon(
        player: Player,
        callback: PlayerNotificationManager.BitmapCallback
    ): Bitmap? {
        val artworkUri = player.mediaMetadata.artworkUri

        Glide.with(context)
            .asBitmap()
            .load(artworkUri ?: R.mipmap.ic_launcher)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .error(R.mipmap.ic_launcher)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    callback.onBitmap(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) = Unit
            })

        return null
    }
}
