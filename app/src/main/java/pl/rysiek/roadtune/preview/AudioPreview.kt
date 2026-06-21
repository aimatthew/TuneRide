package pl.rysiek.roadtune.preview

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest

data class PreviewTrack(
    val title: String,
    val uploader: String?,
    val thumbnailUrl: String?,
    val streamUrl: String,
    val httpHeaders: Map<String, String>
)

sealed interface PreviewState {
    data object Idle : PreviewState
    data object Loading : PreviewState
    data class Ready(val track: PreviewTrack) : PreviewState
    data class Error(val message: String) : PreviewState
}

class AudioPreviewRepository(private val context: Context) {
    fun load(sourceUrl: String): PreviewTrack {
        updateYoutubeDlIfNeeded()
        val request = YoutubeDLRequest(sourceUrl).apply {
            addOption("--no-playlist")
            addOption("--no-warnings")
            addOption("-f", "bestaudio[ext=m4a]/bestaudio")
        }
        val info = YoutubeDL.getInstance().getInfo(request)
        val streamUrl = info.url?.takeIf { it.isNotBlank() }
            ?: error("Nie udało się znaleźć strumienia audio dla tego filmu")

        return PreviewTrack(
            title = info.title?.trim().takeUnless { it.isNullOrBlank() } ?: "Podgląd utworu",
            uploader = info.uploader?.trim().takeUnless { it.isNullOrBlank() },
            thumbnailUrl = info.thumbnail,
            streamUrl = streamUrl,
            httpHeaders = info.httpHeaders.orEmpty()
        )
    }

    private fun updateYoutubeDlIfNeeded() {
        val preferences = context.getSharedPreferences(
            "roadtune_engine_updates",
            Context.MODE_PRIVATE
        )
        val now = System.currentTimeMillis()
        if (now - preferences.getLong("last_update", 0L) < 12 * 60 * 60 * 1000L) return

        runCatching {
            YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.NIGHTLY)
        }.onSuccess {
            preferences.edit().putLong("last_update", now).apply()
        }
    }
}
