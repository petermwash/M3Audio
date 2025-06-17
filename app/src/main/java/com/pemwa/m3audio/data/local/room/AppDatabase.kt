package com.pemwa.m3audio.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import com.pemwa.m3audio.data.local.room.dao.AudioDao
import com.pemwa.m3audio.data.local.room.entity.AudioEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Main Room database for the M3Audio app.
 *
 * Holds the [AudioEntity] table and exposes the [AudioDao] for interaction.
 */
@Database(entities = [AudioEntity::class], version = 1)
abstract class AppDatabase: RoomDatabase() {
    /**
     * Provides access to audio-related database operations.
     */
    abstract fun audioDao(): AudioDao
}
