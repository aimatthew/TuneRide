package pl.rysiek.roadtune.download

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.rysiek.roadtune.RoadTuneApplication

class DownloadActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val workTag = intent.getStringExtra(EXTRA_WORK_TAG) ?: return
        val itemId = intent.getStringExtra(EXTRA_ITEM_ID)
        val playlistId = intent.getStringExtra(EXTRA_PLAYLIST_ID)
        val pendingResult = goAsync()

        WorkManager.getInstance(context).cancelAllWorkByTag(workTag)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = (context.applicationContext as RoadTuneApplication).database.downloads()
                if (!playlistId.isNullOrBlank()) {
                    dao.cancelActivePlaylist(playlistId, "Anulowano przez użytkownika")
                    context.getSystemService(NotificationManager::class.java).cancel(
                        DownloadNotifications.summaryNotificationId(playlistId)
                    )
                } else if (!itemId.isNullOrBlank()) {
                    dao.cancelActiveItem(itemId, "Anulowano przez użytkownika")
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_WORK_TAG = "work_tag"
        const val EXTRA_ITEM_ID = "item_id"
        const val EXTRA_PLAYLIST_ID = "playlist_id"
    }
}
