package com.pemwa.m3audio.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pemwa.m3audio.data.local.room.entity.AudioEntity

/**
 * Data Access Object for the [AudioEntity] table.
 *
 * Provides methods to read and write audio metadata in the local Room database.
 */
@Dao
interface AudioDao {
    /**
     * Returns all audio records stored in the database.
     *
     * @return List of [AudioEntity] objects.
     */
    @Query("SELECT * FROM audio")
    suspend fun getAll(): List<AudioEntity>

    /**
     * Inserts a list of audio records into the database.
     * Replaces existing entries if there is a conflict on [AudioEntity.id].
     *
     * @param audioList List of audio metadata to cache.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(audioList: List<AudioEntity>)
}
