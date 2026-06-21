package pl.rysiek.roadtune.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF3D5AFE),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3E7FF),
    onPrimaryContainer = Color(0xFF152170),
    secondary = Color(0xFF00A884),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB8F4E4),
    background = Color(0xFFF7F8FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEEF0F7),
    onSurfaceVariant = Color(0xFF5E6375),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB9C3FF),
    primaryContainer = Color(0xFF2437B5),
    secondary = Color(0xFF63DDBB),
    secondaryContainer = Color(0xFF005143),
    background = Color(0xFF111318),
    surface = Color(0xFF191B21),
    surfaceVariant = Color(0xFF252831)
)

@Composable
fun RoadTuneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
