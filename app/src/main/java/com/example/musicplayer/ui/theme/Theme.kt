package com.example.musicplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AceCyan = Color(0xFF00E5FF)
private val AceViolet = Color(0xFF7C4DFF)
private val AceDarkBackground = Color(0xFF080A0F)
private val AceDarkSurface = Color(0xFF15171C)
private val AceDarkSurfaceVariant = Color(0xFF20242B)
private val AceDarkText = Color(0xFFF5F5F5)
private val AceDarkMuted = Color(0xFFB8BDC7)

private val AceDarkColors = darkColorScheme(
    primary = AceCyan,
    onPrimary = Color.White,
    secondary = AceViolet,
    onSecondary = Color.Black,
    tertiary = AceViolet,
    background = AceDarkBackground,
    onBackground = AceDarkText,
    surface = AceDarkSurface,
    onSurface = AceDarkText,
    surfaceVariant = AceDarkSurfaceVariant,
    onSurfaceVariant = AceDarkMuted
)

private val AceLightColors = lightColorScheme(
    primary = Color(0xFF00A9C2),
    onPrimary = Color.White,
    secondary = Color(0xFF6840D9),
    onSecondary = Color.White,
    tertiary = Color(0xFF5B3FC4),
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
