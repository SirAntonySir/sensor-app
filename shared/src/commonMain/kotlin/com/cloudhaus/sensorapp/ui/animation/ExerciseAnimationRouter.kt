package com.cloudhaus.sensorapp.ui.animation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cloudhaus.sensorapp.engine.AnimationState

/**
 * Top-level animation dispatcher. Timed/counted breath exercises render their
 * own dedicated UI; everything else routes through the breathanim rig system.
 */
@Composable
fun ExerciseAnimationRouter(
    animationState: AnimationState,
    modifier: Modifier = Modifier,
) {
    when (animationState) {
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
        else -> BreathRigView(animationState, modifier)
    }
}
