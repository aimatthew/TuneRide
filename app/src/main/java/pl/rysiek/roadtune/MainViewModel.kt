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
import pl.rysiek.roadtune.download.DownloadWorker
import pl.rysiek.roadtune.update.UpdateRepository
import pl.rysiek.roadtune.update.UpdateUiState
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as RoadTuneApplication
    private val dao = app.database.downloads()
    private val workManager = WorkManager.getInstance(application)
    private val updateRepository = UpdateRepository(application)

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

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState = _updateState.asStateFlow()

    private val _openUpdates = MutableStateFlow(false)
    val openUpdates = _openUpdates.asStateFlow()

    fun setUrl(value: String) {
        _url.value = extractUrl(value)
    }

    fun clearMessage() {
        _message.value = null
    }

    fun setBitrate(value: Int) {
        viewModelScope.launch { app.settings.setBitrate(value) }
    }

    fun setFolder(uri: Uri) {
        viewModelScope.launch {
            val name = DocumentFile.fromTreeUri(getApplication(), uri)?.name ?: "Wybrany folder"
            app.settings.setFolder(uri.toString(), name)
        }
    }

    fun startDownload() {
        val cleanUrl = extractUrl(_url.value)
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            _message.value = "Wklej prawidłowy link do filmu"
            return
        }

        val id = UUID.randomUUID().toString()
        val currentSettings = settings.value
        val entity = DownloadEntity(
            id = id,
            sourceUrl = cleanUrl,
            bitrate = currentSettings.bitrate
        )
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                DownloadWorker.input(
                    id = id,
                    url = cleanUrl,
                    bitrate = currentSettings.bitrate,
                    folderUri = currentSettings.folderUri
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
            _message.value = "Dodano do kolejki"
        }
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
}
