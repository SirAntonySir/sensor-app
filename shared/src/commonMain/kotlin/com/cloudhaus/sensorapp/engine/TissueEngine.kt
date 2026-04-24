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

class TissueEngine(
    private val breathDetector: BreathDetector,
    private val difficulty: Int = 0,
    private val scope: CoroutineScope,
) : ExerciseEngine {

    private val _state = MutableStateFlow<ExerciseState>(ExerciseState.Idle)
    override val state: StateFlow<ExerciseState> = _state.asStateFlow()
    private var job: Job? = null

    private var targetPressure = 10.0
    private var durationMs = 6000L
    private var greenTimeMs = 0L
    private var lastGreenTimestamp = 0L
    private var hasBlown = false
    private var startTime = 0L
    private var maxPressure = 0.0

    override fun start(calibration: CalibrationData?) {
        val maxExhale = calibration?.maxExhale ?: 0.5
        val exhaleTime = calibration?.exhaleTimeMs ?: 3000L
        val basicPressure = maxExhale * 0.2
        targetPressure = basicPressure + basicPressure * (difficulty / 100.0) * 0.5
        durationMs = exhaleTime * 2 + (exhaleTime * 2 * difficulty / 100)

        _state.value = ExerciseState.Active(AnimationState.Tissue())

        job = scope.launch {
            breathDetector.breathFlow.collect { event ->
                val pressureDiff = when (event) {
                    is BreathEvent.Exhale -> event.velocity
                    else -> 0.0
                }

                if (pressureDiff > 5 && !hasBlown) {
                    hasBlown = true
                    startTime = currentTimeMillis()
                }

                if (pressureDiff > maxPressure) maxPressure = pressureDiff

                val tolerance = targetPressure * 0.3
                val colorZone = when {
                    pressureDiff >= targetPressure - tolerance -> {
                        val now = currentTimeMillis()
                        if (lastGreenTimestamp > 0) greenTimeMs += now - lastGreenTimestamp
                        lastGreenTimestamp = now
                        ColorZone.Green
                    }
                    pressureDiff >= targetPressure - tolerance * 2 -> {
                        lastGreenTimestamp = 0
                        ColorZone.Orange
                    }
                    else -> {
                        lastGreenTimestamp = 0
                        ColorZone.Red
                    }
                }

                _state.value = ExerciseState.Active(
                    AnimationState.Tissue(colorZone, pressureDiff.toFloat())
                )

                if (hasBlown && currentTimeMillis() - startTime >= durationMs) {
                    val success = greenTimeMs >= durationMs / 2 && maxPressure > 0
                    _state.value = ExerciseState.Complete(
                        ExerciseResultData(success = success, capacity = maxPressure, timeInGreenMs = greenTimeMs)
                    )
                    job?.cancel()
                }
            }
        }
    }

    override fun stop() { job?.cancel() }

    override fun onVirtualBlow() {
        if (!hasBlown) {
            hasBlown = true
            startTime = currentTimeMillis()
        }
        _state.value = ExerciseState.Active(
            AnimationState.Tissue(ColorZone.Green, 50f)
        )
    }
}
