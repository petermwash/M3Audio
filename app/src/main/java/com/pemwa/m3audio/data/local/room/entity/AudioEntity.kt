package com.pemwa.m3audio.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single audio file stored locally.
 *
 * This entity is used by Room to cache audio metadata retrieved from MediaStore.
 *
 * @property id Unique identifier for the audio file (usually the MediaStore ID).
 * @property uri String version of the audio file URI.
 * @property displayName File name shown to users.
 * @property artist Artist of the audio track.
 * @property data Absolute file path on device storage.
 * @property duration Duration of the audio file in milliseconds.
 * @property title Title metadata of the audio track.
 */
@Entity(tableName = "audio")
data class AudioEntity(
    @PrimaryKey val id: Long,
    val uri: String,
    val displayName: String,
    val artist: String,
    val data: String,
    val duration: Int,
    val title: String
)
