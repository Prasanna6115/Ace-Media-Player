package com.example.musicplayer

import android.Manifest
import android.content.Intent
import android.os.Build
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.musicplayer.playback.PlaybackService
import com.example.musicplayer.ui.navigation.MusicNavGraph
import com.example.musicplayer.ui.theme.MusicPlayerTheme
import com.example.musicplayer.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val videoPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (results[audioPermission] == true ||
            checkSelfPermission(audioPermission) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.scanLibrary()
        }

        if (results[videoPermission] == true ||
            checkSelfPermission(videoPermission) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.scanVideos()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start the playback service up front so lock screen / notification controls
        // and background playback are available as soon as something is queued.
        startService(Intent(this, PlaybackService::class.java))

        requestNeededPermissions()

        setContent {
            MusicPlayerTheme {
                MusicNavGraph(viewModel = viewModel)
            }
        }
    }

    private fun requestNeededPermissions() {
        val permissions = mutableListOf<String>()

        permissions += if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.READ_MEDIA_VIDEO
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }

        permissionLauncher.launch(permissions.toTypedArray())
    }
}
