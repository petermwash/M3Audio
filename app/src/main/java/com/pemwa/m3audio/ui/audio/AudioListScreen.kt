package com.pemwa.m3audio.ui.audio

import android.graphics.drawable.VectorDrawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.pemwa.m3audio.R
import com.pemwa.m3audio.data.local.model.Audio
import com.pemwa.m3audio.ui.theme.M3AudioTheme
import kotlin.math.floor

/**
 * Formats the duration in milliseconds into a string (e.g., 3:42).
 *
 * @param duration Duration in milliseconds
 * @return Human-readable time string
 */
private fun formatDuration(duration: Long): String {
    val seconds = floor(duration / 1E3).toInt()
    val minutes = seconds / 60
    val remSeconds = seconds - (minutes * 60)

    return if (duration < 0) "--:--" else "%d:%02d".format(minutes, remSeconds)
}

/**
 * Main screen displaying the list of audio files and a persistent bottom audio player.
 *
 * @param progress Current playback progress (0f to 100f)
 * @param onProgress Lambda to update playback progress via slider
 * @param isAudioPlaying Playback state (true if playing, false if paused)
 * @param currentPlaying The currently playing audio item
 * @param audioList List of all available audio tracks
 * @param onStart Callback for play/pause toggle
 * @param onItemClick Callback when an item in the list is clicked
 * @param onNext Callback when the next button is clicked
 * @param onPrevious Callback when the previous button is clicked
 */
@Composable
fun AudioListScreen(
    progress: Float,
    onProgress: (Float) -> Unit,
    isAudioPlaying: Boolean,
    currentPlaying: Audio,
    audioList: List<Audio>,
    onStart: () -> Unit,
    onItemClick: (Int) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    Scaffold(
        topBar = {
            AudioPlayer(
                progress = progress,
                onProgress = onProgress,
                isAudioPlaying = isAudioPlaying,
                audio = currentPlaying,
                onStart = onStart,
                onNext = onNext,
                onPrevious = onPrevious
            )
        }
    ) {
        LazyColumn(
            contentPadding = it
        ) {
            itemsIndexed(audioList) { index, audioItem ->
                AudioItem(
                    audio = audioItem,
                    onItemClick = { onItemClick(index) }
                )
            }
        }
    }
}

/**
 * Persistent top player UI shown on the screen.
 * Displays the currently playing track, a slider for progress,
 * and control buttons (play/pause, skipNext and skipPrevious).
 *
 * @param progress Current progress of the track (0f to 100f)
 * @param onProgress Lambda to handle slider movement
 * @param isAudioPlaying Whether playback is active
 * @param audio Currently playing audio metadata
 * @param onStart Toggle play/pause
 * @param onNext Trigger skip to next track
 * @param onPrevious Trigger skip to previous track
 */
@Composable
fun AudioPlayer(
    progress: Float,
    onProgress: (Float) -> Unit,
    isAudioPlaying: Boolean,
    audio: Audio,
    onStart: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(320.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
        content = {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                MyLottieAnimation(isAudioPlaying)
                ArtistInfo(
                    audio = audio,
                    isAudioPlaying = isAudioPlaying,
                    onStart = onStart
                )
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .height(32.dp)
                        .padding(top = 8.dp)
                )
                AudioPlayerController(
                    onProgress = onProgress,
                    progress = progress,
                    onNext = onNext,
                    onPrevious = onPrevious
                )
            }
        }
    )
}

/**
 * Lottie animation for displaying audio waves.
 *
 * @param isAudioPlaying Whether playback is active
 */
@Composable
fun MyLottieAnimation(
    isAudioPlaying: Boolean
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.audio_waves))
    val progress by animateLottieCompositionAsState(
        composition,
        isPlaying = isAudioPlaying,
        restartOnPlay = true,
        iterations = LottieConstants.IterateForever
    )

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier
                .height(200.dp)
                .padding(top = 40.dp)
                .fillMaxWidth()
                .zIndex(10f)
        )
    }

}

/**
 * Represents a single audio item in the list.
 *
 * Displays title, artist, and duration. Clicking it triggers playback.
 *
 * @param audio Audio metadata model
 * @param onItemClick Callback triggered when item is clicked, passes audio ID
 */
