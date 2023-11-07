package com.example.myaudioplayer.ui.audio

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.myaudioplayer.audioplayer.data.model.Audio
import com.example.myaudioplayer.audioplayer.data.repository.AudioRepository
import com.example.myaudioplayer.audioplayer.media.exoplayer.MediaPlayerServiceConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AudioViewModel @Inject constructor(
    private val repository: AudioRepository,
    serviceConnection: MediaPlayerServiceConnection
): ViewModel() {
    var audioList = mutableStateListOf<Audio>()

}