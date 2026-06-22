package pl.rysiek.roadtune.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import pl.rysiek.roadtune.MainViewModel
import pl.rysiek.roadtune.R
import pl.rysiek.roadtune.data.AppSettings
import pl.rysiek.roadtune.data.DownloadEntity
import pl.rysiek.roadtune.download.PlaylistSelection
import pl.rysiek.roadtune.data.DownloadState
import pl.rysiek.roadtune.download.DownloadMode
import pl.rysiek.roadtune.download.PlaylistPrompt
import pl.rysiek.roadtune.preview.PreviewState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class AppTab { DOWNLOAD, HISTORY, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoadTuneApp(viewModel: MainViewModel) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val url by viewModel.url.collectAsStateWithLifecycle()
    val previewState by viewModel.previewState.collectAsStateWithLifecycle()
    val downloadMode by viewModel.downloadMode.collectAsStateWithLifecycle()
    val playlistPrompt by viewModel.playlistPrompt.collectAsStateWithLifecycle()
    val isPreparingPlaylist by viewModel.isPreparingPlaylist.collectAsStateWithLifecycle()
    val playlistSelection by viewModel.playlistSelection.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val openUpdates by viewModel.openUpdates.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    val currentTab = AppTab.entries[selectedTab]

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(openUpdates) {
        if (openUpdates) {
            selectedTab = AppTab.SETTINGS.ordinal
        }
    }

    playlistPrompt?.let {
        PlaylistChoiceDialog(
            prompt = it,
            onSingleTrack = viewModel::selectSingleTrack,
            onWholePlaylist = viewModel::selectWholePlaylist,
            onCancel = viewModel::cancelPlaylistSelection
        )
    }

    playlistSelection?.let {
        PlaylistSelectionDialog(
            selection = it,
            isPreparing = isPreparingPlaylist,
            onToggle = viewModel::togglePlaylistTrack,
            onSelectAll = viewModel::selectAllPlaylistTracks,
            onClearAll = viewModel::clearPlaylistTracks,
            onDownload = viewModel::downloadSelectedPlaylist,
            onCancel = viewModel::closePlaylistSelection
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.tuneride_icon),
                            contentDescription = "Ikona TuneRide",
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("TuneRide", fontWeight = FontWeight.Bold)
                            Text(
                                "Muzyka gotowa do drogi",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == AppTab.DOWNLOAD,
                    onClick = { selectedTab = AppTab.DOWNLOAD.ordinal },
                    icon = { Icon(Icons.Default.Download, null) },
                    label = { Text("Pobierz") }
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.HISTORY,
                    onClick = { selectedTab = AppTab.HISTORY.ordinal },
                    icon = { Icon(Icons.Default.History, null) },
                    label = { Text("Historia") }
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.SETTINGS,
                    onClick = { selectedTab = AppTab.SETTINGS.ordinal },
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { Text("Ustawienia") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        when (currentTab) {
            AppTab.DOWNLOAD -> DownloadScreen(
                padding = padding,
                url = url,
                previewState = previewState,
                downloadMode = downloadMode,
                isPreparingPlaylist = isPreparingPlaylist,
                settings = settings,
                downloads = history,
                onUrlChange = viewModel::setUrl,
                onLoadPreview = viewModel::loadPreview,
                onBitrateChange = viewModel::setBitrate,
                onFolderChange = viewModel::setFolder,
                onDownload = viewModel::startDownload
            )

            AppTab.HISTORY -> HistoryScreen(
                padding = padding,
                items = history,
                onDelete = viewModel::deleteHistory,
                onRetry = viewModel::retryDownload,
                onClear = viewModel::clearHistory
            )

            AppTab.SETTINGS -> SettingsScreen(
                padding = padding,
                themeMode = settings.themeMode,
                maxConcurrentDownloads = settings.maxConcurrentDownloads,
                updateState = updateState,
                openUpdatesInitially = openUpdates,
                onOpenUpdatesConsumed = viewModel::updatesScreenOpened,
                onThemeChange = viewModel::setThemeMode,
                onMaxConcurrentDownloadsChange = viewModel::setMaxConcurrentDownloads,
                onCheckUpdate = viewModel::checkForUpdates,
                onDownloadUpdate = viewModel::downloadUpdate,
                onInstallUpdate = viewModel::installDownloadedUpdate
            )
        }
    }
}

@Composable
private fun DownloadScreen(
    padding: PaddingValues,
    url: String,
    previewState: PreviewState,
    downloadMode: DownloadMode,
    isPreparingPlaylist: Boolean,
    settings: AppSettings,
    downloads: List<DownloadEntity>,
    onUrlChange: (String) -> Unit,
    onLoadPreview: () -> Unit,
    onBitrateChange: (Int) -> Unit,
    onFolderChange: (Uri) -> Unit,
    onDownload: () -> Unit
) {
    val context = LocalContext.current
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            onFolderChange(it)
        }
    }
    val activePlaylistGroups = downloads
        .filter { it.playlistId != null }
        .groupBy { it.playlistId.orEmpty() }
        .filterValues { group -> group.any { it.state.isActive() } }
        .values
        .sortedByDescending { group -> group.maxOfOrNull { it.createdAt } ?: 0L }
    val activeSingles = downloads.filter {
        it.playlistId == null && it.state.isActive()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { HeroCard() }
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "Link do filmu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = url,
                        onValueChange = onUrlChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://youtu.be/…") },
                        leadingIcon = { Icon(Icons.Default.LibraryMusic, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onLoadPreview,
                        enabled = url.isNotBlank() &&
                            previewState !is PreviewState.Loading &&
                            downloadMode == DownloadMode.SINGLE,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Headphones, null)
                        Spacer(Modifier.width(9.dp))
                        Text(
                            if (downloadMode == DownloadMode.PLAYLIST) {
                                "Odsłuch tylko pojedynczego utworu"
                            } else if (previewState is PreviewState.Loading) {
                                "Przygotowuję odsłuch…"
                            } else {
                                "Sprawdź i odsłuchaj"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    AudioPreviewCard(previewState, Modifier.padding(top = 10.dp))
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Jakość MP3",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(128, 192, 256, 320).forEach { bitrate ->
                            FilterChip(
                                selected = settings.bitrate == bitrate,
                                onClick = { onBitrateChange(bitrate) },
                                label = { Text("$bitrate") }
                            )
                        }
                    }
                    Text(
                        "192 kb/s to najlepszy kompromis dla muzyki z YouTube.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(18.dp))
                    FolderCard(settings.folderName) { folderPicker.launch(null) }
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = onDownload,
                        enabled = url.isNotBlank() && !isPreparingPlaylist,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Download, null)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (isPreparingPlaylist) {
                                "Odczytuję utwory i miniatury…"
                            } else if (downloadMode == DownloadMode.PLAYLIST) {
                                "Wybierz utwory z playlisty"
                            } else {
                                "Pobierz jako MP3"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        if (isPreparingPlaylist) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 3.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Odczytuję listę utworów, tytuły i miniatury…")
                    }
                }
            }
        }
        if (activePlaylistGroups.isNotEmpty() || activeSingles.isNotEmpty()) {
            item {
                Text(
                    "Aktualne zadania",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(activePlaylistGroups, key = { it.first().playlistId.orEmpty() }) { group ->
                PlaylistProgressCard(group)
            }
            items(activeSingles, key = { it.id }) { DownloadProgressCard(it) }
        }
        item {
            Text(
                "Pobieraj wyłącznie materiały, do których masz prawa lub zgodę autora.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun PlaylistChoiceDialog(
    prompt: PlaylistPrompt,
    onSingleTrack: () -> Unit,
    onWholePlaylist: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Wykryto playlistę") },
        text = {
            Text(
                if (prompt.canSelectSingleTrack) {
                    "Ten link zawiera konkretny utwór oraz playlistę. Co chcesz pobrać?"
                } else {
                    "Ten link prowadzi do całej playlisty i nie wskazuje jednego utworu."
                }
            )
        },
        confirmButton = {
            Button(onClick = onWholePlaylist) {
                Text("Wybierz z playlisty")
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onCancel) { Text("Anuluj") }
                if (prompt.canSelectSingleTrack) {
                    TextButton(onClick = onSingleTrack) { Text("Tylko ten utwór") }
                }
            }
        }
    )
}

@Composable
private fun PlaylistSelectionDialog(
    selection: PlaylistSelection,
    isPreparing: Boolean,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit
) {
    val tracks = selection.details.tracks
    val selectedCount = selection.selectedVideoIds.size
    val allSelected = selectedCount == tracks.size

    AlertDialog(
        onDismissRequest = { if (!isPreparing) onCancel() },
        title = {
            Column {
                Text(
                    selection.details.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "$selectedCount z ${tracks.size} zaznaczonych",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column {
                TextButton(
                    onClick = if (allSelected) onClearAll else onSelectAll,
                    enabled = !isPreparing
                ) {
                    Text(if (allSelected) "Odznacz wszystkie" else "Zaznacz wszystkie")
                }
                HorizontalDivider()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 430.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(tracks, key = { it.videoId }) { track ->
                        val selected = track.videoId in selection.selectedVideoIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !isPreparing) {
                                    onToggle(track.videoId)
                                }
                                .padding(vertical = 7.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selected,
                                onCheckedChange = { onToggle(track.videoId) },
                                enabled = !isPreparing
                            )
                            if (!track.thumbnailUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = track.thumbnailUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${track.position}. ${track.title}",
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                track.uploader?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDownload,
                enabled = selectedCount > 0 && !isPreparing
            ) {
                Text(
                    if (allSelected) {
                        "Pobierz całość ($selectedCount)"
                    } else {
                        "Pobierz wybrane ($selectedCount)"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !isPreparing) {
                Text("Wróć")
            }
        }
    )
}

@Composable
private fun HeroCard() {
    val gradient = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary
        )
    )
    Card(shape = RoundedCornerShape(26.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Z filmu do playlisty",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Wklej link, wybierz jakość i ruszaj w drogę.",
                    color = Color.White.copy(alpha = 0.84f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AudioFile,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
    }
}

@Composable
private fun FolderCard(folderName: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.FolderOpen, null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Folder zapisu", style = MaterialTheme.typography.labelMedium)
            Text(
                folderName,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Icons.Default.MoreVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PlaylistProgressCard(items: List<DownloadEntity>) {
    val ordered = items.sortedBy { it.playlistPosition ?: Int.MAX_VALUE }
    val total = ordered.firstOrNull()?.playlistTotal ?: ordered.size
    val completed = ordered.count { it.state == DownloadState.COMPLETED }
    val failed = ordered.count { it.state == DownloadState.FAILED }
    val remaining = (total - completed - failed).coerceAtLeast(0)
    val active = ordered.filter {
        it.state in setOf(
            DownloadState.PREPARING,
            DownloadState.DOWNLOADING,
            DownloadState.COPYING
        )
    }
    val queued = ordered.count { it.state == DownloadState.QUEUED }
    val progress = if (total == 0) 0f else {
        ordered.sumOf { item ->
            when (item.state) {
                DownloadState.COMPLETED, DownloadState.FAILED -> 100
                else -> item.progress
            }
        }.toFloat() / (total * 100f)
    }.coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LibraryMusic,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        ordered.firstOrNull()?.playlistTitle ?: "Playlista YouTube",
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Pobrano $completed z $total • Pozostało $remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
            )
            if (failed > 0) {
                Spacer(Modifier.height(7.dp))
                Text(
                    "Nieudane: $failed",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                if (active.size > 1) "Pobierane teraz (${active.size})" else "Pobierane teraz",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            if (active.isEmpty()) {
                Text(
                    "Oczekiwanie na rozpoczęcie • w kolejce: $queued",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                active.forEach { item ->
                    PlaylistCurrentTrack(item)
                    Spacer(Modifier.height(8.dp))
                }
                if (queued > 0) {
                    Text(
                        "W kolejce: $queued",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistCurrentTrack(item: DownloadEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!item.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "#${item.playlistPosition ?: "–"} ${item.title}",
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${item.state.label()} • ${item.progress}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { item.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
private fun DownloadProgressCard(item: DownloadEntity) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.title,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        item.state.label(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("${item.progress}%", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { item.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
private fun HistoryScreen(
    padding: PaddingValues,
    items: List<DownloadEntity>,
    onDelete: (DownloadEntity) -> Unit,
    onRetry: (DownloadEntity) -> Unit,
    onClear: () -> Unit
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.History,
                        null,
                        modifier = Modifier.size(42.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text("Historia jest pusta", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Pobrane tytuły pojawią się tutaj.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Historia pobierania",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${items.size} ${historyCountLabel(items.size)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onClear) { Text("Wyczyść") }
            }
        }
        items(items, key = { it.id }) { item ->
            HistoryCard(item = item, onDelete = { onDelete(item) }, onRetry = { onRetry(item) })
        }
    }
}

@Composable
private fun HistoryCard(
    item: DownloadEntity,
    onDelete: () -> Unit,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!item.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, null)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.title,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    item.uploader?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (item.state == DownloadState.COMPLETED && item.outputUri != null) {
                    IconButton(onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(item.outputUri), "audio/mpeg")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                            )
                        }
                    }) {
                        Icon(Icons.Default.OpenInNew, "Otwórz plik")
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, "Usuń z historii")
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StateIcon(item.state)
                Spacer(Modifier.width(6.dp))
                Text(
                    item.state.label(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = item.state.color(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "${item.bitrate} kb/s",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }
            AnimatedVisibility(item.state.isActive()) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { item.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item.errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schedule,
                    null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    formatDate(item.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                item.outputFileName?.let {
                    Text(
                        "  •  $it",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (item.outputFileName == null) {
                    Spacer(Modifier.weight(1f))
                }
                if (item.state == DownloadState.FAILED) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onRetry)
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "Ponów",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StateIcon(state: DownloadState) {
    val icon = when (state) {
        DownloadState.COMPLETED -> Icons.Default.CheckCircle
        DownloadState.FAILED -> Icons.Default.ErrorOutline
        else -> Icons.Default.Download
    }
    Icon(icon, null, modifier = Modifier.size(15.dp), tint = state.color())
}

@Composable
private fun DownloadState.color(): Color = when (this) {
    DownloadState.COMPLETED -> MaterialTheme.colorScheme.secondary
    DownloadState.FAILED -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.primary
}

private fun DownloadState.label(): String = when (this) {
    DownloadState.QUEUED -> "W kolejce"
    DownloadState.PREPARING -> "Sprawdzanie filmu"
    DownloadState.DOWNLOADING -> "Pobieranie i konwersja"
    DownloadState.COPYING -> "Zapisywanie pliku"
    DownloadState.COMPLETED -> "Gotowe"
    DownloadState.FAILED -> "Błąd"
}

private fun DownloadState.isActive(): Boolean = this in setOf(
    DownloadState.QUEUED,
    DownloadState.PREPARING,
    DownloadState.DOWNLOADING,
    DownloadState.COPYING
)

private fun historyCountLabel(count: Int): String = when {
    count == 1 -> "pozycja"
    count % 10 in 2..4 && count % 100 !in 12..14 -> "pozycje"
    else -> "pozycji"
}

private fun formatDate(timestamp: Long): String {
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale("pl", "PL"))
    return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(formatter)
}
