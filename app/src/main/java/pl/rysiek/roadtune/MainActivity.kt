package pl.rysiek.roadtune

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.rysiek.roadtune.data.ThemeMode
import pl.rysiek.roadtune.ui.RoadTuneApp
import pl.rysiek.roadtune.ui.RoadTuneTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        receiveSharedText(intent)
        receiveUpdateIntent(intent)

        setContent {
            val appSettings by viewModel.settings.collectAsStateWithLifecycle()
            LaunchedEffect(appSettings.themeMode) {
                val lightBars = appSettings.themeMode == ThemeMode.LIGHT
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = lightBars
                    isAppearanceLightNavigationBars = lightBars
                }
            }
            RoadTuneTheme(themeMode = appSettings.themeMode) {
                val notificationPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { }

                LaunchedEffect(Unit) {
                    if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                RoadTuneApp(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receiveSharedText(intent)
        receiveUpdateIntent(intent)
    }

    private fun receiveSharedText(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let(viewModel::setUrl)
        }
    }

    private fun receiveUpdateIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_UPDATES, false) == true) {
            viewModel.requestUpdatesScreen()
        }
    }

    companion object {
        const val EXTRA_OPEN_UPDATES = "open_updates"
    }
}
