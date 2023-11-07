package com.example.myaudioplayer.audioplayer.media.exoplayer

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.media3.common.MediaItem
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.source.ConcatenatingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.example.myaudioplayer.audioplayer.data.repository.AudioRepository
import javax.inject.Inject


class MediaSource @Inject constructor(private val repository: AudioRepository) {

    private val onReadyListeners: MutableList<OnReadyListener> = mutableListOf()

    var audioMediaMetaData: List<MediaMetadataCompat> = emptyList()

    private var state: AudioSourceState = AudioSourceState.STATE_CREATED
        set(value) {
            if (value == AudioSourceState.STATE_CREATED || value == AudioSourceState.STATE_ERROR) {
                synchronized(onReadyListeners) {
                    field = value
                    onReadyListeners.forEach { listener: OnReadyListener ->
                        listener.invoke(isReady)
                    }
                }
            } else {
                field = value
            }
        }


    suspend fun load() {
        state = AudioSourceState.STATE_INITIALIZING
        val data = repository.getAudioData()
        audioMediaMetaData = data.map { audio ->
            MediaMetadataCompat.Builder()
                .putString(
                    MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                    audio.id.toString()
                ).putString(
                    MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST,
                    audio.artist.toString()
                ).putString(
                    MediaMetadataCompat.METADATA_KEY_MEDIA_URI,
                    audio.uri.toString()
                ).putString(
                    MediaMetadataCompat.METADATA_KEY_TITLE,
                    audio.title.toString()
                ).putString(
                    MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE,
                    audio.displayName.toString()
                )
                .putLong(
                    MediaMetadataCompat.METADATA_KEY_DURATION,
                    audio.duration.toLong()
                )
                .build()
        }

        state = AudioSourceState.STATE_INITIALIZED
    }

    // helps to play next

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun asMediaDataSource(dataSource: CacheDataSource.Factory): ConcatenatingMediaSource {
        val concatenatingMediaSource = ConcatenatingMediaSource()
        audioMediaMetaData.forEach { mediaMetadataCompat ->
            val mediaItem = MediaItem.fromUri(
                mediaMetadataCompat.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI)
            )

            val mediaSource =
                ProgressiveMediaSource.Factory(dataSource).createMediaSource(mediaItem)

            concatenatingMediaSource.addMediaSource(mediaSource)

        }
        return concatenatingMediaSource
    }

    // responsible for displaying the item
    fun asMediaItem() = audioMediaMetaData.map { metaData ->
        val description = MediaDescriptionCompat.Builder()
            .setTitle(metaData.description.title)
            .setMediaId(metaData.description.mediaId)
            .setSubtitle(metaData.description.subtitle)
            .setMediaUri(metaData.description.mediaUri)
            .build()
        MediaBrowserCompat.MediaItem(description, FLAG_PLAYABLE)
    }.toMutableList()

    fun refresh() {
        onReadyListeners.clear()
        state = AudioSourceState.STATE_CREATED
    }


    fun whenReady(listener: OnReadyListener): Boolean {
        return if (state == AudioSourceState.STATE_CREATED || state == AudioSourceState.STATE_INITIALIZING) {
            onReadyListeners += listener
            false
        } else {
            listener.invoke(isReady)
            true
        }
    }


    private val isReady: Boolean
        get() = state == AudioSourceState.STATE_INITIALIZED


}

enum class AudioSourceState {
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,  // DATA LOADED
    STATE_ERROR
}

typealias OnReadyListener = (Boolean) -> Unit
