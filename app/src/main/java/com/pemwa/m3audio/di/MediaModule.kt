package com.pemwa.m3audio.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import androidx.room.Room
import com.pemwa.m3audio.data.local.M3AudioPreferences
import com.pemwa.m3audio.data.local.room.AppDatabase
import com.pemwa.m3audio.data.local.room.dao.AudioDao
import com.pemwa.m3audio.player.notification.M3AudioNotificationManager
import com.pemwa.m3audio.player.service.M3AudioServiceHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    /**
     * Provides configured [AudioAttributes] for media playback.
     * Sets the content type and usage for handling audio focus and routing.
     */
    @Provides
    @Singleton
    fun provideAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .setUsage(C.USAGE_MEDIA)
        .build()

    /**
     * Provides a singleton [ExoPlayer] instance.
     * It applies audio attributes, handles audio focus and noisy transitions (e.g. unplugging headphones),
     * and uses the default track selector.
     *
     * @param context Application context
     * @param audioAttributes Custom audio attributes provided for media playback
     */
    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        audioAttributes: AudioAttributes
    ): ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(audioAttributes, true)
        .setHandleAudioBecomingNoisy(true)
        .setTrackSelector(DefaultTrackSelector(context))
        .build()

    /**
     * Provides a [MediaSession] that links the system media controls with the ExoPlayer instance.
     *
     * @param context Application context
     * @param player The ExoPlayer to attach to the session
     */
    @Provides
    @Singleton
    fun provideMediaSession(
        @ApplicationContext context: Context,
        player: ExoPlayer
    ): MediaSession = MediaSession.Builder(context, player).build()

    /**
     * Provides a [M3AudioNotificationManager] that handles the media playback notification UI.
     *
     * @param context Application context
     * @param player ExoPlayer instance to track playback changes
     */
    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideNotificationManager(
        @ApplicationContext context: Context,
        player: ExoPlayer
    ): M3AudioNotificationManager = M3AudioNotificationManager(
        applicationContext = context,
        exoPlayer = player
    )

    /**
     * Provides the core service handler [M3AudioServiceHandler] for controlling playback,
     * observing player state, and managing progress updates.
     *
     * @param exoPlayer The ExoPlayer instance
     * @param playbackPreferences SharedPreferences wrapper for persisting playback state
     */
    @Provides
    @Singleton
    fun provideServiceHandler(exoPlayer: ExoPlayer, playbackPreferences: M3AudioPreferences): M3AudioServiceHandler =
        M3AudioServiceHandler(exoPlayer = exoPlayer, playbackPreferences = playbackPreferences)

    /**
     * Provides the singleton Room [AppDatabase] instance.
     *
     * @param appContext Application context
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): AppDatabase =
        Room.databaseBuilder(appContext, AppDatabase::class.java, "audio_db").build()

    /**
     * Provides the [AudioDao] to access audio metadata stored in the local database.
     *
     * @param db The Room database instance
     */
    @Provides
    @Singleton
    fun provideAudioDao(db: AppDatabase): AudioDao = db.audioDao()

    /**
     * Provides the [M3AudioPreferences] instance to persist playback state like
     * current audio index, progress, and play state.
     *
     * @param context Application context
     */
    @Provides
    @Singleton
    fun provideM3AudioPreferences(
        @ApplicationContext context: Context
    ): M3AudioPreferences = M3AudioPreferences(context)
}
