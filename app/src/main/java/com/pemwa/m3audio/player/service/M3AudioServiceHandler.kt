package com.pemwa.m3audio.player.service

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * For managing audio playback using ExoPlayer
 *
 * It handles Playback events, state management and progress updates
 */
class M3AudioServiceHandler @Inject constructor(
    private val exoPlayer: ExoPlayer
) : Player.Listener, CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext = Dispatchers.IO + job

    private val _audioState: MutableStateFlow<M3AudioState> = MutableStateFlow(M3AudioState.Initial)
    val audioState: StateFlow<M3AudioState>
        get() = _audioState.asStateFlow()

    private var progressJob: Job? = null

    fun addMediaItem(mediaItem: MediaItem) {
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
    }

    fun addMediaItemList(mediaItems: List<MediaItem>) {
        exoPlayer.setMediaItems(mediaItems)
        exoPlayer.prepare()
    }

    private suspend fun playOrPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            stopProgressUpdate()
        } else {
            exoPlayer.play()
            _audioState.value = M3AudioState.Playing(true)
            startProgressUpdate()
        }
    }

    private suspend fun startProgressUpdate() = progressJob.run {
        while (true) {
            delay(500)
            _audioState.value = M3AudioState.Progress(exoPlayer.currentPosition)
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        _audioState.value = M3AudioState.Playing(false)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            ExoPlayer.STATE_BUFFERING -> _audioState.value =
                M3AudioState.Buffering(exoPlayer.currentPosition)

            Player.STATE_ENDED -> {
                stopProgressUpdate()
                // Handle completion logic if needed
            }

            Player.STATE_IDLE -> {
                stopProgressUpdate()
                // Handle idle logic if needed
            }

            Player.STATE_READY -> _audioState.value = M3AudioState.Ready(exoPlayer.duration)
        }
        super.onPlaybackStateChanged(playbackState)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _audioState.value = M3AudioState.Playing(true)
        _audioState.value = M3AudioState.CurrentPlaying(exoPlayer.currentMediaItemIndex)

        if (isPlaying) {
            launch(Dispatchers.IO) {
                startProgressUpdate()
            }
        } else {
            stopProgressUpdate()
        }
    }

    suspend fun onPlayerEvents(
        playerEvent: PlayerEvent,
        selectedAudioIndex: Int = -1,
        seekPosition: Long = 0,
    ) {
        when (playerEvent) {
            PlayerEvent.Backward -> exoPlayer.seekBack()
            PlayerEvent.Forward -> exoPlayer.seekForward()
            PlayerEvent.PlayPause -> playOrPause()
            PlayerEvent.SeekTo -> exoPlayer.seekTo(seekPosition)
            PlayerEvent.SeekToNext -> exoPlayer.seekToNext()
            PlayerEvent.SelectedAudioChange -> {
                when (selectedAudioIndex) {
                    exoPlayer.currentMediaItemIndex -> playOrPause()
                    else -> {
                        exoPlayer.seekToDefaultPosition(selectedAudioIndex)
                        _audioState.value = M3AudioState.Playing(true)
                        exoPlayer.playWhenReady = true
                        startProgressUpdate()
                    }
                }
            }

            PlayerEvent.Stop -> stopProgressUpdate()
            is PlayerEvent.UpdateProgress ->
                exoPlayer.seekTo(
                    (exoPlayer.duration * playerEvent.newProgress).toLong()
                )
        }
    }

    fun release() {
        stopProgressUpdate()
        job.cancel() // Cancel the whole scope to release resources
    }

}

sealed class PlayerEvent {
    data object PlayPause : PlayerEvent()
    data object SelectedAudioChange : PlayerEvent()
    data object Backward : PlayerEvent()
    data object SeekToNext : PlayerEvent()
    data object Forward : PlayerEvent()
    data object SeekTo : PlayerEvent()
    data object Stop : PlayerEvent()
    data class UpdateProgress(val newProgress: Float) : PlayerEvent()
}

sealed class M3AudioState {
    data object Initial : M3AudioState()
    data class Ready(val duration: Long) : M3AudioState()
    data class Progress(val progress: Long) : M3AudioState()
    data class Buffering(val progress: Long) : M3AudioState()
    data class Playing(val isPlaying: Boolean) : M3AudioState()
    data class CurrentPlaying(val mediaItemIndex: Int) : M3AudioState()
}