@Composable
fun AudioItem(
    audio: Audio,
    onItemClick: (Long) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                onItemClick(audio.id)
            }
    ) {
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(12.dp)
            ) {
                PlayerIcon(
                    modifier = Modifier
                        .height(60.dp)
                        .width(60.dp),
                    icon = Icons.Default.MusicNote,
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    borderStroke = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                ) {}
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = audio.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Artist: ${audio.artist}",
                        style = MaterialTheme.typography.bodySmall,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        fontWeight = FontWeight.Normal
                    )
                }
                Text(
                    text = formatDuration(audio.duration.toLong()),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

    }
}

/**
 * Horizontal row with playback control buttons: play/pause, skipNext and skipPrevious.
 *
 * @param progress Current progress of the track (0f to 100f)
 * @param onProgress Lambda to handle slider movement
 * @param onNext Callback to skip to the next track
 * @param onPrevious Callback to skip to the previous track
 */
@Composable
fun AudioPlayerController(
    modifier: Modifier = Modifier,
    progress: Float,
    onProgress: (Float) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    Row(
        modifier = modifier
            .padding(4.dp)
            .height(56.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            imageVector = Icons.Default.SkipPrevious,
            modifier = Modifier.clickable { onPrevious() }.size(46.dp),
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(8.dp))
        AudioSlider(
            progress = progress,
            onProgress = onProgress
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.SkipNext,
            modifier = Modifier.clickable { onNext() }.size(46.dp),
            contentDescription = null
        )
    }
}

/**
 * Displays the currently playing song's title and artist.
 *
 * @param audio The audio currently playing
 */
@Composable
fun ArtistInfo(
    modifier: Modifier = Modifier,
    audio: Audio,
    isAudioPlaying: Boolean,
    onStart: () -> Unit,
) {
    Row(
        modifier = modifier
            .padding(4.dp)
            .height(56.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = audio.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Artist: ${audio.artist}",
                style = MaterialTheme.typography.bodySmall,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                fontWeight = FontWeight.Normal
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        PlayerIcon(
            modifier = Modifier.size(60.dp),
            backgroundColor = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            tint = MaterialTheme.colorScheme.surfaceVariant,
            size = 60.dp
        ) {
            onStart()
        }
    }
}

/**
 * Reusable composable for drawing a circular media control icon.
 *
 * @param icon Icon to display (e.g., play, pause, music note)
 * @param borderStroke Optional border for styling
 * @param backgroundColor Background of the icon surface
 * @param color Foreground (icon) color
 * @param onClick Callback when icon is clicked
 */
@Composable
fun PlayerIcon(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = 20.dp,
    borderStroke: BorderStroke? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit

) {
    Surface(
        shape = CircleShape,
        border = borderStroke,
        modifier = modifier
            .clip(CircleShape)
            .clickable { onClick() },
        contentColor = color,
        color = backgroundColor
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Icon(
                imageVector = icon,
                tint = tint,
                contentDescription = null,
                modifier = Modifier.padding(4.dp).size(size)
            )
        }
    }
}

/**
 * Reusable composable for displaying a slider.
 *
 * @param progress Current progress of the track (0f to 100f)
 * @param onProgress Lambda to handle slider movement
 * @param activeColor Color of the slider track
 * @param inactiveColor Color of the inactive slider track
 */
@Composable
fun AudioSlider(
    progress: Float,
    onProgress: (Float) -> Unit,
    activeColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    inactiveColor: Color = MaterialTheme.colorScheme.primary
) {
    Slider(
        value = progress,
        onValueChange = onProgress,
        valueRange = 0f..100f,
        colors = SliderDefaults.colors(
            thumbColor = activeColor,
            activeTrackColor = activeColor,
            inactiveTrackColor = inactiveColor
        ),
        modifier = Modifier
            .height(24.dp)
            .padding(horizontal = 4.dp)
            .width(250.dp)
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AudioListScreenPreview() {
    M3AudioTheme {
        AudioListScreen(
            progress = 50f,
            onProgress = {},
            isAudioPlaying = true,
            currentPlaying = Audio(
                "".toUri(), "Title One", 0L, "Said", "", 0, "Song One"
            ),
            audioList = listOf(
                Audio(
                    "".toUri(), "Title One", 0L, "Said", "", 0, ""
                ),
                Audio(
                    "".toUri(), "Title Two", 0L, "Said Said", "", 0, ""
                )
            ),
            onStart = {},
            onItemClick = {},
            onNext = {},
            onPrevious = {}
        )
    }
}
