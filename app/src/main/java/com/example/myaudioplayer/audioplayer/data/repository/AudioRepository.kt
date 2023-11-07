package com.example.myaudioplayer.audioplayer.data.repository

import android.content.ContentResolver
import com.example.myaudioplayer.audioplayer.data.ContentResolverHelper
import com.example.myaudioplayer.audioplayer.data.model.Audio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

//Helps to get Audio data
class AudioRepository @Inject
constructor(private val contentResolver: ContentResolverHelper){
    suspend fun getAudioData(): List<Audio> = withContext(Dispatchers.IO){
        contentResolver.getAudioData()
    }
}