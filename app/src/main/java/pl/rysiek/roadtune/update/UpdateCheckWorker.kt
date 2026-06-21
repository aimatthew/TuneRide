package pl.rysiek.roadtune.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.rysiek.roadtune.BuildConfig
import pl.rysiek.roadtune.MainActivity
import pl.rysiek.roadtune.R

class UpdateCheckWorker(
    appContext: Context,
    parameters: WorkerParameters
) : CoroutineWorker(appContext, parameters) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatching {
            UpdateRepository(applicationContext).check(BuildConfig.VERSION_NAME)
        }.fold(
            onSuccess = { release ->
                release?.let(::notifyAboutUpdate)
                Result.success()
            },
            onFailure = { Result.retry() }
        )
    }

    private fun notifyAboutUpdate(release: AppRelease) {
        val channelId = "tuneride_updates"
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Aktualizacje TuneRide",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            501,
            Intent(applicationContext, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_UPDATES, true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Nowa wersja TuneRide")
            .setContentText("Dostępna jest wersja ${release.versionName}")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(501, notification)
    }
}
