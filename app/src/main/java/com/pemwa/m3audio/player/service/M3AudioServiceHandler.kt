package com.pemwa.m3audio.player.service

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.pemwa.m3audio.data.local.M3AudioPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles audio playback control, player state observation, and progress updates using ExoPlayer.
 *
 * This class is the core logic handler for:
 * - Listening to ExoPlayer events
 * - Persisting and restoring playback state
 * - Managing playback commands via [PlayerEvent]
 * - Emitting player state updates via [M3AudioState]
 * - Updating progress periodically while playing
 *
 * Dependencies:
 * - [ExoPlayer] for media playback
 * - [M3AudioPreferences] to persist current playing index, position and playback state
 */
class M3AudioServiceHandler @Inject constructor(
    private val exoPlayer: ExoPlayer,
    private val playbackPreferences: M3AudioPreferences,
) : Player.Listener, CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext = Dispatchers.IO + job

    private val _audioState: MutableStateFlow<M3AudioState> = MutableStateFlow(M3AudioState.Initial)
    val audioState: StateFlow<M3AudioState>
        get() = _audioState.asStateFlow()

    private var progressJob: Job? = null

    private var currentPlayingIndex: Int = -1

    init {
        exoPlayer.addListener(this)
    }

    /**
     * Triggered when a new media item starts playing.
     * Saves the current index, position, and playback state.
     */
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)
        savePlaybackState(
            currentPlayingIndex,
            exoPlayer.currentPosition,
            exoPlayer.isPlaying
        )
    }

    /**
     * Updates state when play/pause changes.
     * Triggers progress updates if playing.
     */
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        savePlaybackState(
            currentPlayingIndex,
            exoPlayer.currentPosition,
            isPlaying
        )
        _audioState.value = M3AudioState.Playing(true)
        _audioState.value = M3AudioState.CurrentPlaying(exoPlayer.currentMediaItemIndex)

        if (isPlaying) {
            launch(Dispatchers.Main) {
                startProgressUpdate()
            }
        } else {
            stopProgressUpdate()
        }
    }

    /**
     * Adds a list of media items to the player and prepares for playback.
     */
    fun addMediaItemList(mediaItems: List<MediaItem>) {
        exoPlayer.setMediaItems(mediaItems)
        exoPlayer.prepare()
    }

    /**
     * Starts or pauses playback, and manages progress updates accordingly.
     */
    private suspend fun playOrPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            stopProgressUpdate()
        } else {
            exoPlayer.play()
            currentPlayingIndex = exoPlayer.currentMediaItemIndex
            _audioState.value = M3AudioState.Playing(true)
            startProgressUpdate()
        }
    }

    /**
     * Continuously emits playback progress while media is playing.
     */
    private suspend fun startProgressUpdate() = progressJob.run {
        while (true) {
            delay(500)
            _audioState.value = M3AudioState.Progress(exoPlayer.currentPosition)
        }
    }

    /**
     * Stops progress emission and sets state to not playing.
     */
    private fun stopProgressUpdate() {
        progressJob?.cancel()
        _audioState.value = M3AudioState.Playing(false)
    }

    /**
     * Responds to changes in ExoPlayer’s internal playback state.
     */
    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            ExoPlayer.STATE_BUFFERING -> _audioState.value =
                M3AudioState.Buffering(exoPlayer.currentPosition)

            Player.STATE_ENDED -> {
                stopProgressUpdate()
                // Handle completion
            }

            Player.STATE_IDLE -> {
                stopProgressUpdate()
                // Handle idle logic
            }

            Player.STATE_READY -> _audioState.value = M3AudioState.Ready(exoPlayer.duration)
        }
        super.onPlaybackStateChanged(playbackState)
    }

    /**
     * Handles various playback commands received from UI or external triggers.
     */
    suspend fun onPlayerEvents(
        playerEvent: PlayerEvent,
        selectedAudioIndex: Int = currentPlayingIndex,
        seekPosition: Long = 0,
    ) {
        when (playerEvent) {
            PlayerEvent.Backward -> exoPlayer.seekBack()
            PlayerEvent.Forward -> exoPlayer.seekForward()
            PlayerEvent.PlayPause -> playOrPause()
            PlayerEvent.SeekTo -> exoPlayer.seekTo(seekPosition)
            PlayerEvent.SeekToNext -> exoPlayer.seekToNext()
            PlayerEvent.SeekToPrevious -> exoPlayer.seekToPrevious()
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

    /**
     * Persists playback state using SharedPreferences.
     */
    private fun savePlaybackState(currentMediaItemIndex: Int, position: Long, isPlaying: Boolean) {
        playbackPreferences.savePlaybackState(currentMediaItemIndex, position, isPlaying)
    }


    /**
     * Cancels progress updates and coroutine jobs to clean up resources.
     */
    fun release() {
        stopProgressUpdate()
        job.cancel() // Cancel the whole scope to release resources
    }

}

/**
 * Events that can be triggered to control audio playback.
 */
sealed class PlayerEvent {
    data object PlayPause : PlayerEvent()
    data object SelectedAudioChange : PlayerEvent()
    data object Backward : PlayerEvent()
    data object SeekToNext : PlayerEvent()
    data object SeekToPrevious : PlayerEvent()
    data object Forward : PlayerEvent()
    data object SeekTo : PlayerEvent()
    data object Stop : PlayerEvent()
    data class UpdateProgress(val newProgress: Float) : PlayerEvent()
}

/**
 * States emitted by the audio handler to describe playback status.
 */
sealed class M3AudioState {
    data object Initial : M3AudioState()
    data class Ready(val duration: Long) : M3AudioState()
    data class Progress(val progress: Long) : M3AudioState()
    data class Buffering(val progress: Long) : M3AudioState()
    data class Playing(val isPlaying: Boolean) : M3AudioState()
    data class CurrentPlaying(val mediaItemIndex: Int) : M3AudioState()
}
