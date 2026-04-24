package com.cloudhaus.sensorapp.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Straw exercise has no sensor input — purely animation-driven.
 * Frame sequencing is triggered by virtual blow button.
 */
class StrawEngine(
    private val scope: CoroutineScope,
) : ExerciseEngine {

    private val _state = MutableStateFlow<ExerciseState>(ExerciseState.Idle)
    override val state: StateFlow<ExerciseState> = _state.asStateFlow()
    private var blowCount = 0
    private val targetBlows = 5

    override fun start(calibration: CalibrationData?) {
        _state.value = ExerciseState.Active(AnimationState.Straw(StrawFrame.Idle))
    }

    override fun stop() {}

    override fun onVirtualBlow() {
        blowCount++
        scope.launch {
            // Shoot sequence
            _state.value = ExerciseState.Active(AnimationState.Straw(StrawFrame.Suck1))
            delay(300)
            _state.value = ExerciseState.Active(AnimationState.Straw(StrawFrame.Suck2))
            delay(300)
            _state.value = ExerciseState.Active(AnimationState.Straw(StrawFrame.Shoot1))
            delay(300)
            _state.value = ExerciseState.Active(AnimationState.Straw(StrawFrame.Shoot2))
            delay(300)
            if (blowCount >= targetBlows) {
                _state.value = ExerciseState.Complete(
                    ExerciseResultData(success = true, breathCount = blowCount)
                )
            } else {
                _state.value = ExerciseState.Active(AnimationState.Straw(StrawFrame.Idle))
            }
        }
    }
}
