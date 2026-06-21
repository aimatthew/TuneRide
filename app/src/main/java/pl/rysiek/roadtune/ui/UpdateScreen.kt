package pl.rysiek.roadtune.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.rysiek.roadtune.BuildConfig
import pl.rysiek.roadtune.update.UpdateRepository
import pl.rysiek.roadtune.update.UpdateUiState

@Composable
fun UpdateScreen(
    padding: PaddingValues,
    state: UpdateUiState,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    "Aktualizacje",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "TuneRide ${BuildConfig.VERSION_NAME}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        when (state) {
                            UpdateUiState.Checking -> CircularProgressIndicator(Modifier.size(34.dp))
                            UpdateUiState.UpToDate -> Icon(
                                Icons.Default.CheckCircle,
                                null,
                                modifier = Modifier.size(38.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            is UpdateUiState.Error -> Icon(
                                Icons.Default.ErrorOutline,
                                null,
                                modifier = Modifier.size(38.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            is UpdateUiState.Available,
                            is UpdateUiState.Downloading,
                            is UpdateUiState.Downloaded -> Icon(
                                Icons.Default.CloudDownload,
                                null,
                                modifier = Modifier.size(38.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            UpdateUiState.Idle -> Icon(
                                Icons.Default.SystemUpdate,
                                null,
                                modifier = Modifier.size(38.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    UpdateStatus(state)
                    Spacer(Modifier.height(18.dp))

                    when (state) {
                        is UpdateUiState.Available -> Button(
                            onClick = onDownload,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Pobierz wersję ${state.release.versionName}") }

                        is UpdateUiState.Downloading -> {
                            LinearProgressIndicator(
                                progress = { state.progress / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("${state.progress}%")
                        }

                        is UpdateUiState.Downloaded -> Button(
                            onClick = onInstall,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Zainstaluj aktualizację") }

                        UpdateUiState.Checking -> Unit
                        else -> Button(
                            onClick = onCheck,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Sprawdź aktualizacje") }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("Bezpieczna instalacja", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "APK jest pobierany wyłącznie z oficjalnego repozytorium TuneRide. " +
                            "Android sprawdza podpis i zawsze poprosi Cię o potwierdzenie instalacji.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/${UpdateRepository.REPOSITORY}/releases")
                                )
                            )
                        }
                    ) {
                        Icon(Icons.Default.OpenInNew, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Otwórz wydania na GitHubie")
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateStatus(state: UpdateUiState) {
    when (state) {
        UpdateUiState.Idle -> {
            Text("Sprawdź, czy jest nowa wersja", fontWeight = FontWeight.Bold)
            Text("Automatyczne sprawdzanie odbywa się raz dziennie.")
        }
        UpdateUiState.Checking -> Text("Sprawdzanie GitHuba…", fontWeight = FontWeight.Bold)
        UpdateUiState.UpToDate -> {
            Text("Masz najnowszą wersję", fontWeight = FontWeight.Bold)
            Text("Nie musisz niczego robić.")
        }
        is UpdateUiState.Available -> {
            Text("Dostępna wersja ${state.release.versionName}", fontWeight = FontWeight.Bold)
            if (state.release.notes.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(state.release.notes, maxLines = 6)
            }
        }
        is UpdateUiState.Downloading ->
            Text("Pobieranie wersji ${state.release.versionName}", fontWeight = FontWeight.Bold)
        is UpdateUiState.Downloaded -> {
            Text("Aktualizacja jest gotowa", fontWeight = FontWeight.Bold)
            Text("Android poprosi o potwierdzenie instalacji.")
        }
        is UpdateUiState.Error -> {
            Text("Nie udało się sprawdzić aktualizacji", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(state.message, color = MaterialTheme.colorScheme.error)
        }
    }
}
