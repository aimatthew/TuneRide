package pl.rysiek.roadtune.download

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.rysiek.roadtune.RoadTuneApplication

class DownloadActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val itemId = intent.getStringExtra(EXTRA_ITEM_ID)
        val playlistId = intent.getStringExtra(EXTRA_PLAYLIST_ID)
        val workManager = WorkManager.getInstance(context)
        when (action) {
            ACTION_CANCEL_ITEM -> {
                if (itemId.isNullOrBlank()) return
                workManager.cancelAllWorkByTag(itemId)
            }
            ACTION_CANCEL_PLAYLIST -> {
                if (playlistId.isNullOrBlank()) return
                workManager.cancelAllWorkByTag("playlist:$playlistId")
            }
            else -> return
        }
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = (context.applicationContext as RoadTuneApplication).database.downloads()
                val manager = context.getSystemService(NotificationManager::class.java)
                when (action) {
                    ACTION_CANCEL_ITEM -> {
                        val targetItemId = checkNotNull(itemId)
                        YoutubeDL.getInstance().destroyProcessById(targetItemId)
                        dao.cancelActiveItem(targetItemId, "Anulowano przez użytkownika")
                        manager.cancel(DownloadNotifications.itemNotificationId(targetItemId))
                        if (!playlistId.isNullOrBlank()) {
                            DownloadNotifications(context, dao, targetItemId, playlistId)
                                .updatePlaylistSummary()
                        }
                    }
                    ACTION_CANCEL_PLAYLIST -> {
                        val targetPlaylistId = checkNotNull(playlistId)
                        val playlistItems = dao.getPlaylistItems(targetPlaylistId)
                        playlistItems.forEach {
                            YoutubeDL.getInstance().destroyProcessById(it.id)
                        }
                        dao.cancelActivePlaylist(targetPlaylistId, "Anulowano przez użytkownika")
                        playlistItems.forEach {
                            manager.cancel(DownloadNotifications.itemNotificationId(it.id))
                        }
                        manager.cancel(DownloadNotifications.summaryNotificationId(targetPlaylistId))
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_CANCEL_ITEM = "pl.rysiek.roadtune.action.CANCEL_ITEM"
        const val ACTION_CANCEL_PLAYLIST = "pl.rysiek.roadtune.action.CANCEL_PLAYLIST"
        const val EXTRA_ITEM_ID = "item_id"
        const val EXTRA_PLAYLIST_ID = "playlist_id"
    }
}
