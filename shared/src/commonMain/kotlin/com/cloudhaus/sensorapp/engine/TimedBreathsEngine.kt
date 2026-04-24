package com.cloudhaus.sensorapp.engine

import com.cloudhaus.sensorapp.model.StepConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimedBreathsEngine(
    private val config: StepConfig,
    private val totalRepetitions: Int = 5,
    private val scope: CoroutineScope,
) : ExerciseEngine {

    private val _state = MutableStateFlow<ExerciseState>(ExerciseState.Idle)
    override val state: StateFlow<ExerciseState> = _state.asStateFlow()
    private var job: Job? = null

    private val inhaleSeconds = config.inhaleDuration ?: 4
    private val exhaleSeconds = config.exhaleDuration ?: 4
    private val holdSeconds = config.holdDuration ?: 0

    override fun start(calibration: CalibrationData?) {
        _state.value = ExerciseState.Active(
            AnimationState.TimedBreaths(TimedBreathPhase.Inhale, inhaleSeconds, 0, totalRepetitions)
        )

        job = scope.launch {
            repeat(totalRepetitions) { rep ->
                // Inhale phase
                for (sec in inhaleSeconds downTo 1) {
                    _state.value = ExerciseState.Active(
                        AnimationState.TimedBreaths(TimedBreathPhase.Inhale, sec, rep, totalRepetitions)
                    )
                    delay(1000)
                }

                // Hold phase (if configured)
                if (holdSeconds > 0) {
                    for (sec in holdSeconds downTo 1) {
                        _state.value = ExerciseState.Active(
                            AnimationState.TimedBreaths(TimedBreathPhase.Hold, sec, rep, totalRepetitions)
                        )
                        delay(1000)
                    }
                }

                // Exhale phase
                for (sec in exhaleSeconds downTo 1) {
                    _state.value = ExerciseState.Active(
                        AnimationState.TimedBreaths(TimedBreathPhase.Exhale, sec, rep, totalRepetitions)
                    )
                    delay(1000)
                }

                // Brief wait between reps
                if (rep < totalRepetitions - 1) {
                    _state.value = ExerciseState.Active(
                        AnimationState.TimedBreaths(TimedBreathPhase.Wait, 1, rep, totalRepetitions)
                    )
                    delay(1000)
                }
            }

            _state.value = ExerciseState.Complete(
                ExerciseResultData(success = true, breathCount = totalRepetitions)
            )
        }
    }

    override fun stop() { job?.cancel() }

    override fun onVirtualBlow() {
        // No-op for timed breaths — timer-driven
    }
}
