package com.devssocial.localodge.utils.helpers

import android.net.Uri
import com.devssocial.localodge.R
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

class ExoPlayerHelper(
    private val playerView: PlayerView,
    onError: (ExoPlaybackException) -> Unit,
    onPlayerBuffer: (Boolean) -> Unit
) {

    private var exoPlayer: ExoPlayer? = null
    private var mediaSource: ProgressiveMediaSource? = null

    private val playerListener = object : Player.EventListener {
        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {}
        override fun onSeekProcessed() {}
        override fun onTracksChanged(
            trackGroups: TrackGroupArray?,
            trackSelections: TrackSelectionArray?
        ) {
        }

        override fun onPlayerError(error: ExoPlaybackException?) {
            error?.let { onError(it) }
        }

        override fun onLoadingChanged(isLoading: Boolean) {}
        override fun onPositionDiscontinuity(reason: Int) {}
        override fun onRepeatModeChanged(repeatMode: Int) {}
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}
        override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {}
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            onPlayerBuffer(playbackState == Player.STATE_BUFFERING)
        }
    }

    fun killPlayer() {
        pausePlayer()
        if (exoPlayer != null) playerView.player = null
    }

    fun initializePlayer(url: String) {
        val trackSelector = DefaultTrackSelector()
        val loadControl = DefaultLoadControl()
        val renderersFactory = DefaultRenderersFactory(playerView.context)

        exoPlayer = ExoPlayerFactory.newSimpleInstance(
            playerView.context,
            renderersFactory,
            trackSelector,
            loadControl
        )
        exoPlayer!!.repeatMode = Player.REPEAT_MODE_ALL
        exoPlayer!!.addListener(playerListener)

        playerView.player = exoPlayer
//        exoPlayer!!.seekTo(currentWindow, playbackPosition)

        val userAgent =
            Util.getUserAgent(playerView.context, playerView.context.getString(R.string.app_name))
        mediaSource = ProgressiveMediaSource
            .Factory(
                DefaultDataSourceFactory(playerView.context, userAgent),
                DefaultExtractorsFactory()
            )
            .createMediaSource(Uri.parse(url))

        exoPlayer!!.prepare(mediaSource, true, false)
        exoPlayer!!.playWhenReady = true
    }

    private fun pausePlayer() {
        if (exoPlayer != null) {
            exoPlayer!!.release()
            exoPlayer = null
            mediaSource = null
        }
    }
}