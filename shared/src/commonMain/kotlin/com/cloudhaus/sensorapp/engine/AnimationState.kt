package com.cloudhaus.sensorapp.engine

enum class ColorZone { Red, Orange, Green }
enum class BreathPhase { Inhale, Exhale }
enum class TimedBreathPhase { Inhale, Hold, Exhale, Wait }
enum class RotationSpeed(val durationMs: Int) {
    Slow(2500), Medium(1000), Fast(300)
}
enum class DandelionStage { Still, Blow, Partial, FullBlown }

sealed class AnimationState {

    data class Candle(
        val isBlownOut: Boolean = false,
        val flameScale: Float = 1f,
        val colorZone: ColorZone = ColorZone.Red,
        val pressure: Float = 0f,
    ) : AnimationState()

    data class Windmill(
        val rotationSpeed: RotationSpeed = RotationSpeed.Slow,
        val colorZone: ColorZone = ColorZone.Red,
        val pressure: Float = 0f,
    ) : AnimationState()

    data class Dandelion(
        val stage: DandelionStage = DandelionStage.Still,
        val pressure: Float = 0f,
    ) : AnimationState()

    data class FloatBall(
        val pressure: Float = 0f,
        /** Increment to fire a one-shot lift burst on the rig. */
        val burstId: Int = 0,
    ) : AnimationState()

    data class CountingBreaths(
        val currentCount: Int = 0,
        val totalCount: Int = 5,
        val breathPhase: BreathPhase = BreathPhase.Inhale,
    ) : AnimationState()

    data class TimedBreaths(
        val phase: TimedBreathPhase = TimedBreathPhase.Inhale,
        val remainingSeconds: Int = 0,
        val currentRepetition: Int = 0,
        val totalRepetitions: Int = 5,
    ) : AnimationState()
}
