package com.cloudhaus.sensorapp.engine

import kotlinx.coroutines.flow.StateFlow

data class CalibrationData(
    val maxInhale: Double = 0.5,
    val maxExhale: Double = 0.5,
    val inhaleTimeMs: Long = 3000,
    val exhaleTimeMs: Long = 3000,
)

sealed class ExerciseState {
    data object Idle : ExerciseState()
    data class Active(val animationState: AnimationState) : ExerciseState()
    data class Complete(val result: ExerciseResultData) : ExerciseState()
}

data class ExerciseResultData(
    val success: Boolean,
    val capacity: Double? = null,
    val inhale: Double? = null,
    val exhale: Double? = null,
    val breathCount: Int? = null,
    val timeInGreenMs: Long? = null,
)

interface ExerciseEngine {
    val state: StateFlow<ExerciseState>
    fun start(calibration: CalibrationData? = null)
    fun stop()
    fun onVirtualBlow()
}
