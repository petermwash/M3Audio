package com.pemwa.m3audio.ui.audio

import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.pemwa.m3audio.data.local.M3AudioPreferences
import com.pemwa.m3audio.data.repository.AudioRepository
import com.pemwa.m3audio.player.service.M3AudioServiceHandler
import com.pemwa.m3audio.data.local.model.Audio
import com.pemwa.m3audio.player.service.M3AudioState
import com.pemwa.m3audio.player.service.PlayerEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private val audioDummy = Audio(
    "".toUri(), "", 0L, "", "", 0, ""
)

/**
 * [AudioViewModel] manages UI-related audio playback state for a Composable-based audio player UI.
 *
 * It bridges user interaction with the [M3AudioServiceHandler] and observes playback state updates.
 * It also handles media item construction and formats playback progress for display.
 *
 * @property audioServiceHandler The service handler responsible for controlling ExoPlayer playback.
 * @property repository Repository used to fetch audio data.
 * @property playbackPreferences Persistence utility for storing playback progress and index.
 * @property savedStateHandle Used for saving/restoring UI state across configuration changes or process death.
 */
@HiltViewModel
@OptIn(SavedStateHandleSaveableApi::class)
class AudioViewModel @Inject constructor(
    private val audioServiceHandler: M3AudioServiceHandler,
    private val repository: AudioRepository,
    private val playbackPreferences: M3AudioPreferences,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private var duration by savedStateHandle.saveable { mutableLongStateOf(0L) }
    var progress by savedStateHandle.saveable { mutableFloatStateOf(0f) }
    private var progressString by savedStateHandle.saveable { mutableStateOf("00:00") }
    var isPlaying by savedStateHandle.saveable { mutableStateOf(false) }
    var currentSelected by savedStateHandle.saveable { mutableStateOf(audioDummy) }
    var audioList by savedStateHandle.saveable { mutableStateOf(listOf<Audio>()) }

    private val _uiState: MutableStateFlow<UIState> = MutableStateFlow(UIState.Initial)
    val uiState: StateFlow<UIState> = _uiState.asStateFlow()

    init {
        getAudioList()
        Log.d("AudioViewModel: Save_Index", playbackPreferences.getSavedAudioIndex().toString())
        Log.d("AudioViewModel: Save_Progress", playbackPreferences.getSavedProgress().toString())
        Log.d("AudioViewModel: Save_State", playbackPreferences.isPlaying().toString())
    }

    init {
        viewModelScope.launch {
            audioServiceHandler.audioState.collectLatest { mediaState ->
                when (mediaState) {
                    M3AudioState.Initial -> _uiState.value = UIState.Initial
                    is M3AudioState.Buffering -> calculateProgress(mediaState.progress)
                    is M3AudioState.CurrentPlaying -> currentSelected =
                        audioList[mediaState.mediaItemIndex]

                    is M3AudioState.Playing -> isPlaying = mediaState.isPlaying
                    is M3AudioState.Progress -> calculateProgress(mediaState.progress)
                    is M3AudioState.Ready -> {
                        duration = mediaState.duration
                        _uiState.value = UIState.Ready
                    }
                }
            }
        }
    }

    /**
     * Fetches the audio list from the repository and sets it to the ExoPlayer instance.
     */
    private fun getAudioList() {
        viewModelScope.launch {
            val audios = repository.getAudioData()
            audioList = audios
            setMediaItems()
        }
    }

    /**
     * Constructs [MediaItem]s from the [audioList] and submits them to the [M3AudioServiceHandler].
     */
    private fun setMediaItems() {
        audioList.map { audio ->
            MediaItem.Builder()
                .setUri(audio.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setAlbumArtist(audio.artist)
                        .setDisplayTitle(audio.title)
                        .setSubtitle(audio.displayName)
                        .build()
                )
                .build()
        }.also {
            audioServiceHandler.addMediaItemList(it)
        }
    }

    /**
     * Updates [progress] and [progressString] from the current playback progress.
     */
    private fun calculateProgress(currentProgress: Long) {
        progress =
            if (currentProgress > 0) ((currentProgress.toFloat() / duration.toFloat()) * 100f) else 0f
        progressString = formatDuration(currentProgress)
    }

    /**
     * Handles UI actions by mapping [UIEvents] to [PlayerEvent]s and dispatching them.
     */
    fun onUiEvent(uiEvent: UIEvents) = viewModelScope.launch {
        when (uiEvent) {
            UIEvents.Backward -> audioServiceHandler.onPlayerEvents(PlayerEvent.Backward)
            UIEvents.Forward -> audioServiceHandler.onPlayerEvents(PlayerEvent.Forward)
            UIEvents.PlayPause -> audioServiceHandler.onPlayerEvents(PlayerEvent.PlayPause)
            is UIEvents.SeekTo -> audioServiceHandler.onPlayerEvents(
                PlayerEvent.SeekTo,
                seekPosition = ((duration * uiEvent.position) / 100f).toLong()
            )

            UIEvents.SeekToNext -> audioServiceHandler.onPlayerEvents(PlayerEvent.SeekToNext)
            UIEvents.SeekToPrevious -> audioServiceHandler.onPlayerEvents(PlayerEvent.SeekToPrevious)
            is UIEvents.SelectedAudioChange -> audioServiceHandler.onPlayerEvents(
                PlayerEvent.SelectedAudioChange,
                selectedAudioIndex = uiEvent.index
            )

            is UIEvents.UpdateProgress -> {
                audioServiceHandler.onPlayerEvents(
                    PlayerEvent.UpdateProgress(
                        uiEvent.newProgress
                    )
                )
                progress = uiEvent.newProgress
            }
        }

    }

    /**
     * Formats the duration from milliseconds to a MM:SS string.
     */
    private fun formatDuration(currentProgress: Long): String {
        val minutes = TimeUnit.MINUTES.convert(currentProgress, TimeUnit.MILLISECONDS)
        val seconds = (minutes) - minutes * TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES)

        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    /**
     * Called when ViewModel is about to be destroyed. Stops the player.
     */
    override fun onCleared() {
        viewModelScope.launch {
            audioServiceHandler.onPlayerEvents(PlayerEvent.Stop)
        }
        super.onCleared()
    }
}

/**
 * Represents different states the audio UI can be in.
 */
sealed class UIState {
    data object Initial : UIState()
    data object Ready : UIState()
}

/**
 * Describes events triggered by the user from the UI.
 */
sealed class UIEvents {
    data object PlayPause : UIEvents()
    data class SelectedAudioChange(val index: Int) : UIEvents()
    data class SeekTo(val position: Float) : UIEvents()
    data object SeekToNext : UIEvents()
    data object SeekToPrevious : UIEvents()
    data object Backward : UIEvents()
    data object Forward : UIEvents()
    data class UpdateProgress(val newProgress: Float) : UIEvents()
}
