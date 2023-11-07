package com.example.myaudioplayer.audioplayer.media.exoplayer

import android.support.v4.media.session.PlaybackStateCompat

inline val PlaybackStateCompat.isPlaying: Boolean
    get() = state == PlaybackStateCompat.STATE_BUFFERING ||
            state == PlaybackStateCompat.STATE_PLAYING

inline val PlaybackStateCompat.currentPosition: Long
    get() = if (
        state == PlaybackStateCompat.STATE_PLAYING
    ) {
        val timeDeltaOrDifferenceBwPlayingPositions =
            android.os.SystemClock.elapsedRealtime() - lastPositionUpdateTime
        (position + (timeDeltaOrDifferenceBwPlayingPositions * playbackSpeed)).toLong()
    } else position