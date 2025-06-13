package com.pemwa.m3audio.data.local

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.WorkerThread
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

import com.pemwa.m3audio.data.local.model.Audio

class ContentResolverHelper @Inject
constructor(@ApplicationContext val applicationContext: Context) {
    private var mCursor: Cursor? = null

    private val projection: Array<String> = arrayOf(
        MediaStore.Audio.AudioColumns.DISPLAY_NAME,
        MediaStore.Audio.AudioColumns._ID,
        MediaStore.Audio.AudioColumns.ARTIST,
        MediaStore.Audio.AudioColumns.DATA,
        MediaStore.Audio.AudioColumns.DURATION,
        MediaStore.Audio.AudioColumns.TITLE,
    )

    private var selectionClause: String? =
        "${MediaStore.Audio.AudioColumns.IS_MUSIC} = ?"
    private var selectionArg = arrayOf("1")

    private val sortOrder = "${MediaStore.Audio.AudioColumns.DISPLAY_NAME} ASC"

    @WorkerThread
    fun getAudioData(): List<Audio> {
        return getCursorData()
    }

    private fun getCursorData(): MutableList<Audio> {
        val audioList = mutableListOf<Audio>()

        mCursor = applicationContext.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selectionClause,
            selectionArg,
            sortOrder
        )

        mCursor?.use { cursor ->
            if (cursor.count == 0) {
                Log.d("Cursor", "No music files found.")
            } else {
                while (cursor.moveToNext()) {
                    audioList += cursor.toAudio()
                }
            }
        }

        return audioList
    }

    private fun Cursor.toAudio(): Audio {
        val id = getLong(getColumnIndexOrThrow(MediaStore.Audio.AudioColumns._ID))
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

        return Audio(
            uri = uri,
            displayName = getString(getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DISPLAY_NAME)),
            id = id,
            artist = getString(getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST)),
            data = getString(getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATA)),
            duration = getInt(getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION)),
            title = getString(getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE))
        )
    }

}
