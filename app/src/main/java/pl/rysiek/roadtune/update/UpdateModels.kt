package pl.rysiek.roadtune.update

import java.io.File

data class AppRelease(
    val versionName: String,
    val title: String,
    val notes: String,
    val pageUrl: String,
    val apkUrl: String,
    val apkName: String,
    val sha256: String?
)

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val release: AppRelease) : UpdateUiState
    data class Downloading(val release: AppRelease, val progress: Int) : UpdateUiState
    data class Downloaded(val release: AppRelease, val file: File) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}
