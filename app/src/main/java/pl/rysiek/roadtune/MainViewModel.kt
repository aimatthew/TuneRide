package pl.rysiek.roadtune

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.rysiek.roadtune.data.DownloadEntity
import pl.rysiek.roadtune.data.ThemeMode
import pl.rysiek.roadtune.download.DownloadMode
import pl.rysiek.roadtune.download.DownloadWorker
import pl.rysiek.roadtune.download.PlaylistDetails
import pl.rysiek.roadtune.download.PlaylistPrompt
import pl.rysiek.roadtune.download.PlaylistRepository
import pl.rysiek.roadtune.preview.AudioPreviewRepository
import pl.rysiek.roadtune.preview.PreviewState
import pl.rysiek.roadtune.preview.PreviewState.Ready
import pl.rysiek.roadtune.update.UpdateRepository
import pl.rysiek.roadtune.update.UpdateUiState
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as RoadTuneApplication
    private val dao = app.database.downloads()
    private val workManager = WorkManager.getInstance(application)
    private val updateRepository = UpdateRepository(application)
    private val audioPreviewRepository = AudioPreviewRepository(application)
    private val playlistRepository = PlaylistRepository(application)

    val history = dao.observeAll().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )
    val settings = app.settings.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        pl.rysiek.roadtune.data.AppSettings()
    )

    private val _url = MutableStateFlow("")
    val url = _url.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private val _previewState = MutableStateFlow<PreviewState>(PreviewState.Idle)
    val previewState = _previewState.asStateFlow()

    private val _downloadMode = MutableStateFlow(DownloadMode.SINGLE)
    val downloadMode = _downloadMode.asStateFlow()

    private val _playlistPrompt = MutableStateFlow<PlaylistPrompt?>(null)
    val playlistPrompt = _playlistPrompt.asStateFlow()

    private val _isPreparingPlaylist = MutableStateFlow(false)
    val isPreparingPlaylist = _isPreparingPlaylist.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState = _updateState.asStateFlow()

    private val _openUpdates = MutableStateFlow(false)
    val openUpdates = _openUpdates.asStateFlow()

    fun setUrl(value: String) {
        val newUrl = extractUrl(value)
        if (newUrl != _url.value) {
            _previewState.value = PreviewState.Idle
            _downloadMode.value = DownloadMode.SINGLE
            _playlistPrompt.value = if (isYoutubePlaylist(newUrl)) {
                PlaylistPrompt(canSelectSingleTrack = singleVideoUrl(newUrl) != null)
            } else {
                null
            }
        }
        _url.value = newUrl
    }

    fun selectSingleTrack() {
        val singleUrl = singleVideoUrl(_url.value)
        if (singleUrl == null) {
            _message.value = "Ten link nie wskazuje konkretnego utworu"
            return
        }
        _url.value = singleUrl
        _downloadMode.value = DownloadMode.SINGLE
        _playlistPrompt.value = null
        _previewState.value = PreviewState.Idle
    }

    fun selectWholePlaylist() {
        _url.value = playlistOnlyUrl(_url.value) ?: _url.value
        _downloadMode.value = DownloadMode.PLAYLIST
        _playlistPrompt.value = null
        _previewState.value = PreviewState.Idle
    }

    fun cancelPlaylistSelection() {
        _url.value = ""
        _downloadMode.value = DownloadMode.SINGLE
        _playlistPrompt.value = null
        _previewState.value = PreviewState.Idle
    }

    fun loadPreview() {
        if (_previewState.value is PreviewState.Loading) return
        if (_downloadMode.value == DownloadMode.PLAYLIST) {
            _previewState.value = PreviewState.Error(
                "Odsłuch jest dostępny po wybraniu pojedynczego utworu"
            )
            return
        }
        val cleanUrl = extractUrl(_url.value)
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            _previewState.value = PreviewState.Error("Wklej prawidłowy link do filmu")
            return
        }

        _url.value = cleanUrl
        _previewState.value = PreviewState.Loading
        viewModelScope.launch {
            val result: PreviewState = runCatching {
                withContext(Dispatchers.IO) { audioPreviewRepository.load(cleanUrl) }
            }.fold(
                onSuccess = { Ready(it) },
                onFailure = { error ->
                    PreviewState.Error(previewErrorMessage(error))
                }
            )
            if (_url.value == cleanUrl) {
                _previewState.value = result
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun setBitrate(value: Int) {
        viewModelScope.launch { app.settings.setBitrate(value) }
    }

    fun setThemeMode(value: ThemeMode) {
        viewModelScope.launch { app.settings.setThemeMode(value) }
    }

    fun setMaxConcurrentDownloads(value: Int) {
        viewModelScope.launch { app.settings.setMaxConcurrentDownloads(value) }
    }

    fun setFolder(uri: Uri) {
        viewModelScope.launch {
            val name = DocumentFile.fromTreeUri(getApplication(), uri)?.name ?: "Wybrany folder"
            app.settings.setFolder(uri.toString(), name)
        }
    }

    fun startDownload() {
        val cleanUrl = extractUrl(_url.value)
        val mode = _downloadMode.value
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            _message.value = "Wklej prawidłowy link do filmu"
            return
        }
        if (mode == DownloadMode.PLAYLIST) {
            prepareAndEnqueuePlaylist(cleanUrl)
            return
        }

        val id = UUID.randomUUID().toString()
        val currentSettings = settings.value
        val entity = DownloadEntity(
            id = id,
            sourceUrl = cleanUrl,
            title = "Oczekiwanie na informacje…",
            bitrate = currentSettings.bitrate
        )
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                DownloadWorker.input(
                    id = id,
                    url = cleanUrl,
                    bitrate = currentSettings.bitrate,
                    folderUri = currentSettings.folderUri,
                    downloadPlaylist = false,
                    playlistGroupId = null,
                    maxConcurrent = 1,
                    filePrefix = null
                )
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag(id)
            .build()

        viewModelScope.launch {
            dao.insert(entity)
            workManager.enqueue(request)
            _url.value = ""
            _previewState.value = PreviewState.Idle
            _downloadMode.value = DownloadMode.SINGLE
            _playlistPrompt.value = null
            _message.value = "Dodano do kolejki"
        }
    }

    private fun prepareAndEnqueuePlaylist(sourceUrl: String) {
        if (_isPreparingPlaylist.value) return
        _isPreparingPlaylist.value = true
        viewModelScope.launch {
            try {
                val details = withContext(Dispatchers.IO) {
                    playlistRepository.load(sourceUrl)
                }
                enqueuePlaylist(details)
                _url.value = ""
                _previewState.value = PreviewState.Idle
                _downloadMode.value = DownloadMode.SINGLE
                _playlistPrompt.value = null
                _message.value = "Dodano ${details.tracks.size} utworów do kolejki"
            } catch (error: Throwable) {
                _message.value = playlistErrorMessage(error)
            } finally {
                _isPreparingPlaylist.value = false
            }
        }
    }

    private suspend fun enqueuePlaylist(details: PlaylistDetails) {
        val currentSettings = settings.value
        val playlistId = UUID.randomUUID().toString()
        val total = details.tracks.size
        val now = System.currentTimeMillis()
        val items = details.tracks.map { track ->
            DownloadEntity(
                id = UUID.randomUUID().toString(),
                sourceUrl = track.sourceUrl,
                title = track.title,
                uploader = track.uploader,
                thumbnailUrl = track.thumbnailUrl,
                bitrate = currentSettings.bitrate,
                playlistId = playlistId,
                playlistTitle = details.title,
                playlistPosition = track.position,
                playlistTotal = total,
                createdAt = now + (total - track.position).toLong()
            )
        }
        dao.insertAll(items)

        val maxConcurrent = currentSettings.maxConcurrentDownloads.coerceIn(1, 4)
        val requests = items.map { item ->
            OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(
                    DownloadWorker.input(
                        id = item.id,
                        url = item.sourceUrl,
                        bitrate = item.bitrate,
                        folderUri = currentSettings.folderUri,
                        downloadPlaylist = false,
                        playlistGroupId = playlistId,
                        maxConcurrent = maxConcurrent,
                        filePrefix = item.playlistPosition?.toString()?.padStart(3, '0')
                    )
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag(item.id)
                .addTag("playlist:$playlistId")
                .build()
        }
        workManager.enqueue(requests)
    }

    fun deleteHistory(item: DownloadEntity) {
        viewModelScope.launch {
            if (item.state.name in setOf("QUEUED", "PREPARING", "DOWNLOADING", "COPYING")) {
                workManager.cancelAllWorkByTag(item.id)
            }
            dao.delete(item)
        }
    }

    fun clearHistory() {
        viewModelScope.launch { dao.clear() }
    }

    fun requestUpdatesScreen() {
        _openUpdates.value = true
    }

    fun updatesScreenOpened() {
        _openUpdates.value = false
    }

    fun checkForUpdates() {
        if (_updateState.value is UpdateUiState.Checking) return
        viewModelScope.launch {
            _updateState.value = UpdateUiState.Checking
            _updateState.value = runCatching {
                withContext(Dispatchers.IO) {
                    updateRepository.check(BuildConfig.VERSION_NAME)
                }
            }.fold(
                onSuccess = { release ->
                    release?.let { UpdateUiState.Available(it) } ?: UpdateUiState.UpToDate
                },
                onFailure = { error ->
                    UpdateUiState.Error(error.message ?: "Nieznany błąd połączenia")
                }
            )
        }
    }

    fun downloadUpdate() {
        val release = (_updateState.value as? UpdateUiState.Available)?.release ?: return
        viewModelScope.launch {
            _updateState.value = UpdateUiState.Downloading(release, 0)
            runCatching {
                withContext(Dispatchers.IO) {
                    updateRepository.download(release) { progress ->
                        _updateState.value = UpdateUiState.Downloading(release, progress)
                    }
                }
            }.onSuccess { file ->
                _updateState.value = UpdateUiState.Downloaded(release, file)
            }.onFailure { error ->
                _updateState.value = UpdateUiState.Error(
                    error.message ?: "Nie udało się pobrać aktualizacji"
                )
            }
        }
    }

    fun installDownloadedUpdate() {
        val downloaded = _updateState.value as? UpdateUiState.Downloaded ?: return
        if (!updateRepository.requestInstall(downloaded.file)) {
            _message.value =
                "Zezwól TuneRide na instalowanie aplikacji, wróć i naciśnij Zainstaluj ponownie"
        }
    }

    private fun extractUrl(text: String): String {
        return Regex("https?://\\S+").find(text.trim())?.value?.trimEnd(')', ']', ',', '.')
            ?: text.trim()
    }

    private fun isYoutubePlaylist(value: String): Boolean {
        val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return false
        val host = uri.host?.lowercase().orEmpty()
        val isYoutube = host == "youtu.be" || host.endsWith(".youtube.com") || host == "youtube.com"
        return isYoutube && !uri.getQueryParameter("list").isNullOrBlank()
    }

    private fun playlistOnlyUrl(value: String): String? {
        val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return null
        val host = uri.host?.lowercase().orEmpty()
        val isYoutube = host == "youtu.be" || host == "youtube.com" || host.endsWith(".youtube.com")
        if (!isYoutube) return null
        val listId = uri.getQueryParameter("list")?.takeIf { it.isNotBlank() } ?: return null
        if (listId.startsWith("RD")) {
            val videoId = uri.getQueryParameter("v")
                ?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }
                ?: listId.removePrefix("RD")
                    .takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }
            return videoId?.let {
                "https://www.youtube.com/watch?v=$it&list=${Uri.encode(listId)}"
            } ?: value
        }
        return "https://www.youtube.com/playlist?list=${Uri.encode(listId)}"
    }

    private fun singleVideoUrl(value: String): String? {
        val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return null
        val host = uri.host?.lowercase().orEmpty()
        val videoId = when {
            host == "youtu.be" -> uri.pathSegments.firstOrNull()
            host == "youtube.com" || host.endsWith(".youtube.com") -> when {
                uri.path == "/watch" -> uri.getQueryParameter("v")
                uri.pathSegments.firstOrNull() in setOf("shorts", "embed", "live") -> {
                    uri.pathSegments.getOrNull(1)
                }
                else -> null
            }
            else -> null
        }?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{6,}")) }

        return videoId?.let { "https://www.youtube.com/watch?v=$it" }
    }

    private fun previewErrorMessage(error: Throwable): String {
        val raw = error.message.orEmpty()
        return when {
            raw.contains("Unsupported URL", true) -> "Nieobsługiwany adres filmu"
            raw.contains("Private video", true) -> "Film jest prywatny"
            raw.contains("Video unavailable", true) -> "Film jest niedostępny"
            raw.isBlank() -> "Nie udało się przygotować odsłuchu"
            else -> raw.lines()
                .filterNot { it.contains("older than 90 days", ignoreCase = true) }
                .filterNot { it.contains("strongly recommended", ignoreCase = true) }
                .filter { it.isNotBlank() }
                .takeLast(3)
                .joinToString("\n")
        }
    }

    private fun playlistErrorMessage(error: Throwable): String {
        val raw = error.message.orEmpty()
        return raw.lines()
            .filterNot { it.contains("older than 90 days", ignoreCase = true) }
            .filterNot { it.contains("strongly recommended", ignoreCase = true) }
            .filter { it.isNotBlank() }
            .takeLast(3)
            .joinToString("\n")
            .ifBlank { "Nie udało się odczytać playlisty" }
    }
}
