package com.pemwa.m3audio.data.repository

import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import com.pemwa.m3audio.data.local.ContentResolverHelper
import com.pemwa.m3audio.data.local.model.Audio
import com.pemwa.m3audio.data.local.room.dao.AudioDao
import com.pemwa.m3audio.data.local.room.entity.AudioEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import androidx.core.net.toUri

class AudioRepository @Inject constructor(
    private val contentResolverHelper: ContentResolverHelper,
    private val audioDao: AudioDao
) {
    suspend fun getAudioData(): List<Audio> = withContext(Dispatchers.IO) {
        val cachedAudios = audioDao.getAll()
        if (cachedAudios.isNotEmpty()) {
            return@withContext cachedAudios
                .map { it.toAudio() }
                .sortedBy { it.displayName }
        } else {
            val audioList = contentResolverHelper.getAudioData()
            audioDao.insertAll(audioList.map { it.toAudioEntity() })
            return@withContext audioList
        }
    }

    /**
     * Converts a Room [AudioEntity] into a domain [Audio] model.
     */
    private fun AudioEntity.toAudio() = Audio(
        uri = uri.toUri(),
        displayName = displayName
            .replace("_", " ")
            .replace("-", " ")
            .replace(".mp3", "")
            .capitalize(Locale.current),
        id = id,
        artist = artist
            .replace("<", "")
            .replace(">", "")
            .capitalize(Locale.current),
        data = data,
        duration = duration,
        title = title
            .replace("_", " ")
            .replace("-", " ")
            .replace(".mp3", "")
            .capitalize(Locale.current),
    )

    /**
     * Converts a domain [Audio] model into a Room [AudioEntity] for persistence.
     */
    private fun Audio.toAudioEntity() = AudioEntity(
        id = id,
        uri = uri.toString(),
        displayName = displayName,
        artist = artist,
        data = data,
        duration = duration,
        title = title
    )

}
