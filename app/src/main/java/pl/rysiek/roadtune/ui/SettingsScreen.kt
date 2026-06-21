package pl.rysiek.roadtune.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.rysiek.roadtune.data.ThemeMode
import pl.rysiek.roadtune.update.UpdateUiState

@Composable
fun SettingsScreen(
    padding: PaddingValues,
    themeMode: ThemeMode,
    maxConcurrentDownloads: Int,
    updateState: UpdateUiState,
    openUpdatesInitially: Boolean,
    onOpenUpdatesConsumed: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onMaxConcurrentDownloadsChange: (Int) -> Unit,
    onCheckUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit
) {
    var section by remember { mutableIntStateOf(if (openUpdatesInitially) 2 else 0) }
    LaunchedEffect(openUpdatesInitially) {
        if (openUpdatesInitially) {
            section = 2
            onOpenUpdatesConsumed()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
            Text(
                "Ustawienia",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = section == 0,
                    onClick = { section = 0 },
                    leadingIcon = { Icon(Icons.Default.Palette, null) },
                    label = { Text("Wygląd") }
                )
                FilterChip(
                    selected = section == 1,
                    onClick = { section = 1 },
                    leadingIcon = { Icon(Icons.Default.Download, null) },
                    label = { Text("Pobieranie") }
                )
                FilterChip(
                    selected = section == 2,
                    onClick = { section = 2 },
                    leadingIcon = { Icon(Icons.Default.SystemUpdate, null) },
                    label = { Text("Aktualizacja") }
                )
            }
        }

        Box(Modifier.weight(1f)) {
            when (section) {
                0 -> AppearanceSettings(themeMode, onThemeChange)
                1 -> DownloadSettings(
                    maxConcurrentDownloads = maxConcurrentDownloads,
                    onMaxConcurrentDownloadsChange = onMaxConcurrentDownloadsChange
                )
                else -> {
                UpdateScreen(
                    padding = PaddingValues(0.dp),
                    state = updateState,
                    onCheck = onCheckUpdate,
                    onDownload = onDownloadUpdate,
                    onInstall = onInstallUpdate
                )
                }
            }
        }
    }
}

@Composable
private fun DownloadSettings(
    maxConcurrentDownloads: Int,
    onMaxConcurrentDownloadsChange: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Równoległe pobieranie playlist",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Maksymalna liczba utworów jednocześnie",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..4).forEach { count ->
                            FilterChip(
                                selected = maxConcurrentDownloads == count,
                                onClick = { onMaxConcurrentDownloadsChange(count) },
                                label = { Text(count.toString()) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "2 to ustawienie zalecane. Limit dotyczy nowo dodawanych playlist. Większa liczba może szybciej nagrzewać telefon i zużywać więcej pamięci.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AppearanceSettings(
    selected: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Motyw aplikacji",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            ThemeOption(
                title = "Jasny",
                description = "Białe, przejrzyste UI — motyw domyślny",
                selected = selected == ThemeMode.LIGHT,
                iconColor = Color(0xFF3D5AFE),
                previewColors = listOf(Color.White, Color(0xFFF1F3FA), Color(0xFF3D5AFE)),
                icon = { Icon(Icons.Default.LightMode, null, tint = Color(0xFF3D5AFE)) },
                onClick = { onThemeChange(ThemeMode.LIGHT) }
            )
        }
        item {
            ThemeOption(
                title = "Ciemnoczerwony",
                description = "Ciemne tło z bordowymi i czerwonymi akcentami",
                selected = selected == ThemeMode.DARK_RED,
                iconColor = Color(0xFFFF5A64),
                previewColors = listOf(Color(0xFF12090B), Color(0xFF281216), Color(0xFFB82232)),
                icon = { Icon(Icons.Default.DarkMode, null, tint = Color(0xFFFF5A64)) },
                onClick = { onThemeChange(ThemeMode.DARK_RED) }
            )
        }
        item {
            Text(
                "Zmiana jest stosowana natychmiast i zapamiętywana na tym urządzeniu.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun ThemeOption(
    title: String,
    description: String,
    selected: Boolean,
    iconColor: Color,
    previewColors: List<Color>,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(iconColor.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) { icon() }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RadioButton(selected = selected, onClick = onClick)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                previewColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(color, CircleShape)
                    )
                }
            }
        }
    }
}
