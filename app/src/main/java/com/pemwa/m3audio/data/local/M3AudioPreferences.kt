package com.pemwa.m3audio.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class M3AudioPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("m3_audio_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AUDIO_INDEX = "audio_index"
        private const val KEY_AUDIO_PROGRESS = "audio_progress"
        private const val KEY_AUDIO_IS_PLAYING = "audio_is_playing"
    }

    fun savePlaybackState(index: Int, progress: Long, isPlaying: Boolean) {
        prefs.edit {
            putInt(KEY_AUDIO_INDEX, index)
                .putLong(KEY_AUDIO_PROGRESS, progress)
                .putBoolean(KEY_AUDIO_IS_PLAYING, isPlaying)
        }
    }

    fun getSavedAudioIndex(): Int = prefs.getInt(KEY_AUDIO_INDEX, 0)

    fun getSavedProgress(): Long = prefs.getLong(KEY_AUDIO_PROGRESS, 0L)

    fun isPlaying(): Boolean = prefs.getBoolean(KEY_AUDIO_IS_PLAYING, false)

    fun clear() {
        prefs.edit { clear() }
    }
}
