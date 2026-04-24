package com.cloudhaus.sensorapp.ui.exercise

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudhaus.sensorapp.engine.*
import com.cloudhaus.sensorapp.model.ExerciseType
import com.cloudhaus.sensorapp.model.StepConfig
import com.cloudhaus.sensorapp.pipeline.BreathDetector
import com.cloudhaus.sensorapp.sensor.MockSensorSource
import com.cloudhaus.sensorapp.ui.animation.ExerciseAnimationRouter
import com.cloudhaus.sensorapp.viewmodel.SensorViewModel
import kotlinx.coroutines.delay

private enum class ScreenPhase { Instructions, Countdown, Active, Complete }

@Composable
fun ExerciseScreen(
    exerciseName: String,
    unitId: Int,
    viewModel: SensorViewModel,
    onFinish: () -> Unit,
) {
    val exerciseType = exerciseTypeFromName(exerciseName)
    val scope = rememberCoroutineScope()

    val mockSensor = remember { MockSensorSource().also { it.start() } }
    val breathDetector = remember { BreathDetector(mockSensor) }

    val engine = remember {
        ExerciseEngineFactory.create(
            type = exerciseType,
            breathDetector = breathDetector,
            config = configForExercise(exerciseName),
            scope = scope,
        )
    }

    val engineState by engine.state.collectAsState()
    var screenPhase by remember { mutableStateOf(ScreenPhase.Instructions) }
    var countdown by remember { mutableIntStateOf(3) }

    LaunchedEffect(screenPhase) {
        if (screenPhase == ScreenPhase.Countdown) {
            countdown = 3
            repeat(3) {
                delay(1000)
                countdown--
            }
            engine.start()
            screenPhase = ScreenPhase.Active
        }
    }

    LaunchedEffect(engineState) {
        if (engineState is ExerciseState.Complete) {
            viewModel.recordCompletion(exerciseName, unitId)
            screenPhase = ScreenPhase.Complete
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            engine.stop()
            mockSensor.stop()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (screenPhase) {
            ScreenPhase.Instructions -> InstructionsContent(exerciseName) { screenPhase = ScreenPhase.Countdown }
            ScreenPhase.Countdown -> CountdownContent(countdown)
            ScreenPhase.Active -> ActiveContent(
                exerciseName = exerciseName,
                engineState = engineState,
                onBlow = { engine.onVirtualBlow() },
                onFinish = {
                    engine.stop()
                    viewModel.recordCompletion(exerciseName, unitId)
                    screenPhase = ScreenPhase.Complete
                },
            )
            ScreenPhase.Complete -> CompleteContent(
                result = (engineState as? ExerciseState.Complete)?.result,
                onDone = onFinish,
            )
        }
    }
}

@Composable
private fun InstructionsContent(exerciseName: String, onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(exerciseIcon(exerciseName), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(80.dp))
        Spacer(Modifier.height(24.dp))
        Text("Get ready for $exerciseName", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(exerciseDescription(exerciseName), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(48.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Text("Start Exercise", Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
private fun CountdownContent(countdown: Int) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("$countdown", fontSize = 120.sp, color = MaterialTheme.colorScheme.primary)
        Text("Get ready...", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ActiveContent(exerciseName: String, engineState: ExerciseState, onBlow: () -> Unit, onFinish: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(exerciseName, style = MaterialTheme.typography.titleLarge)

        when (val active = engineState) {
            is ExerciseState.Active -> RiveExerciseAnimation(active.animationState, Modifier.weight(1f))
            else -> Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onBlow, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Air, null)
                Spacer(Modifier.width(8.dp))
                Text("Blow", Modifier.padding(vertical = 8.dp))
            }
            TextButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("Finish") }
        }
    }
}

@Composable
private fun CompleteContent(result: ExerciseResultData?, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(80.dp))
        Spacer(Modifier.height(16.dp))
        Text("Exercise Complete!", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))
        if (result != null) {
            Card {
                Column(Modifier.padding(16.dp)) {
                    result.capacity?.let { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Capacity", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${it.toInt()}", style = MaterialTheme.typography.titleMedium) } }
                    result.breathCount?.let { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Breaths", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("$it", style = MaterialTheme.typography.titleMedium) } }
                    result.timeInGreenMs?.let { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Time in zone", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${it / 1000}s", style = MaterialTheme.typography.titleMedium) } }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Result", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(if (result.success) "Success" else "Try again", style = MaterialTheme.typography.titleMedium) }
                }
            }
        }
        Spacer(Modifier.height(48.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Text("Done", Modifier.padding(vertical = 8.dp))
        }
    }
}

private fun exerciseTypeFromName(name: String) = when (name) {
    "Candle" -> ExerciseType.Candle; "Windmill" -> ExerciseType.Windmill; "Tissue" -> ExerciseType.Tissue
    "Dandelion" -> ExerciseType.Dandelion; "Boat" -> ExerciseType.Boat; "Straw" -> ExerciseType.Straw
    "MIE" -> ExerciseType.CountingBreaths; else -> ExerciseType.TimedBreaths
}

private fun configForExercise(name: String): StepConfig = when {
    name.contains("-") -> {
        val parts = name.split("-").mapNotNull { it.toIntOrNull() }
        StepConfig(inhaleDuration = parts.getOrNull(0) ?: 4, holdDuration = parts.getOrNull(1) ?: 0, exhaleDuration = parts.getOrNull(2) ?: 4, breathCount = 5)
    }
    else -> StepConfig(breathCount = 5)
}

private fun exerciseIcon(name: String) = when (name) {
    "Candle" -> Icons.Default.LocalFireDepartment; "Windmill" -> Icons.Default.Air; "Tissue" -> Icons.Default.Description
    "Dandelion" -> Icons.Default.Spa; "Boat" -> Icons.Default.Sailing; "MIE" -> Icons.Default.MonitorHeart
    else -> Icons.Default.Timer
}

private fun exerciseDescription(name: String) = when (name) {
    "Candle" -> "Blow steadily to extinguish the candle flame."
    "Windmill" -> "Blow to spin the windmill blades as fast as you can."
    "Tissue" -> "Blow the tissue as far as possible."
    "Dandelion" -> "Blow the dandelion seeds away with a sustained breath."
    "Boat" -> "Blow to propel the boat across the water."
    "MIE" -> "Breathe in and out as deeply as you can."
    else -> "Follow the breathing pattern shown on screen."
}
