package pl.rysiek.roadtune.download

enum class DownloadMode {
    SINGLE,
    PLAYLIST
}

data class PlaylistPrompt(
    val canSelectSingleTrack: Boolean
)
