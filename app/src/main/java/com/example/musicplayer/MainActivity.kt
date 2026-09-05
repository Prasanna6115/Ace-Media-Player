package com.example.musicplayer

import android.Manifest
import android.content.Intent
import android.os.Build
import android.content.pm.PackageManager
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowInsetsControllerCompat
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
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Start the playback service up front so lock screen / notification controls
        // and background playback are available as soon as something is queued.
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        startService(Intent(this, PlaybackService::class.java))

        requestNeededPermissions()

        setContent {
            val darkTheme by viewModel.darkTheme.collectAsState()
            MusicPlayerTheme(darkTheme = darkTheme) {
                SideEffect {
                    val barColor = if (darkTheme) Color(0xFF080A0F) else Color(0xFFF8F8FA)
                    window.statusBarColor = barColor.toArgb()
                    window.navigationBarColor = barColor.toArgb()
                    WindowInsetsControllerCompat(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !darkTheme
                        isAppearanceLightNavigationBars = !darkTheme
                    }
                }
                MusicNavGraph(viewModel = viewModel)
            }
        }
    }

    override fun onUserLeaveHint() {
        // Videos must stop/pause when the app is intentionally minimized.
        // Music playback remains owned by MusicController and continues normally.
        viewModel.videoPauseForBackground()
        super.onUserLeaveHint()
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
