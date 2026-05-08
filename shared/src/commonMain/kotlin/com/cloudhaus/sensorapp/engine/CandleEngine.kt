package com.cloudhaus.sensorapp.engine

import com.cloudhaus.sensorapp.pipeline.BreathDetector
import com.cloudhaus.sensorapp.pipeline.BreathEvent
import com.cloudhaus.sensorapp.sensor.SensorSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CandleEngine(
    private val breathDetector: BreathDetector,
    private val difficulty: Int = 0,
    private val scope: CoroutineScope,
) : ExerciseEngine {

    private val _state = MutableStateFlow<ExerciseState>(ExerciseState.Idle)
    override val state: StateFlow<ExerciseState> = _state.asStateFlow()
    private var job: Job? = null

    private var targetPressure = 30.0
    private var maxCapacity = 0.0
    private var flameSize = 1f

    override fun start(calibration: CalibrationData?) {
        val maxExhale = calibration?.maxExhale ?: 0.5
        val basicPressure = maxExhale * 0.6
        targetPressure = basicPressure + basicPressure * (difficulty / 100.0)

        _state.value = ExerciseState.Active(AnimationState.Candle())

        job = scope.launch {
            breathDetector.breathFlow.collect { event ->
                val pressureDiff = when (event) {
                    is BreathEvent.Exhale -> event.velocity
                    is BreathEvent.Inhale -> 0.0
                    is BreathEvent.Idle -> 0.0
                }

                if (pressureDiff > maxCapacity) maxCapacity = pressureDiff

                val colorZone = when {
                    pressureDiff >= targetPressure * 0.7 && pressureDiff <= targetPressure * 1.2 -> ColorZone.Green
                    pressureDiff >= targetPressure * 0.4 -> ColorZone.Orange
                    else -> ColorZone.Red
                }

                if (colorZone == ColorZone.Red && pressureDiff > 8) {
                    flameSize = (flameSize - 0.002f).coerceAtLeast(0.3f)
                }

                val isBlownOut = flameSize <= 0.35f

                _state.value = ExerciseState.Active(
                    AnimationState.Candle(
                        isBlownOut = isBlownOut,
                        flameScale = flameSize,
                        colorZone = colorZone,
                        pressure = pressureDiff.toFloat(),
                    )
                )

                if (isBlownOut) {
                    val success = maxCapacity > targetPressure * 0.7
                    _state.value = ExerciseState.Complete(
                        ExerciseResultData(success = success, capacity = maxCapacity)
                    )
                    job?.cancel()
                }
            }
        }
    }

    override fun stop() { job?.cancel() }

    override fun onVirtualIntensity(intensity: Float) {
        // Cancel the breath-detector loop so it stops overwriting `pressure`
        // back to 0 between slider events; from here the slider is the sole
        // writer to _state.
        job?.cancel(); job = null
        // The Candle composable runs its own flame-erosion sim driven by
        // breath, so the engine only has to publish the slider value as
        // pressure. The composable handles guttering, snuffing, and relight
        // via its `gutterStart` / `snuffStart` / `relightDelay` config.
        val clamped = intensity.coerceIn(0f, 100f)
        _state.value = ExerciseState.Active(
            AnimationState.Candle(
                isBlownOut = false,
                flameScale = 1f,
                colorZone = ColorZone.Green,
                pressure = clamped,
            )
        )
    }

    override fun onVirtualBlow() {
        job?.cancel(); job = null
        flameSize = (flameSize - 0.15f).coerceAtLeast(0f)
        val isBlownOut = flameSize <= 0.1f
        _state.value = ExerciseState.Active(
            AnimationState.Candle(
                isBlownOut = isBlownOut,
                flameScale = flameSize,
                colorZone = ColorZone.Green,
                pressure = 50f,
            )
        )
        if (isBlownOut) {
            _state.value = ExerciseState.Complete(
                ExerciseResultData(success = true, capacity = 50.0)
            )
        }
    }
}
