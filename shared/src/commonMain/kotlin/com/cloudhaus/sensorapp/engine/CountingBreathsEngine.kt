package com.cloudhaus.sensorapp.engine

import com.cloudhaus.sensorapp.pipeline.BreathDetector
import com.cloudhaus.sensorapp.pipeline.BreathEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CountingBreathsEngine(
    private val breathDetector: BreathDetector,
    private val totalCount: Int = 5,
    private val scope: CoroutineScope,
) : ExerciseEngine {

    private val _state = MutableStateFlow<ExerciseState>(ExerciseState.Idle)
    override val state: StateFlow<ExerciseState> = _state.asStateFlow()
    private var job: Job? = null
    private var currentCount = 0
    private var totalInhale = 0.0
    private var totalExhale = 0.0

    override fun start(calibration: CalibrationData?) {
        currentCount = 0
        _state.value = ExerciseState.Active(
            AnimationState.CountingBreaths(0, totalCount, BreathPhase.Inhale)
        )

        job = scope.launch {
            repeat(totalCount) {
                // Wait for inhale
                _state.value = ExerciseState.Active(
                    AnimationState.CountingBreaths(currentCount, totalCount, BreathPhase.Inhale)
                )
                val inhaleEvent = breathDetector.breathFlow.first { it is BreathEvent.Inhale }
                totalInhale += (inhaleEvent as BreathEvent.Inhale).velocity

                // Wait for exhale
                _state.value = ExerciseState.Active(
                    AnimationState.CountingBreaths(currentCount, totalCount, BreathPhase.Exhale)
                )
                val exhaleEvent = breathDetector.breathFlow.first { it is BreathEvent.Exhale }
                totalExhale += (exhaleEvent as BreathEvent.Exhale).velocity

                currentCount++
                _state.value = ExerciseState.Active(
                    AnimationState.CountingBreaths(currentCount, totalCount, BreathPhase.Inhale)
                )
            }

            _state.value = ExerciseState.Complete(
                ExerciseResultData(
                    success = true,
                    inhale = totalInhale,
                    exhale = totalExhale,
                    breathCount = totalCount,
                )
            )
        }
    }

    override fun stop() { job?.cancel() }

    override fun onVirtualBlow() {
        currentCount++
        if (currentCount >= totalCount) {
            _state.value = ExerciseState.Complete(
                ExerciseResultData(success = true, breathCount = totalCount)
            )
        } else {
            _state.value = ExerciseState.Active(
                AnimationState.CountingBreaths(currentCount, totalCount, BreathPhase.Inhale)
            )
        }
    }
}
