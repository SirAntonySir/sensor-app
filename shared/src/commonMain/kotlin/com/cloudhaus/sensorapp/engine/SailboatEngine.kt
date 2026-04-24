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

class SailboatEngine(
    private val breathDetector: BreathDetector,
    private val difficulty: Int = 0,
    private val scope: CoroutineScope,
) : ExerciseEngine {

    private val _state = MutableStateFlow<ExerciseState>(ExerciseState.Idle)
    override val state: StateFlow<ExerciseState> = _state.asStateFlow()
    private var job: Job? = null

    private val exerciseDurationMs = 6000L
    private var targetCount = 6
    private var targetPressure = 15.0
    private var blowCount = 0
    private var canCount = true
    private var lastPeak = 0.0
    private var startTime = 0L

    override fun start(calibration: CalibrationData?) {
        val maxExhale = calibration?.maxExhale ?: 0.5
        targetPressure = maxExhale * 0.5 * 100  // convert to Pa-like scale
        targetCount = ((exerciseDurationMs / 1000) * (100 + difficulty) / 100).toInt().coerceAtLeast(1)
        startTime = currentTimeMillis()

        _state.value = ExerciseState.Active(
            AnimationState.Sailboat(0f, 0, targetCount)
        )

        job = scope.launch {
            breathDetector.breathFlow.collect { event ->
                val elapsed = currentTimeMillis() - startTime
                val pressureDiff = when (event) {
                    is BreathEvent.Exhale -> event.velocity
                    else -> 0.0
                }

                if (pressureDiff > targetPressure && canCount) {
                    blowCount++
                    canCount = false
                    lastPeak = pressureDiff
                }

                if (!canCount && pressureDiff <= lastPeak * 0.75) {
                    canCount = true
                }

                val progress = (blowCount.toFloat() / targetCount).coerceAtMost(1f)
                _state.value = ExerciseState.Active(
                    AnimationState.Sailboat(progress, blowCount, targetCount)
                )

                if (elapsed >= exerciseDurationMs) {
                    val success = blowCount >= targetCount
                    _state.value = ExerciseState.Complete(
                        ExerciseResultData(success = success, breathCount = blowCount)
                    )
                    job?.cancel()
                }
            }
        }
    }

    override fun stop() { job?.cancel() }

    override fun onVirtualBlow() {
        blowCount++
        val progress = (blowCount.toFloat() / targetCount).coerceAtMost(1f)
        _state.value = ExerciseState.Active(
            AnimationState.Sailboat(progress, blowCount, targetCount)
        )
        if (blowCount >= targetCount) {
            _state.value = ExerciseState.Complete(
                ExerciseResultData(success = true, breathCount = blowCount)
            )
        }
    }
}
