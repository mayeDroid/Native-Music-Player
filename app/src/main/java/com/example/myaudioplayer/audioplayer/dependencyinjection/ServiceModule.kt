package com.example.myaudioplayer.audioplayer.dependencyinjection

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource

import androidx.media3.datasource.cache.CacheDataSource

import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import java.io.File

@Module
@InstallIn(ServiceComponent::class)

object ServiceModule {
    @Provides
    @ServiceScoped
    fun provideAudioAttributes(): androidx.media3.common.AudioAttributes =
        androidx.media3.common.AudioAttributes.Builder().setContentType(
            androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC
        ).setUsage(androidx.media3.common.C.USAGE_MEDIA).build()

    //AudioAttributes = AudioAttributes.Builder()

    @Provides
    @ServiceScoped
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        audioAttributes: androidx.media3.common.AudioAttributes

    ): androidx.media3.exoplayer.ExoPlayer =
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(audioAttributes, true)
            setHandleAudioBecomingNoisy(true)       // remove headset playback stops and other audio forecasts like notification handling
        }

    @Provides
    @ServiceScoped
    fun provideDataSourceFactory(
        @ApplicationContext context: Context
    ) = DefaultDataSource.Factory(context)


    @Provides
    @ServiceScoped
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun provideCacheDataSourceFactory(
        @ApplicationContext context: Context,
        dataSource: androidx.media3.datasource.DefaultDataSource.Factory
    ): CacheDataSource.Factory {
        val cacheDirectory = File(context.cacheDir, "media")

        val databaseProvider = StandaloneDatabaseProvider(context)

        val cache = SimpleCache(cacheDirectory, NoOpCacheEvictor(), databaseProvider)
        return CacheDataSource.Factory().apply {
            setCache(cache)
            setUpstreamDataSourceFactory(dataSource)
        }
    }


}