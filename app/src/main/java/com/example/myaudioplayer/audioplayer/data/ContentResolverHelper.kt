package com.example.myaudioplayer.audioplayer.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.WorkerThread
import com.example.myaudioplayer.audioplayer.data.model.Audio
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

// help get the music locally using dagger hilt
class ContentResolverHelper @Inject constructor(@ApplicationContext val context: Context) {

    private var mCusor: Cursor? = null      // use this to get the data from the database

    // use this to get the columns we want
    private val projection: Array<String> = arrayOf(
        MediaStore.Audio.AudioColumns.DISPLAY_NAME,
        MediaStore.Audio.AudioColumns._ID,
        MediaStore.Audio.AudioColumns.ARTIST,
        MediaStore.Audio.AudioColumns.DATA,
        MediaStore.Audio.AudioColumns.DURATION,
        MediaStore.Audio.AudioColumns.TITLE,
    )

    // sort out the data
    private var selectionClause: String? = "${MediaStore.Audio.AudioColumns.IS_MUSIC} = ?"
    private var selectionArg = arrayOf("1")
    private val sortOrder = "${MediaStore.Audio.AudioColumns.DISPLAY_NAME} ASC" //ASC = accending

    @WorkerThread
    fun getAudioData(): List<Audio> {
        return getCursorData()
    }


    private fun getCursorData(): MutableList<Audio> {
        val audioList = mutableListOf<Audio>()
        mCusor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selectionClause,
            selectionArg,
            sortOrder
        )

        // use this to get the index
        mCusor?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns._ID)
            val displayNameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DISPLAY_NAME)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATA)
            val durationColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE)


            // here we get the individual rows
            cursor.apply {
                if (count == 0) {
                    Log.e("Cursor", "getCursor: Cursor is empty")
                } else {
                    while (cursor.moveToNext()) {
                        val displayName = getString(displayNameColumn)
                        val id = getLong(idColumn)
                        val artistColumns = getString(artistColumn)
                        val dataColumn = getString(dataColumn)
                        val durationColumn = getInt(durationColumn)
                        val titleColumn = getString(titleColumn)
                        val uri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                        )

                        // here we get the audio list
                        audioList += Audio(
                            uri,
                            displayName,
                            id,
                            artistColumns,
                            dataColumn,
                            durationColumn,
                            titleColumn
                        )
                    }
                }
            }

        }
        return audioList
    }
}