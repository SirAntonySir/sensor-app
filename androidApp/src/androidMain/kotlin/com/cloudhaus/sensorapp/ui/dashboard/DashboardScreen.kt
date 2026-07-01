package com.cloudhaus.sensorapp.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cloudhaus.sensorapp.viewmodel.SensorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: SensorViewModel,
    onUnitClick: (Int) -> Unit,
    onSettingsClick: () -> Unit,
) {
    val units by viewModel.units.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sensor App") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                HeaderSection()
            }
            itemsIndexed(units) { index, unit ->
                UnitCard(
                    unitName = unit.name,
                    exercises = unit.exercises,
                    medal = unit.medal,
                    isUnlocked = unit.isUnlocked,
                    unitNumber = unit.id,
                    onClick = { onUnitClick(unit.id) },
                    showLine = index < units.size - 1,
                )
            }
        }
    }
}

@Composable
private fun HeaderSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🫁", style = MaterialTheme.typography.displayMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Breathing Exercises",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            "Complete exercises to earn medals and unlock new units",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun UnitCard(
    unitName: String,
    exercises: List<String>,
    medal: String?,
    isUnlocked: Boolean,
    unitNumber: Int,
    onClick: () -> Unit,
    showLine: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isUnlocked) 1f else 0.5f),
    ) {
        // Timeline dot + line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(48.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = if (isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (medal != null) {
                        Text(medal, style = MaterialTheme.typography.titleMedium)
                    } else if (isUnlocked) {
                        Text(
                            "$unitNumber",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    } else {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Locked",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (showLine) {
                Surface(
                    modifier = Modifier
                        .width(2.dp)
                        .height(52.dp),
                    color = if (isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ) {}
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Card content
        Card(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(unitName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        exercises.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isUnlocked) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
