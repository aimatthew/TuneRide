package pl.rysiek.roadtune.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("roadtune_settings")

enum class ThemeMode { LIGHT, MIDNIGHT, DARK_RED }

data class AppSettings(
    val folderUri: String? = null,
    val folderName: String = "Muzyka/TuneRide",
    val bitrate: Int = 192,
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val maxConcurrentDownloads: Int = 2
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val folderUri = stringPreferencesKey("folder_uri")
        val folderName = stringPreferencesKey("folder_name")
        val bitrate = intPreferencesKey("bitrate")
        val themeMode = stringPreferencesKey("theme_mode")
        val maxConcurrentDownloads = intPreferencesKey("max_concurrent_downloads")
    }

    val settings = context.settingsDataStore.data.map { preferences ->
        AppSettings(
            folderUri = preferences[Keys.folderUri],
            folderName = preferences[Keys.folderName] ?: "Muzyka/TuneRide",
            bitrate = preferences[Keys.bitrate] ?: 192,
            themeMode = preferences[Keys.themeMode]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.LIGHT,
            maxConcurrentDownloads = (preferences[Keys.maxConcurrentDownloads] ?: 2).coerceIn(1, 4)
        )
    }

    suspend fun setFolder(uri: String, name: String) {
        context.settingsDataStore.edit {
            it[Keys.folderUri] = uri
            it[Keys.folderName] = name
        }
    }

    suspend fun setBitrate(value: Int) {
        context.settingsDataStore.edit { it[Keys.bitrate] = value }
    }

    suspend fun setThemeMode(value: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.themeMode] = value.name }
    }

    suspend fun setMaxConcurrentDownloads(value: Int) {
        context.settingsDataStore.edit { it[Keys.maxConcurrentDownloads] = value.coerceIn(1, 4) }
    }
}
