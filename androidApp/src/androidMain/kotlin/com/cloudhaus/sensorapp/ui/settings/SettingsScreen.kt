package com.cloudhaus.sensorapp.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.cloudhaus.sensorapp.BuildConfig
import com.cloudhaus.sensorapp.sensor.SensorLogger
import com.cloudhaus.sensorapp.settings.AndroidAppSettings
import org.koin.compose.koinInject
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val logger = koinInject<SensorLogger>()
    val settings = koinInject<AndroidAppSettings>()

    val forceMock by settings.forceMockSensor.collectAsState()
    // Recomputed after clear/exercises so the size + enabled state stay fresh.
    var logSize by remember { mutableStateOf(logger.sizeBytes()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Sensor data", style = MaterialTheme.typography.titleMedium)
            Text(
                if (logSize > 0) "Logged ${logSize / 1024} KB of readings" else "No sensor data logged yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = { shareLog(context, logger.filePath()) },
                enabled = logSize > 0,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Download sensor data")
            }

            OutlinedButton(
                onClick = { logger.clear(); logSize = logger.sizeBytes() },
                enabled = logSize > 0,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Clear log")
            }

            if (BuildConfig.DEBUG) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text("Developer", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Force simulated sensor")
                        Text(
                            "Use the mock breathing signal even if a barometer is present",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = forceMock,
                        onCheckedChange = { settings.setForceMockSensor(it) },
                    )
                }
            }
        }
    }
}

private fun shareLog(context: android.content.Context, path: String?) {
    val file = path?.let { File(it) } ?: return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Sensor data"))
}
