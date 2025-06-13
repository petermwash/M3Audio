package com.pemwa.m3audio

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.pemwa.m3audio.player.service.M3AudioService
import com.pemwa.m3audio.ui.audio.AudioListScreen
import com.pemwa.m3audio.ui.audio.AudioViewModel
import com.pemwa.m3audio.ui.audio.UIEvents
import com.pemwa.m3audio.ui.theme.M3AudioTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: AudioViewModel by viewModels()
    private var isServiceRunning = false

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            M3AudioTheme {
                val permissionState = rememberPermissionState(
                    permission = Manifest.permission.READ_EXTERNAL_STORAGE
                )
                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(key1 = lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            permissionState.launchPermissionRequest()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AudioListScreen(
                        audioList = viewModel.audioList,
                        progress = viewModel.progress,
                        onProgress = { viewModel.onUiEvent(UIEvents.SeekTo(it)) },
                        onStart = { viewModel.onUiEvent(UIEvents.PlayPause) },
                        onNext = { viewModel.onUiEvent(UIEvents.SeekToNext) },
                        isAudioPlaying = viewModel.isPlaying,
                        currentPlaying = viewModel.currentSelected,
                        onItemClick = {
                            viewModel.onUiEvent(UIEvents.SelectedAudioChange(it))
                            startService()
                        }
                    )
                }
            }
        }
    }

    private fun startService() {
        if (!isServiceRunning) {
            val serviceIntent = Intent(this, M3AudioService::class.java)
            startForegroundService(serviceIntent)
        }
        isServiceRunning = true
    }
}
