package com.example.musicplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AceOrange = Color(0xFFFF6A00)
private val AceAmber = Color(0xFFFFB000)
private val AceDarkBackground = Color(0xFF080A0F)
private val AceDarkSurface = Color(0xFF15171C)
private val AceDarkSurfaceVariant = Color(0xFF20242B)
private val AceDarkText = Color(0xFFF5F5F5)
private val AceDarkMuted = Color(0xFFB8BDC7)

private val AceDarkColors = darkColorScheme(
    primary = AceOrange,
    onPrimary = Color.White,
    secondary = AceAmber,
    onSecondary = Color.Black,
    tertiary = AceAmber,
    background = AceDarkBackground,
    onBackground = AceDarkText,
    surface = AceDarkSurface,
    onSurface = AceDarkText,
    surfaceVariant = AceDarkSurfaceVariant,
    onSurfaceVariant = AceDarkMuted
)

private val AceLightColors = lightColorScheme(
    primary = Color(0xFFE85D00),
    onPrimary = Color.White,
    secondary = Color(0xFF9A6800),
    onSecondary = Color.White,
    tertiary = Color(0xFFB45309),
    background = Color(0xFFF8F8FA),
    onBackground = Color(0xFF17181C),
    surface = Color.White,
    onSurface = Color(0xFF17181C),
    surfaceVariant = Color(0xFFE9EAEE),
    onSurfaceVariant = Color(0xFF5E626B)
)

@Composable
fun MusicPlayerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) AceDarkColors else AceLightColors,
        typography = Typography,
        content = content
    )
}
