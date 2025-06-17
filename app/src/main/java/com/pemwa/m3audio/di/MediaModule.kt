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
    @Provides
    @Singleton
    fun provideAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .setUsage(C.USAGE_MEDIA)
        .build()

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

    @Provides
    @Singleton
    fun provideMediaSession(
        @ApplicationContext context: Context,
        player: ExoPlayer
    ): MediaSession = MediaSession.Builder(context, player).build()

    @Provides
    @Singleton
    fun provideNotificationManager(
        @ApplicationContext context: Context,
        player: ExoPlayer
    ): M3AudioNotificationManager = M3AudioNotificationManager(
        applicationContext = context,
        exoPlayer = player
    )

    @Provides
    @Singleton
    fun provideServiceHandler(exoPlayer: ExoPlayer, playbackPreferences: M3AudioPreferences): M3AudioServiceHandler =
        M3AudioServiceHandler(exoPlayer = exoPlayer, playbackPreferences = playbackPreferences)

    /**
     * Provides the singleton Room database instance using the app context.
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): AppDatabase =
        Room.databaseBuilder(appContext, AppDatabase::class.java, "audio_db").build()

    /**
     * Provides the AudioDao instance from the database.
     */
    @Provides
    @Singleton
    fun provideAudioDao(db: AppDatabase): AudioDao = db.audioDao()

    /**
     * Provides the SharedPreferences instance using the app context.
     */
    @Provides
    @Singleton
    fun provideM3AudioPreferences(
        @ApplicationContext context: Context
    ): M3AudioPreferences = M3AudioPreferences(context)
}
