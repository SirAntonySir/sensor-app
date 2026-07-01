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
import com.cloudhaus.sensorapp.BuildConfig
import com.cloudhaus.sensorapp.pipeline.BreathDetector
import com.cloudhaus.sensorapp.sensor.LoggingSensorSource
import com.cloudhaus.sensorapp.sensor.SensorLogger
import com.cloudhaus.sensorapp.sensor.SensorSourceProvider
import com.cloudhaus.sensorapp.settings.AndroidAppSettings
import com.cloudhaus.sensorapp.ui.animation.ExerciseAnimationRouter
import com.cloudhaus.sensorapp.viewmodel.SensorViewModel
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

private enum class ScreenPhase { Instructions, Countdown, Active, Complete }

@Composable
fun ExerciseScreen(
    exerciseName: String,
    unitId: Int,
    viewModel: SensorViewModel,
    onFinish: () -> Unit,
) {
    val provider = koinInject<SensorSourceProvider>()
    val settings = koinInject<AndroidAppSettings>()
    val forceMock by settings.forceMockSensor.collectAsState()

    // No real barometer and not forced to mock: don't fake an exercise.
    if (!provider.barometerAvailable && !forceMock) {
        BarometerUnavailableContent(
            allowMock = BuildConfig.DEBUG,
            onUseMock = { settings.setForceMockSensor(true) },
            onBack = onFinish,
        )
        return
    }

    ExerciseRunner(exerciseName, unitId, viewModel, provider, forceMock, onFinish)
}

@Composable
private fun ExerciseRunner(
    exerciseName: String,
    unitId: Int,
    viewModel: SensorViewModel,
    provider: SensorSourceProvider,
    forceMock: Boolean,
    onFinish: () -> Unit,
) {
    val exerciseType = exerciseTypeFromName(exerciseName)
    val scope = rememberCoroutineScope()
    val logger = koinInject<SensorLogger>()

    val sensor = remember {
        val label = if (!forceMock && provider.barometerAvailable) "barometer" else "mock"
        LoggingSensorSource(provider.create(forceMock), logger, label, exerciseName)
            .also { it.start() }
    }
    val breathDetector = remember { BreathDetector(sensor) }

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
            sensor.stop()
        }
    }

    // Remember the last Active animation state so the visual can keep
    // playing after the engine has flipped to Complete (or after the user
    // hits Finish, which freezes the engine state).
    var lastAnim by remember { mutableStateOf<AnimationState?>(null) }
    LaunchedEffect(engineState) {
        (engineState as? ExerciseState.Active)?.let { lastAnim = it.animationState }
    }
    val displayedAnim = (engineState as? ExerciseState.Active)?.animationState ?: lastAnim

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background animation layer — call site is stable across Active
            // and Complete so seed/frame state survives the phase change.
            val showAnim = screenPhase == ScreenPhase.Active || screenPhase == ScreenPhase.Complete
            if (showAnim && displayedAnim != null) {
                ExerciseAnimationRouter(displayedAnim, Modifier.fillMaxSize())
            }

            when (screenPhase) {
                ScreenPhase.Instructions -> InstructionsContent(exerciseName) { screenPhase = ScreenPhase.Countdown }
                ScreenPhase.Countdown -> CountdownContent(countdown)
                ScreenPhase.Active -> ActiveOverlay(
                    exerciseName = exerciseName,
                    useIntensitySlider = exerciseName in setOf("Dandelion", "Candle", "Windmill", "FloatBall"),
                    onIntensity = { engine.onVirtualIntensity(it) },
                    onBlow = { engine.onVirtualBlow() },
                )
                ScreenPhase.Complete -> CompleteOverlay(
                    result = (engineState as? ExerciseState.Complete)?.result,
                    onDone = onFinish,
                )
            }
        }
    }
}

@Composable
private fun BarometerUnavailableContent(
    allowMock: Boolean,
    onUseMock: () -> Unit,
    onBack: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Default.SensorsOff, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(24.dp))
            Text("Barometer unavailable", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(
                "This device has no pressure sensor, so breathing exercises can't be driven by the barometer.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(48.dp))
            if (allowMock) {
                Button(onClick = onUseMock, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text("Continue with simulated sensor", Modifier.padding(vertical = 8.dp))
                }
                Spacer(Modifier.height(12.dp))
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text("Back", Modifier.padding(vertical = 8.dp))
            }
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
private fun ActiveOverlay(
    exerciseName: String,
    useIntensitySlider: Boolean,
    onIntensity: (Float) -> Unit,
    onBlow: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (useIntensitySlider) {
                var intensity by remember { mutableFloatStateOf(0f) }
                Text(
                    "Intensity: ${intensity.toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = intensity,
                    onValueChange = {
                        intensity = it
                        onIntensity(it)
                    },
                    valueRange = 0f..100f,
                )
            } else {
                Button(onClick = onBlow, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Air, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Blow", Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun CompleteOverlay(result: ExerciseResultData?, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Card {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(12.dp))
                Text("Exercise Complete!", style = MaterialTheme.typography.headlineSmall)
                if (result != null) {
                    Spacer(Modifier.height(16.dp))
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        result.capacity?.let { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Capacity", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${it.toInt()}", style = MaterialTheme.typography.titleMedium) } }
                        result.breathCount?.let { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Breaths", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("$it", style = MaterialTheme.typography.titleMedium) } }
                        result.timeInGreenMs?.let { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Time in zone", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${it / 1000}s", style = MaterialTheme.typography.titleMedium) } }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Result", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(if (result.success) "Success" else "Try again", style = MaterialTheme.typography.titleMedium) }
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Text("Done", Modifier.padding(vertical = 8.dp))
        }
    }
}

private fun exerciseTypeFromName(name: String) = when (name) {
    "Candle" -> ExerciseType.Candle
    "Windmill" -> ExerciseType.Windmill
    "Dandelion" -> ExerciseType.Dandelion
    "FloatBall" -> ExerciseType.FloatBall
    "MIE" -> ExerciseType.CountingBreaths
    else -> ExerciseType.TimedBreaths
}

private fun configForExercise(name: String): StepConfig = when {
    name.contains("-") -> {
        val parts = name.split("-").mapNotNull { it.toIntOrNull() }
        StepConfig(inhaleDuration = parts.getOrNull(0) ?: 4, holdDuration = parts.getOrNull(1) ?: 0, exhaleDuration = parts.getOrNull(2) ?: 4, breathCount = 5)
    }
    else -> StepConfig(breathCount = 5)
}

private fun exerciseIcon(name: String) = when (name) {
    "Candle" -> Icons.Default.LocalFireDepartment
    "Windmill" -> Icons.Default.Air
    "Dandelion" -> Icons.Default.Spa
    "FloatBall" -> Icons.Default.Speed
    "MIE" -> Icons.Default.MonitorHeart
    else -> Icons.Default.Timer
}

private fun exerciseDescription(name: String) = when (name) {
    "Candle" -> "Blow steadily to extinguish the candle flame."
    "Windmill" -> "Blow to spin the windmill blades as fast as you can."
    "Dandelion" -> "Blow the dandelion seeds away with a sustained breath."
    "FloatBall" -> "Sustain a steady breath to keep the ball floating at the top."
    "MIE" -> "Breathe in and out as deeply as you can."
    else -> "Follow the breathing pattern shown on screen."
}
