package com.cloudhaus.sensorapp.engine

import com.cloudhaus.sensorapp.pipeline.BreathDetector
import com.cloudhaus.sensorapp.pipeline.BreathEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives the FloatBall (incentive-spirometer-style) animation. The composable
 * runs its own ball physics from `breath`, so the engine just publishes
 * `pressure`. A virtual blow fires a one-shot `burst` so the rig can kick the
 * ball with extra lift.
 */
class FloatBallEngine(
    private val breathDetector: BreathDetector,
    private val difficulty: Int = 0,
    private val scope: CoroutineScope,
) : ExerciseEngine {

    private val _state = MutableStateFlow<ExerciseState>(ExerciseState.Idle)
    override val state: StateFlow<ExerciseState> = _state.asStateFlow()
    private var job: Job? = null

    override fun start(calibration: CalibrationData?) {
        _state.value = ExerciseState.Active(AnimationState.FloatBall())

        job = scope.launch {
            breathDetector.breathFlow.collect { event ->
                val pressure = when (event) {
                    is BreathEvent.Exhale -> event.velocity.toFloat()
                    else -> 0f
                }
                _state.value = ExerciseState.Active(
                    AnimationState.FloatBall(pressure = pressure)
                )
            }
        }
    }

    override fun stop() { job?.cancel() }

    override fun onVirtualIntensity(intensity: Float) {
        // Cancel the breath-detector loop so it stops overwriting `pressure`
        // back to 0 between slider events.
        job?.cancel(); job = null
        val clamped = intensity.coerceIn(0f, 100f)
        _state.value = ExerciseState.Active(
            AnimationState.FloatBall(pressure = clamped)
        )
    }

    override fun onVirtualBlow() {
        job?.cancel(); job = null
        // Each click bumps burstId so BreathRigView fires a fresh end trigger,
        // and bumps the pressure floor so the ball reacts visually.
        val current = (_state.value as? ExerciseState.Active)?.animationState as? AnimationState.FloatBall
        val pressure = (current?.pressure ?: 0f).coerceAtLeast(80f)
        val nextBurst = (current?.burstId ?: 0) + 1
        _state.value = ExerciseState.Active(
            AnimationState.FloatBall(pressure = pressure, burstId = nextBurst)
        )
    }
}
