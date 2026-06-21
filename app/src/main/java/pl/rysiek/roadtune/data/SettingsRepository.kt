package pl.rysiek.roadtune.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("roadtune_settings")

data class AppSettings(
    val folderUri: String? = null,
    val folderName: String = "Muzyka/TuneRide",
    val bitrate: Int = 192
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val folderUri = stringPreferencesKey("folder_uri")
        val folderName = stringPreferencesKey("folder_name")
        val bitrate = intPreferencesKey("bitrate")
    }

    val settings = context.settingsDataStore.data.map { preferences ->
        AppSettings(
            folderUri = preferences[Keys.folderUri],
            folderName = preferences[Keys.folderName] ?: "Muzyka/TuneRide",
            bitrate = preferences[Keys.bitrate] ?: 192
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
}
