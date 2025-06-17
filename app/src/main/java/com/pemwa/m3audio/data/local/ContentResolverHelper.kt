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

/**
 * Helper class for querying local audio files from the Android MediaStore.
 *
 * This class uses the app's [ContentResolver] to fetch a list of music files stored
 * on the device, returning them as a list of [Audio] data models.
 *
 * @property applicationContext the application context used to access the content resolver.
 */
class ContentResolverHelper @Inject
constructor(@ApplicationContext val applicationContext: Context) {
    private var mCursor: Cursor? = null

    /**
     * Projection specifies the columns to retrieve from the MediaStore.
     */
    private val projection: Array<String> = arrayOf(
        MediaStore.Audio.AudioColumns.DISPLAY_NAME,
        MediaStore.Audio.AudioColumns._ID,
        MediaStore.Audio.AudioColumns.ARTIST,
        MediaStore.Audio.AudioColumns.DATA,
        MediaStore.Audio.AudioColumns.DURATION,
        MediaStore.Audio.AudioColumns.TITLE,
    )

    /**
     * Selection clause to only include music files.
     */
    private var selectionClause: String? =
        "${MediaStore.Audio.AudioColumns.IS_MUSIC} = ?"
    /**
     * Arguments for the selection clause.
     */
    private var selectionArg = arrayOf("1")

    /**
     * Sort order for the query results (alphabetical by display name).
     */
    private val sortOrder = "${MediaStore.Audio.AudioColumns.DISPLAY_NAME} ASC"

    /**
     * Returns a list of audio files from the device's external storage.
     *
     * This is called on a background thread due to the use of a content resolver query.
     *
     * @return a list of [Audio] objects representing music files.
     */
    @WorkerThread
    fun getAudioData(): List<Audio> {
        return getCursorData()
    }

    /**
     * Performs the actual query using the [ContentResolver] and builds a list of [Audio] items.
     *
     * @return a mutable list of [Audio] items.
     */
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

    /**
     * Extension function to convert a [Cursor] row to an [Audio] object.
     *
     * @receiver the current cursor row.
     * @return an [Audio] object populated with metadata from the current row.
     */
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
