package pl.rysiek.roadtune.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadState {
    QUEUED, PREPARING, DOWNLOADING, COPYING, COMPLETED, FAILED
}

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val sourceUrl: String,
    val title: String = "Oczekiwanie na informacje…",
    val uploader: String? = null,
    val thumbnailUrl: String? = null,
    val bitrate: Int,
    val playlistId: String? = null,
    val playlistTitle: String? = null,
    val playlistPosition: Int? = null,
    val playlistTotal: Int? = null,
    val state: DownloadState = DownloadState.QUEUED,
    val progress: Int = 0,
    val outputFileName: String? = null,
    val outputUri: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
