package com.example.myaudioplayer.audioplayer.media.exoplayer

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media3.common.Player
import androidx.media3.ui.PlayerNotificationManager
import com.example.myaudioplayer.R
import com.example.myaudioplayer.audioplayer.media.constants.K

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class MediaPlayerNotificationManager(
    context: Context,
    sessionTaken: MediaSessionCompat.Token,
    notificationListener: PlayerNotificationManager.NotificationListener

) {

    private val notificationManager: PlayerNotificationManager

    init {
        val mediaController = MediaControllerCompat(context, sessionTaken)

        val builder = PlayerNotificationManager.Builder(
            context,
            K.PLAYBACK_NOTIFICATION_ID,
            K.PLAYBACK_NOTIFICATION_CHANNEL_ID
        )

        with(builder) {
            setMediaDescriptionAdapter(DescriptionAdapter((mediaController)))
            setNotificationListener(notificationListener)
            setChannelNameResourceId(com.google.android.gms.cast.framework.R.string.media_notification_channel_name)
            setChannelDescriptionResourceId(com.google.android.tv.ads.R.string.abc_action_bar_home_description)

        }

        notificationManager = builder.build()

        with(notificationManager){
            setMediaSessionToken(sessionTaken)
            setSmallIcon(R.drawable.baseline_music_note_24)
            setUseRewindAction(false)
            setUseFastForwardAction(false)
        }

    }

    fun hideNotification (){
        notificationManager.setPlayer(null)
    }

    fun showNotification(player: Player){
        notificationManager.setPlayer(player)
    }


    inner class DescriptionAdapter(
        private val controller: MediaControllerCompat
    ): PlayerNotificationManager.MediaDescriptionAdapter {

        override fun getCurrentContentTitle(player: Player): CharSequence =
            controller.metadata.description.mediaId.toString()

        override fun createCurrentContentIntent(player: Player): PendingIntent? =
            controller.sessionActivity

        override fun getCurrentContentText(player: Player): CharSequence? =
            controller.metadata.description.subtitle

        // need an icon to pass here but we use null
        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? {
            return null
        }
    }
}