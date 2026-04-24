package com.cloudhaus.sensorapp.ui.unit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.cloudhaus.sensorapp.viewmodel.SensorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitDetailScreen(
    unitId: Int,
    viewModel: SensorViewModel,
    onExerciseClick: (String) -> Unit,
    onBack: () -> Unit,
) {
    val units by viewModel.units.collectAsState()
    val unit = units.find { it.id == unitId } ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(unit.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(unit.exercises) { exerciseName ->
                val completions = viewModel.completionCount(exerciseName, unitId)
                ExerciseRow(
                    name = exerciseName,
                    completions = completions,
                    onClick = { onExerciseClick(exerciseName) },
                )
            }
        }
    }
}

@Composable
private fun ExerciseRow(
    name: String,
    completions: Int,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                exerciseIcon(name),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "$completions / 5 completions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            LinearProgressIndicator(
                progress = { minOf(completions / 5f, 1f) },
                modifier = Modifier.width(60.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

private fun exerciseIcon(name: String): ImageVector = when (name) {
    "Candle" -> Icons.Default.LocalFireDepartment
    "Windmill" -> Icons.Default.Air
    "Tissue" -> Icons.Default.Description
    "Dandelion" -> Icons.Default.Spa
    "Boat" -> Icons.Default.Sailing
    "MIE" -> Icons.Default.MonitorHeart
    else -> Icons.Default.Timer
}
