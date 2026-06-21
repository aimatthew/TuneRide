package pl.rysiek.roadtune.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import pl.rysiek.roadtune.MainActivity
import pl.rysiek.roadtune.R
import pl.rysiek.roadtune.data.DownloadDao
import pl.rysiek.roadtune.data.DownloadEntity
import pl.rysiek.roadtune.data.DownloadState

class DownloadNotifications(
    private val context: Context,
    private val dao: DownloadDao,
    private val itemId: String,
    private val playlistGroupId: String?
) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    fun foregroundInfo(
        item: DownloadEntity?,
        title: String,
        stage: String,
        progress: Int? = null,
        etaSeconds: Long = -1
    ): ForegroundInfo {
        ensureChannel()
        val position = item?.playlistPosition
        val total = item?.playlistTotal
        val displayTitle = if (position != null && total != null) {
            "$position/$total • $title"
        } else {
            title
        }
        val details = buildList {
            add(stage)
            progress?.takeIf { it > 0 }?.let { add("$it%") }
            formatEta(etaSeconds)?.let { add(it) }
        }.joinToString(" • ")

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_music)
            .setColor(BRAND_COLOR)
            .setContentTitle(displayTitle)
            .setContentText(details)
            .setSubText("TuneRide")
            .setContentIntent(openAppIntent())
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .apply {
                playlistGroupId?.let { setGroup(groupKey(it)) }
                if (progress != null && progress > 0) {
                    setProgress(100, progress.coerceIn(0, 100), false)
                }
                addAction(
                    R.drawable.ic_notification_music,
                    "Otwórz",
                    openAppIntent()
                )
                addAction(
                    R.drawable.ic_notification_music,
                    "Anuluj",
                    cancelIntent()
                )
            }
            .build()

        return ForegroundInfo(
            childNotificationId(),
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    suspend fun updatePlaylistSummary() {
        val groupId = playlistGroupId ?: return
        val items = dao.getPlaylistItems(groupId)
        if (items.isEmpty()) return

        ensureChannel()
        val total = items.firstNotNullOfOrNull { it.playlistTotal } ?: items.size
        val completed = items.count { it.state == DownloadState.COMPLETED }
        val failed = items.count { it.state == DownloadState.FAILED }
        val remaining = (total - completed - failed).coerceAtLeast(0)
        val running = items.filter {
            it.state in setOf(
                DownloadState.PREPARING,
                DownloadState.DOWNLOADING,
                DownloadState.COPYING
            )
        }
        val progress = if (total == 0) 0 else {
            items.sumOf { item ->
                when (item.state) {
                    DownloadState.COMPLETED, DownloadState.FAILED -> 100
                    else -> item.progress
                }
            }.div(total).coerceIn(0, 100)
        }
        val playlistTitle = items.firstNotNullOfOrNull { it.playlistTitle }
            ?: "Playlista YouTube"
        val currentText = running.take(2).joinToString("\n") { item ->
            "${item.playlistPosition ?: "–"}/$total • ${item.title}"
        }
        val summaryText = if (remaining == 0) {
            if (failed == 0) {
                "Gotowe — zapisano $completed utworów"
            } else {
                "Zapisano $completed z $total • Nieudane: $failed"
            }
        } else {
            "Pobrano $completed z $total • Pozostało $remaining"
        }
        val expandedText = listOf(summaryText, currentText.takeIf { it.isNotBlank() })
            .filterNotNull()
            .joinToString("\n")

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_music)
            .setColor(BRAND_COLOR)
            .setContentTitle(playlistTitle)
            .setContentText(summaryText)
            .setSubText("TuneRide • Playlista")
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setContentIntent(openAppIntent())
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setGroup(groupKey(groupId))
            .setGroupSummary(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(remaining > 0)
            .setAutoCancel(remaining == 0)
            .apply {
                if (remaining > 0 && progress > 0) {
                    setProgress(100, progress, false)
                }
                addAction(
                    R.drawable.ic_notification_music,
                    "Otwórz",
                    openAppIntent()
                )
                if (remaining > 0) {
                    addAction(
                        R.drawable.ic_notification_music,
                        "Anuluj",
                        cancelIntent()
                    )
                }
            }
            .build()

        manager.notify(summaryNotificationId(groupId), notification)
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(
            context,
            10_000 + childNotificationId(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancelIntent(): PendingIntent {
        val workTag = playlistGroupId?.let { "playlist:$it" } ?: itemId
        val intent = Intent(context, DownloadActionReceiver::class.java).apply {
            putExtra(DownloadActionReceiver.EXTRA_WORK_TAG, workTag)
            putExtra(DownloadActionReceiver.EXTRA_ITEM_ID, itemId)
            putExtra(DownloadActionReceiver.EXTRA_PLAYLIST_ID, playlistGroupId)
        }
        return PendingIntent.getBroadcast(
            context,
            20_000 + childNotificationId(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun childNotificationId(): Int = itemId.hashCode() and Int.MAX_VALUE

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.download_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Postęp pobierania i konwersji muzyki"
                    setShowBadge(false)
                }
            )
        }
    }

    private fun formatEta(seconds: Long): String? {
        if (seconds <= 0) return null
        return if (seconds < 60) {
            "około ${seconds}s"
        } else {
            val minutes = seconds / 60
            val rest = seconds % 60
            if (rest == 0L) "około ${minutes} min" else "około ${minutes}m ${rest}s"
        }
    }

    private fun DownloadState.isActive(): Boolean = this in setOf(
        DownloadState.QUEUED,
        DownloadState.PREPARING,
        DownloadState.DOWNLOADING,
        DownloadState.COPYING
    )

    companion object {
        const val CHANNEL_ID = "roadtune_downloads"
        private val BRAND_COLOR = Color.rgb(255, 89, 103)

        fun groupKey(playlistId: String): String = "tuneride_playlist_$playlistId"

        fun summaryNotificationId(playlistId: String): Int =
            (playlistId.hashCode() xor 0x5A17) and Int.MAX_VALUE
    }
}
