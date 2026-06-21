package pl.rysiek.roadtune

import android.app.Application
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import pl.rysiek.roadtune.data.AppDatabase
import pl.rysiek.roadtune.data.SettingsRepository
import pl.rysiek.roadtune.update.UpdateScheduler

class RoadTuneApplication : Application() {
    val database by lazy { AppDatabase.create(this) }
    val settings by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        runCatching {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
        }.onFailure {
            Log.e("RoadTune", "Nie udało się uruchomić silnika pobierania", it)
        }
        UpdateScheduler.schedule(this)
    }
}
