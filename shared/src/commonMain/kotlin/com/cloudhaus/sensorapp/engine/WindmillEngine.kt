package com.cloudhaus.sensorapp.engine

import com.cloudhaus.sensorapp.pipeline.BreathDetector
import com.cloudhaus.sensorapp.pipeline.BreathEvent
import com.cloudhaus.sensorapp.util.currentTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WindmillEngine(
    private val breathDetector: BreathDetector,
    private val difficulty: Int = 0,
    private val scope: CoroutineScope,
) : ExerciseEngine {

    private val _state = MutableStateFlow<ExerciseState>(ExerciseState.Idle)
    override val state: StateFlow<ExerciseState> = _state.asStateFlow()
    private var job: Job? = null

    private var targetPressure = 10.0
    private var durationMs = 15000L
    private var timeInGreenMs = 0L
    private var lastGreenTimestamp = 0L
    private var startTime = 0L

    override fun start(calibration: CalibrationData?) {
        val maxExhale = calibration?.maxExhale ?: 0.5
        val basicPressure = maxExhale * 0.3
        targetPressure = basicPressure + basicPressure * (difficulty / 100.0) * 0.5
        val basicTimer = 15000L
        durationMs = basicTimer + (basicTimer * difficulty / 100)
        startTime = currentTimeMillis()

        _state.value = ExerciseState.Active(AnimationState.Windmill())

        job = scope.launch {
            breathDetector.breathFlow.collect { event ->
                val elapsed = currentTimeMillis() - startTime
                val pressureDiff = when (event) {
                    is BreathEvent.Exhale -> event.velocity
                    else -> 0.0
                }

                val (colorZone, speed) = when {
                    pressureDiff >= targetPressure * 0.5 && pressureDiff <= targetPressure * 1.5 -> {
                        val now = currentTimeMillis()
                        if (lastGreenTimestamp > 0) timeInGreenMs += now - lastGreenTimestamp
                        lastGreenTimestamp = now
                        ColorZone.Green to RotationSpeed.Fast
                    }
                    pressureDiff >= targetPressure * 0.3 -> {
                        lastGreenTimestamp = 0
                        ColorZone.Orange to RotationSpeed.Medium
                    }
                    else -> {
                        lastGreenTimestamp = 0
                        ColorZone.Red to RotationSpeed.Slow
                    }
                }

                _state.value = ExerciseState.Active(
                    AnimationState.Windmill(speed, colorZone, pressureDiff.toFloat())
                )

                if (elapsed >= durationMs) {
                    val success = timeInGreenMs > durationMs / 2
                    _state.value = ExerciseState.Complete(
                        ExerciseResultData(success = success, timeInGreenMs = timeInGreenMs)
                    )
                    job?.cancel()
                }
            }
        }
    }

    override fun stop() { job?.cancel() }

    override fun onVirtualBlow() {
        _state.value = ExerciseState.Active(
            AnimationState.Windmill(RotationSpeed.Fast, ColorZone.Green, 50f)
        )
    }
}
