package com.example.myaudioplayer.audioplayer.media.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.media.MediaDescription
import android.media.MediaDrm.PlaybackComponent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerNotificationManager
import com.example.myaudioplayer.R
import com.example.myaudioplayer.audioplayer.media.constants.K
import com.example.myaudioplayer.audioplayer.media.exoplayer.MediaPlayerNotificationManager
import com.example.myaudioplayer.audioplayer.media.exoplayer.MediaSource
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import dagger.Provides
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

// responsible for playback and pause from the apps UI
// also responsible for letting other apps playback via the app and control the playback

@AndroidEntryPoint
class MediaPlayerService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var dataSourceFactory: CacheDataSource.Factory

    @Inject
    lateinit var exoPlayer: ExoPlayer

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var mediaSession: MediaSessionCompat

    private lateinit var mediaSessionConnector: MediaSessionConnector

    private lateinit var mediaPlayerNotificationManager: MediaPlayerNotificationManager

    private var currentPlayingMedia: MediaMetadataCompat? = null

    private val isPlayingInitialized = false

    private var isForeGroundService = false

    companion object {
        private const val TAG = "MediaPlayerService"

        var currentDuration: Long = 0L
            private set
    }



    @Inject
   lateinit var mediaSource: MediaSource

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun  onCreate() {
        super.onCreate()

        val sessionActivityIntent = packageManager
            ?.getLeanbackLaunchIntentForPackage(packageName)
            ?.let {
                    sessionIntent ->
                PendingIntent.getActivity(
                    this,
                    0,
                    sessionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                            PendingIntent.FLAG_IMMUTABLE
                )
            }

        mediaSession = MediaSessionCompat(this, TAG).apply {
            setSessionActivity(sessionActivityIntent)
            isActive = true
        }
        sessionToken = mediaSession.sessionToken

        mediaPlayerNotificationManager = MediaPlayerNotificationManager(
            this,
            mediaSession.sessionToken,
            PlayerNotificationListener()

            )
        serviceScope.launch {
            mediaSource.load()
        }

       /* mediaSessionConnector = MediaSessionConnector(mediaSession).apply {
            setPlaybackPreparer(AudioPlaybackPreparer())
            setQueueNavigator(MediaQueNavigator(mediaSession))
            setPlayer(exoPlayer)

        }*/
        mediaPlayerNotificationManager.showNotification(exoPlayer)

    }


    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot(K.MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        when(parentId){
            K.MEDIA_ROOT_ID ->{
                val resultSent = mediaSource.whenReady { isInitialized ->
                    if (isInitialized){
                        result.sendResult(mediaSource.asMediaItem())
                    } else result.sendResult(null)

                }

                if (!resultSent){
                    result.detach()
                }
            }
            else -> Unit
        }
    }

    override fun onCustomAction(action: String, extras: Bundle?, result: Result<Bundle>) {
        super.onCustomAction(action, extras, result)
        when(action){
            K.START_MEDIA_PLAYBACK_ACTION ->
                mediaPlayerNotificationManager.showNotification(exoPlayer)
            K.REFRESH_MEDIA_PLAYBACK_ACTION -> {
                mediaSource.refresh()
                notifyChildrenChanged(K.MEDIA_ROOT_ID)
            }
            else -> Unit
        }

    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        exoPlayer.release()
    }

    @UnstableApi inner class PlayerNotificationListener:
        PlayerNotificationManager.NotificationListener{
        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            stopForeground(true)
            isForeGroundService = false
            stopSelf()
        }

        override fun onNotificationPosted(
            notificationId: Int,
            notification: Notification,
            ongoing: Boolean
        ) {
            if (ongoing && !isForeGroundService){
                ContextCompat.startForegroundService(
                    applicationContext,
                    Intent(
                        applicationContext,
                        this@MediaPlayerService.javaClass
                    )
                )
                startForeground(notificationId, notification)
                isForeGroundService = true
            }
        }
    }

    inner class AudioPlaybackPreparer: MediaSessionConnector.PlaybackPreparer{
        override fun onCommand(
            player: Player,
            command: String,
            extras: Bundle?,
            cb: ResultReceiver?
        ): Boolean {
            return false
        }

        override fun getSupportedPrepareActions(): Long = PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID

        override fun onPrepare(playWhenReady: Boolean)  = Unit

        override fun onPrepareFromMediaId(
            mediaId: String,
            playWhenReady: Boolean,
            extras: Bundle?
        ) {
            mediaSource.whenReady {
                val itemToPlay = mediaSource.audioMediaMetaData.find {
                    it.description.mediaId == mediaId
                }

                currentPlayingMedia = itemToPlay

                preparePlayer(
                    mediaMetaData = mediaSource.audioMediaMetaData,
                    itemToPlay = itemToPlay,
                    playWhenReady = playWhenReady
                )
            }
        }

        override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) = Unit

        override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) = Unit

    }

    inner class MediaQueNavigator(
        mediaSessionCompat: MediaSessionCompat
    ): TimelineQueueNavigator(mediaSessionCompat){
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            if (windowIndex > mediaSource.audioMediaMetaData.size){
                return mediaSource.audioMediaMetaData[windowIndex].description
            }
            return MediaDescriptionCompat.Builder().build()
        }

    }



    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun  preparePlayer(
        mediaMetaData: List<MediaMetadataCompat>,
        itemToPlay: MediaMetadataCompat?,
        playWhenReady: Boolean
    ){
        val indexToPlay = if (currentPlayingMedia == null) 0
        else mediaMetaData.indexOf(itemToPlay)  // makes the player starts playing from the list

        exoPlayer.addListener(PlayerEventListener())
        exoPlayer.setMediaSource(
            mediaSource.asMediaDataSource(dataSourceFactory)
        )
        exoPlayer.prepare()
        exoPlayer.seekTo(indexToPlay, 0)
        exoPlayer.playWhenReady = playWhenReady
    }

    private inner class PlayerEventListener: androidx.media3.common.Player.Listener{

        override fun onPlaybackStateChanged(playbackState: Int) {

            when(playbackState){
                androidx.media3.common.Player.STATE_BUFFERING,
                    androidx.media3.common.Player.STATE_READY -> {
                        mediaPlayerNotificationManager.showNotification(exoPlayer)
                    }
                else -> {
                    mediaPlayerNotificationManager.hideNotification()
                }
            }
        }

        override fun onEvents(
            player: androidx.media3.common.Player,
            events: androidx.media3.common.Player.Events
        ) {
            super.onEvents(player, events)
            currentDuration = player.duration
        }

        override fun onPlayerError(error: PlaybackException) {

            var message = R.string.generic_error

            if (error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND){
                message = R.string.media_not_found
            }
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }
}