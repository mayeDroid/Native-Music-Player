package com.example.myaudioplayer.ui.audio

import android.support.v4.media.MediaBrowserCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myaudioplayer.audioplayer.data.model.Audio
import com.example.myaudioplayer.audioplayer.data.repository.AudioRepository
import com.example.myaudioplayer.audioplayer.media.constants.K
import com.example.myaudioplayer.audioplayer.media.exoplayer.MediaPlayerServiceConnection
import com.example.myaudioplayer.audioplayer.media.exoplayer.currentPosition
import com.example.myaudioplayer.audioplayer.media.exoplayer.isPlaying
import com.example.myaudioplayer.audioplayer.media.service.MediaPlayerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioViewModel @Inject constructor(
    private val repository: AudioRepository,
    serviceConnection: MediaPlayerServiceConnection
) : ViewModel() {

    val audioList = mutableStateListOf<Audio>()

    val currentPlayingAudio = serviceConnection.currentPlayingAudio

    private val isConnected = serviceConnection.isConnected

    lateinit var rootMediaId: String

    var currentPlayBackPosition by mutableStateOf(0L)

    private var upDatePosition = true

    private val playBackState = serviceConnection.playbackState

    val isAudioPlaying: Boolean
        get() = playBackState.value?.isPlaying == true

    private var subscriptionCallBack = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(
            parentId: String,
            children: MutableList<MediaBrowserCompat.MediaItem>
        ) {
            super.onChildrenLoaded(parentId, children)
        }
    }

    private val serviceConnection = serviceConnection.also {
        updatePlayBack()
    }

    val currentDuration:Long
        get() = MediaPlayerService.currentDuration

    var currentAudioProgress = mutableStateOf(0f)

    init {
        viewModelScope.launch {
            audioList += getAndFormatAudioData()
            isConnected.collect {
                if (it) {
                    rootMediaId = serviceConnection.rootMediaId
                    serviceConnection.playbackState.value?.apply {
                        currentPlayBackPosition = position
                    }
                    serviceConnection.subscribe(
                        rootMediaId,
                        subscriptionCallBack
                    )  //without the subscription here it wouldn't play anything

                }
            }
        }
    }

    private suspend fun getAndFormatAudioData(): List<Audio> {
        return repository.getAudioData().map {
            val displayName = it.displayName.substringBefore(".")
            val artist = if (it.artist.contains("<unknown>"))
                "Unknown artist" else it.artist
            it.copy(
                displayName = displayName,
                artist = artist
            )
        }
    }

    fun playAudio(currentAudio: Audio) {
        serviceConnection.playAudio(audioList)
        if (currentAudio.id == currentPlayingAudio.value?.id) {
            if (isAudioPlaying) {
                serviceConnection.transportControl.pause()
            } else {
                serviceConnection.transportControl.play()
            }
        } else {
            serviceConnection.transportControl.playFromMediaId(currentAudio.id.toString(), null)
        }
    }

    fun stopPlayBack() {
        serviceConnection.transportControl.stop()
    }

    fun fastForward() {
        serviceConnection.fastForward()
    }

    fun rewind() {
        serviceConnection.rewind()
    }

    fun skipToNext() {
        serviceConnection.skipToNext()
    }

    fun seekTo(value: Float) {
        serviceConnection.transportControl.seekTo(
            (currentDuration * value / 100f).toLong()
        )
    }

    private fun updatePlayBack() {
        viewModelScope.launch {
            val position = playBackState.value?.currentPosition ?: 0

            if (currentPlayBackPosition != position) {
                currentPlayBackPosition = position

            }
            if (currentDuration > 0) {
                currentAudioProgress.value = (
                        currentPlayBackPosition.toFloat() / currentDuration.toFloat() * 100f
                        )
            }

            delay(K.PLAYBACK_UPDATE_INTERVAL)
            if (upDatePosition) {
                updatePlayBack()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        serviceConnection.unSubscribe(
            K.MEDIA_ROOT_ID,
            object : MediaBrowserCompat.SubscriptionCallback() {}
        )
        upDatePosition = false
    }


}