package com.cloudhaus.sensorapp.ui.exercise

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudhaus.sensorapp.viewmodel.SensorViewModel
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

private enum class Phase { Instructions, Countdown, Active, Complete }

@Composable
fun ExerciseScreen(
    exerciseName: String,
    unitId: Int,
    viewModel: SensorViewModel,
    onFinish: () -> Unit,
) {
    var phase by remember { mutableStateOf(Phase.Instructions) }
    var countdown by remember { mutableIntStateOf(3) }
    var pressure by remember { mutableFloatStateOf(0f) }
    var breathCount by remember { mutableIntStateOf(0) }

    // Mock sensor loop
    LaunchedEffect(phase) {
        if (phase == Phase.Active) {
            while (phase == Phase.Active) {
                val time = System.currentTimeMillis() / 1000.0
                pressure = (30 + 20 * sin(time * 2)).toFloat()
                delay(100)
            }
        }
    }

    // Countdown
    LaunchedEffect(phase) {
        if (phase == Phase.Countdown) {
            countdown = 3
            repeat(3) {
                delay(1000)
                countdown--
            }
            phase = Phase.Active
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when (phase) {
            Phase.Instructions -> InstructionsContent(
                exerciseName = exerciseName,
                onStart = { phase = Phase.Countdown },
            )
            Phase.Countdown -> CountdownContent(countdown = countdown)
            Phase.Active -> ActiveContent(
                exerciseName = exerciseName,
                pressure = pressure,
                breathCount = breathCount,
                onBlow = {
                    breathCount++
                    pressure = Random.nextFloat() * 35 + 60
                },
                onFinish = {
                    viewModel.recordCompletion(exerciseName, unitId)
                    phase = Phase.Complete
                },
            )
            Phase.Complete -> CompleteContent(
                breathCount = breathCount,
                capacity = pressure,
                onDone = onFinish,
            )
        }
    }
}

@Composable
private fun InstructionsContent(exerciseName: String, onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            exerciseIcon(exerciseName),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Get ready for $exerciseName",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            exerciseDescription(exerciseName),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Start Exercise", modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
private fun CountdownContent(countdown: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "$countdown",
            fontSize = 120.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Get ready...",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActiveContent(
    exerciseName: String,
    pressure: Float,
    breathCount: Int,
    onBlow: () -> Unit,
    onFinish: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(exerciseName, style = MaterialTheme.typography.titleLarge)

        // Pressure ring
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
            val primary = MaterialTheme.colorScheme.primary
            val track = MaterialTheme.colorScheme.surfaceVariant
            val sweepAngle = (pressure / 100f).coerceIn(0f, 1f) * 360f

            Canvas(modifier = Modifier.size(200.dp)) {
                val strokeWidth = 12.dp.toPx()
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                drawArc(
                    color = track,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
                drawArc(
                    color = primary,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    String.format("%.1f", pressure),
                    style = MaterialTheme.typography.displaySmall,
                )
                Text(
                    "pressure",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text("Breaths: $breathCount", style = MaterialTheme.typography.titleMedium)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onBlow,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.Air, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Blow", modifier = Modifier.padding(vertical = 8.dp))
            }
            TextButton(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Finish")
            }
        }
    }
}

@Composable
private fun CompleteContent(breathCount: Int, capacity: Float, onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Exercise Complete!", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Capacity", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(String.format("%.1f", capacity), style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Breaths", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$breathCount", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Done", modifier = Modifier.padding(vertical = 8.dp))
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

private fun exerciseDescription(name: String): String = when (name) {
    "Candle" -> "Blow steadily to extinguish the candle flame."
    "Windmill" -> "Blow to spin the windmill blades as fast as you can."
    "Tissue" -> "Blow the tissue as far as possible."
    "Dandelion" -> "Blow the dandelion seeds away with a sustained breath."
    "Boat" -> "Blow to propel the boat across the water."
    "MIE" -> "Breathe in and out as deeply as you can."
    else -> "Follow the breathing pattern shown on screen."
}
