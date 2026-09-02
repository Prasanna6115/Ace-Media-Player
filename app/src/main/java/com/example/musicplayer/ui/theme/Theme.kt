package com.example.musicplayer.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AceOrange = Color(0xFFFF6A00)
private val AceAmber = Color(0xFFFFB000)
private val AceBackground = Color(0xFF080A0F)
private val AceSurface = Color(0xFF15171C)
private val AceSurfaceVariant = Color(0xFF20242B)
private val AceText = Color(0xFFF5F5F5)
private val AceMuted = Color(0xFFB8BDC7)

private val AceDarkColors = darkColorScheme(
    primary = AceOrange,
    onPrimary = Color.White,
    secondary = AceAmber,
    onSecondary = Color.Black,
    background = AceBackground,
    onBackground = AceText,
    surface = AceSurface,
    onSurface = AceText,
    surfaceVariant = AceSurfaceVariant,
    onSurfaceVariant = AceMuted
)

private val AceLightColors = lightColorScheme(
    primary = Color(0xFFE85D00),
    onPrimary = Color.White,
    secondary = Color(0xFFC88700),
    onSecondary = Color.White
)

@Composable
fun MusicPlayerTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AceDarkColors else AceLightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
