package com.cloudhaus.sensorapp.ui.animation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cloudhaus.sensorapp.engine.AnimationState
import com.cloudhaus.sensorapp.ui.common.PressureIndicator

@Composable
fun ExerciseAnimationRouter(
    animationState: AnimationState,
    modifier: Modifier = Modifier,
) {
    when (animationState) {
        is AnimationState.Candle -> CandleAnimation(
            isBlownOut = animationState.isBlownOut,
            flameScale = animationState.flameScale,
            colorZone = animationState.colorZone,
            modifier = modifier,
        )
        is AnimationState.Windmill -> WindmillAnimation(
            rotationSpeed = animationState.rotationSpeed,
            modifier = modifier,
        )
        is AnimationState.Tissue -> TissueAnimation(
            colorZone = animationState.colorZone,
            modifier = modifier,
        )
        is AnimationState.Dandelion -> DandelionAnimation(
            stage = animationState.stage,
            modifier = modifier,
        )
        is AnimationState.Sailboat -> SailboatAnimation(
            progress = animationState.progress,
            modifier = modifier,
        )
        is AnimationState.Straw -> StrawAnimation(
            frame = animationState.frame,
            modifier = modifier,
        )
        is AnimationState.CountingBreaths -> CountingBreathsDisplay(
            currentCount = animationState.currentCount,
            totalCount = animationState.totalCount,
            breathPhase = animationState.breathPhase,
            modifier = modifier,
        )
        is AnimationState.TimedBreaths -> TimedBreathsDisplay(
            phase = animationState.phase,
            remainingSeconds = animationState.remainingSeconds,
            currentRepetition = animationState.currentRepetition,
            totalRepetitions = animationState.totalRepetitions,
            modifier = modifier,
        )
    }
}
