package pl.rysiek.roadtune.download

enum class DownloadMode {
    SINGLE,
    PLAYLIST
}

data class PlaylistPrompt(
    val canSelectSingleTrack: Boolean
)

data class PlaylistSelection(
    val details: PlaylistDetails,
    val selectedVideoIds: Set<String>
)
