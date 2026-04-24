package com.cloudhaus.sensorapp.engine

import com.cloudhaus.sensorapp.pipeline.BreathDetector
import com.cloudhaus.sensorapp.pipeline.BreathEvent
import com.cloudhaus.sensorapp.sensor.BarometerThresholds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DandelionEngine(
    private val breathDetector: BreathDetector,
    private val difficulty: Int = 0,
    private val scope: CoroutineScope,
) : ExerciseEngine {

    private val _state = MutableStateFlow<ExerciseState>(ExerciseState.Idle)
    override val state: StateFlow<ExerciseState> = _state.asStateFlow()
    private var job: Job? = null

    private var targetPressure = 30.0
    private var firstBlowCapacity = 0.0
    private var secondBlowCapacity = 0.0
    private var firstBlowDone = false

    override fun start(calibration: CalibrationData?) {
        val maxExhale = calibration?.maxExhale ?: 0.5
        val basicPressure = maxExhale * 0.6
        targetPressure = basicPressure + basicPressure * (difficulty / 100.0)

        _state.value = ExerciseState.Active(AnimationState.Dandelion())

        job = scope.launch {
            var maxPressure = 0.0
            var blowDetected = false

            breathDetector.breathFlow.collect { event ->
                val pressureDiff = when (event) {
                    is BreathEvent.Exhale -> event.velocity
                    else -> 0.0
                }

                if (pressureDiff > BarometerThresholds.MINIMUM_TOLERANCE * 100) {
                    blowDetected = true
                    if (pressureDiff > maxPressure) maxPressure = pressureDiff
                } else if (blowDetected && pressureDiff < maxPressure * 0.5) {
                    // Blow ended
                    if (!firstBlowDone) {
                        firstBlowCapacity = maxPressure
                        firstBlowDone = true
                        _state.value = ExerciseState.Active(
                            AnimationState.Dandelion(DandelionStage.Blow, maxPressure.toFloat())
                        )
                        delay(750)
                        _state.value = ExerciseState.Active(
                            AnimationState.Dandelion(DandelionStage.Partial, maxPressure.toFloat())
                        )
                        maxPressure = 0.0
                        blowDetected = false
                    } else {
                        secondBlowCapacity = maxPressure
                        _state.value = ExerciseState.Active(
                            AnimationState.Dandelion(DandelionStage.FullBlown, maxPressure.toFloat())
                        )
                        delay(1500)
                        val totalCapacity = firstBlowCapacity + secondBlowCapacity
                        val success = totalCapacity > targetPressure * 0.7
                        _state.value = ExerciseState.Complete(
                            ExerciseResultData(success = success, capacity = totalCapacity)
                        )
                        job?.cancel()
                    }
                }
            }
        }
    }

    override fun stop() { job?.cancel() }

    override fun onVirtualBlow() {
        if (!firstBlowDone) {
            firstBlowCapacity = 40.0
            firstBlowDone = true
            _state.value = ExerciseState.Active(
                AnimationState.Dandelion(DandelionStage.Blow, 40f)
            )
            scope.launch {
                delay(750)
                _state.value = ExerciseState.Active(
                    AnimationState.Dandelion(DandelionStage.Partial, 40f)
                )
            }
        } else {
            secondBlowCapacity = 45.0
            _state.value = ExerciseState.Active(
                AnimationState.Dandelion(DandelionStage.FullBlown, 45f)
            )
            scope.launch {
                delay(1500)
                _state.value = ExerciseState.Complete(
                    ExerciseResultData(success = true, capacity = 85.0)
                )
            }
        }
    }
}
