package pl.rysiek.roadtune.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import pl.rysiek.roadtune.R
import pl.rysiek.roadtune.RoadTuneApplication
import pl.rysiek.roadtune.data.DownloadState
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock

class DownloadWorker(
    appContext: Context,
    parameters: WorkerParameters
) : CoroutineWorker(appContext, parameters) {

    private val dao = (appContext.applicationContext as RoadTuneApplication).database.downloads()
    private val itemId = inputData.getString(KEY_ID).orEmpty()
    private val sourceUrl = inputData.getString(KEY_URL).orEmpty()
    private val bitrate = inputData.getInt(KEY_BITRATE, 192)
    private val folderUri = inputData.getString(KEY_FOLDER_URI)
    private val downloadPlaylist = inputData.getBoolean(KEY_PLAYLIST, false)
    private val playlistGroupId = inputData.getString(KEY_PLAYLIST_GROUP)
    private val maxConcurrent = inputData.getInt(KEY_MAX_CONCURRENT, 1).coerceIn(1, 4)
    private val filePrefix = inputData.getString(KEY_FILE_PREFIX)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val gate = playlistGroupId?.takeIf { it.isNotBlank() }?.let { groupId ->
            playlistGates.computeIfAbsent(groupId) { Semaphore(maxConcurrent) }
        }
        if (gate == null) {
            return@withContext performDownload()
        }

        gate.acquire()
        try {
            performDownload()
        } finally {
            gate.release()
        }
    }

    private suspend fun performDownload(): Result {
        if (itemId.isBlank() || sourceUrl.isBlank()) return Result.failure()

        setForeground(createForegroundInfo(0, "Przygotowywanie…"))
        dao.updateProgress(itemId, DownloadState.PREPARING, 0)
        val tempDirectory = File(applicationContext.cacheDir, "downloads/$itemId")

        return try {
            tempDirectory.mkdirs()
            updateYoutubeDlIfNeeded()

            val title = if (downloadPlaylist) {
                "Playlista YouTube"
            } else {
                val info = YoutubeDL.getInstance().getInfo(sourceUrl)
                val trackTitle = info.title?.trim().takeUnless { it.isNullOrBlank() }
                    ?: "Pobrany utwór"
                dao.updateMetadata(itemId, trackTitle, info.uploader, info.thumbnail)
                trackTitle
            }

            val request = YoutubeDLRequest(sourceUrl).apply {
                addOption(if (downloadPlaylist) "--yes-playlist" else "--no-playlist")
                addOption("--no-mtime")
                addOption("--extract-audio")
                addOption("--audio-format", "mp3")
                addOption("--audio-quality", "${bitrate}K")
                addOption("--add-metadata")
                addOption("--newline")
                addOption("-f", "bestaudio[ext=m4a]/bestaudio")
                addOption(
                    "-o",
                    File(
                        tempDirectory,
                        if (downloadPlaylist) {
                            "%(playlist_index)03d - %(title).165B [%(id)s].%(ext)s"
                        } else {
                            "${filePrefix?.let { "$it - " }.orEmpty()}%(title).180B [%(id)s].%(ext)s"
                        }
                    ).absolutePath
                )
            }

            dao.updateProgress(itemId, DownloadState.DOWNLOADING, 0)
            var lastDatabaseUpdate = 0L
            YoutubeDL.getInstance().execute(request, itemId) { progress, _, line ->
                val percent = progress.toInt().coerceIn(0, 99)
                setProgressAsync(workDataOf(KEY_PROGRESS to percent, KEY_STATUS to line))
                setForegroundAsync(createForegroundInfo(percent, title))

                val now = System.currentTimeMillis()
                if (now - lastDatabaseUpdate > 750) {
                    lastDatabaseUpdate = now
                    runBlocking {
                        dao.updateProgress(itemId, DownloadState.DOWNLOADING, percent)
                    }
                }
            }

            val mp3Files = tempDirectory.walkTopDown()
                .filter { it.isFile && it.extension.equals("mp3", ignoreCase = true) }
                .sortedBy { it.name }
                .toList()
            if (mp3Files.isEmpty()) {
                error("Konwersja zakończyła się bez utworzenia pliku MP3")
            }

            dao.updateProgress(itemId, DownloadState.COPYING, 99)
            val destinations = mp3Files.map { mp3File ->
                if (!folderUri.isNullOrBlank()) {
                    copyToSelectedFolder(mp3File, Uri.parse(folderUri))
                } else {
                    copyToDefaultMusicFolder(mp3File)
                }
            }
            val firstDestination = destinations.first()
            val historyName = if (downloadPlaylist) {
                "${destinations.size} utworów z playlisty"
            } else firstDestination.fileName
            if (downloadPlaylist) {
                dao.updateMetadata(itemId, "Playlista • ${destinations.size} utworów", null, null)
            }

            dao.markCompleted(
                id = itemId,
                fileName = historyName,
                uri = firstDestination.uri.toString(),
                completedAt = System.currentTimeMillis()
            )
            tempDirectory.deleteRecursively()
            Result.success(workDataOf(KEY_OUTPUT_URI to firstDestination.uri.toString()))
        } catch (cancelled: CancellationException) {
            tempDirectory.deleteRecursively()
            throw cancelled
        } catch (error: Throwable) {
            val message = friendlyError(error)
            dao.markFailed(itemId, message)
            tempDirectory.deleteRecursively()
            Result.failure(workDataOf(KEY_ERROR to message))
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        createForegroundInfo(0, "Przygotowywanie…")

    private suspend fun updateYoutubeDlIfNeeded() {
        engineUpdateMutex.withLock {
            val preferences = applicationContext.getSharedPreferences(
            "roadtune_engine_updates",
            Context.MODE_PRIVATE
        )
        val now = System.currentTimeMillis()
        val lastUpdate = preferences.getLong("last_update", 0L)
        if (now - lastUpdate < 12 * 60 * 60 * 1000L) return@withLock

        runCatching {
            YoutubeDL.getInstance().updateYoutubeDL(
                applicationContext,
                YoutubeDL.UpdateChannel.NIGHTLY
            )
        }.onSuccess {
            preferences.edit().putLong("last_update", now).apply()
        }
        }
    }

    private fun copyToSelectedFolder(source: File, treeUri: Uri): SavedFile {
        val tree = DocumentFile.fromTreeUri(applicationContext, treeUri)
            ?: error("Wybrany folder jest już niedostępny")
        check(tree.canWrite()) { "Brak uprawnień do zapisu w wybranym folderze" }

        val requestedName = sanitizeFileName(source.name)
        val output = tree.createFile("audio/mpeg", requestedName)
            ?: error("Nie udało się utworzyć pliku w wybranym folderze")
        applicationContext.contentResolver.openOutputStream(output.uri, "w")?.use { target ->
            FileInputStream(source).use { it.copyTo(target) }
        } ?: error("Nie udało się otworzyć pliku docelowego")

        return SavedFile(output.uri, output.name ?: requestedName)
    }

    private fun copyToDefaultMusicFolder(source: File): SavedFile {
        val fileName = sanitizeFileName(source.name)
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/TuneRide")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val resolver = applicationContext.contentResolver
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Nie udało się utworzyć pliku w katalogu Muzyka")
        try {
            resolver.openOutputStream(uri, "w")?.use { target ->
                FileInputStream(source).use { it.copyTo(target) }
            } ?: error("Nie udało się zapisać pliku")
            values.clear()
            values.put(MediaStore.Audio.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return SavedFile(uri, fileName)
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    private fun createForegroundInfo(progress: Int, title: String): ForegroundInfo {
        val channelId = "roadtune_downloads"
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    applicationContext.getString(R.string.download_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("TuneRide")
            .setContentText(title)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, progress, progress == 0)
            .build()
        return ForegroundInfo(
            itemId.hashCode() and Int.MAX_VALUE,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun sanitizeFileName(value: String): String {
        val cleaned = value.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().trimEnd('.')
        return cleaned.ifBlank { "TuneRide_${System.currentTimeMillis()}.mp3" }
            .let { if (it.endsWith(".mp3", true)) it else "$it.mp3" }
    }

    private fun friendlyError(error: Throwable): String {
        val raw = error.message.orEmpty()
        return when {
            isStopped -> "Pobieranie zostało zatrzymane"
            raw.contains("Unsupported URL", true) -> "Nieobsługiwany adres filmu"
            raw.contains("Private video", true) -> "Film jest prywatny"
            raw.contains("Video unavailable", true) -> "Film jest niedostępny"
            raw.isBlank() -> "Nie udało się pobrać utworu"
            else -> raw.lines()
                .filterNot { it.contains("older than 90 days", ignoreCase = true) }
                .filterNot { it.contains("strongly recommended", ignoreCase = true) }
                .filter { it.isNotBlank() }
                .takeLast(6)
                .joinToString("\n")
                .ifBlank { raw.takeLast(500) }
        }
    }

    private data class SavedFile(val uri: Uri, val fileName: String)

    companion object {
        const val KEY_ID = "download_id"
        const val KEY_URL = "source_url"
        const val KEY_BITRATE = "bitrate"
        const val KEY_FOLDER_URI = "folder_uri"
        const val KEY_PLAYLIST = "download_playlist"
        const val KEY_PLAYLIST_GROUP = "playlist_group"
        const val KEY_MAX_CONCURRENT = "max_concurrent"
        const val KEY_FILE_PREFIX = "file_prefix"
        const val KEY_PROGRESS = "progress"
        const val KEY_STATUS = "status"
        const val KEY_OUTPUT_URI = "output_uri"
        const val KEY_ERROR = "error"

        fun input(
            id: String,
            url: String,
            bitrate: Int,
            folderUri: String?,
            downloadPlaylist: Boolean,
            playlistGroupId: String?,
            maxConcurrent: Int,
            filePrefix: String?
        ): Data = workDataOf(
                KEY_ID to id,
                KEY_URL to url,
                KEY_BITRATE to bitrate,
                KEY_FOLDER_URI to folderUri,
                KEY_PLAYLIST to downloadPlaylist,
                KEY_PLAYLIST_GROUP to playlistGroupId,
                KEY_MAX_CONCURRENT to maxConcurrent.coerceIn(1, 4),
                KEY_FILE_PREFIX to filePrefix
            )

        private val playlistGates = ConcurrentHashMap<String, Semaphore>()
        private val engineUpdateMutex = Mutex()
    }
}
