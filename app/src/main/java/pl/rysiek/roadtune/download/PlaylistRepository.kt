package pl.rysiek.roadtune.download

import android.content.Context
import android.net.Uri
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import org.json.JSONArray
import org.json.JSONObject

data class PlaylistTrack(
    val videoId: String,
    val title: String,
    val uploader: String?,
    val thumbnailUrl: String?,
    val sourceUrl: String,
    val position: Int
)

data class PlaylistDetails(
    val title: String,
    val tracks: List<PlaylistTrack>
)

class PlaylistRepository(private val context: Context) {
    fun load(sourceUrl: String): PlaylistDetails {
        updateYoutubeDlIfNeeded()
        val isYoutubeMix = isYoutubeMix(sourceUrl)
        val request = YoutubeDLRequest(sourceUrl).apply {
            addOption("--flat-playlist")
            addOption("--dump-single-json")
            addOption("--skip-download")
            addOption("--yes-playlist")
            addOption("--no-warnings")
            addOption("--ignore-errors")
            if (isYoutubeMix) {
                addOption("--playlist-end", "25")
            }
        }
        val output = YoutubeDL.getInstance().execute(request).out.trim()
        val root = JSONObject(output)
        val entries = root.optJSONArray("entries") ?: JSONArray()
        val rawTracks = buildList {
            for (index in 0 until entries.length()) {
                val entry = entries.optJSONObject(index) ?: continue
                val id = entry.optString("id").takeIf { it.isNotBlank() } ?: continue
                val title = entry.optString("title").takeIf { it.isNotBlank() }
                    ?: "Utwór ${index + 1}"
                val rawUrl = entry.optString("webpage_url").takeIf { it.startsWith("http") }
                    ?: entry.optString("url").takeIf { it.startsWith("http") }
                add(
                    PlaylistTrack(
                        videoId = id,
                        title = title,
                        uploader = entry.optString("uploader").takeIf { it.isNotBlank() }
                            ?: entry.optString("channel").takeIf { it.isNotBlank() },
                        thumbnailUrl = findThumbnail(entry),
                        sourceUrl = rawUrl ?: "https://www.youtube.com/watch?v=$id",
                        position = index + 1
                    )
                )
            }
        }
        val tracks = rawTracks
            .distinctBy { it.videoId }
            .let { if (isYoutubeMix) it.take(25) else it }
            .mapIndexed { index, track -> track.copy(position = index + 1) }

        check(tracks.isNotEmpty()) { "Playlista nie zawiera dostępnych utworów" }
        return PlaylistDetails(
            title = root.optString("title").takeIf { it.isNotBlank() } ?: "Playlista YouTube",
            tracks = tracks
        )
    }

    private fun isYoutubeMix(sourceUrl: String): Boolean {
        val uri = runCatching { Uri.parse(sourceUrl) }.getOrNull() ?: return false
        return uri.getQueryParameter("list")?.startsWith("RD") == true
    }

    private fun findThumbnail(entry: JSONObject): String? {
        entry.optString("thumbnail").takeIf { it.startsWith("http") }?.let { return it }
        val thumbnails = entry.optJSONArray("thumbnails") ?: return null
        for (index in thumbnails.length() - 1 downTo 0) {
            val url = thumbnails.optJSONObject(index)?.optString("url")
            if (!url.isNullOrBlank() && url.startsWith("http")) return url
        }
        return null
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
