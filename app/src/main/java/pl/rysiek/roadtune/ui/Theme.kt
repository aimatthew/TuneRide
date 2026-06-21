package pl.rysiek.roadtune.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import pl.rysiek.roadtune.data.ThemeMode

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

private val DarkRedColors = darkColorScheme(
    primary = Color(0xFFFF5967),
    onPrimary = Color(0xFF3B0006),
    primaryContainer = Color(0xFF731827),
    onPrimaryContainer = Color(0xFFFFE0E2),
    secondary = Color(0xFFFFB3B9),
    onSecondary = Color(0xFF4B1017),
    secondaryContainer = Color(0xFF63222A),
    onSecondaryContainer = Color(0xFFFFDADC),
    background = Color(0xFF080405),
    onBackground = Color(0xFFFFE9EB),
    surface = Color(0xFF211014),
    onSurface = Color(0xFFFFE9EB),
    surfaceVariant = Color(0xFF3A1C23),
    onSurfaceVariant = Color(0xFFF2C4C9),
    surfaceContainerLowest = Color(0xFF050203),
    surfaceContainerLow = Color(0xFF160A0D),
    surfaceContainer = Color(0xFF211014),
    surfaceContainerHigh = Color(0xFF2D151A),
    surfaceContainerHighest = Color(0xFF3A1C23),
    outline = Color(0xFFB25C67),
    outlineVariant = Color(0xFF65343B),
    error = Color(0xFFFFB4AB)
)

@Composable
fun RoadTuneTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (themeMode == ThemeMode.DARK_RED) DarkRedColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
