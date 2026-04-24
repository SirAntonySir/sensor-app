package com.cloudhaus.sensorapp.ui.exercise

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Alignment as RiveAlignment
import app.rive.runtime.kotlin.core.Fit
import com.cloudhaus.sensorapp.engine.AnimationState
import com.cloudhaus.sensorapp.engine.ColorZone
import com.cloudhaus.sensorapp.engine.RotationSpeed

/**
 * Maps exercise AnimationState to Rive animation inputs.
 * Falls back to the shared Compose animation if no .riv file exists.
 */
@Composable
fun RiveExerciseAnimation(
    animationState: AnimationState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val riveResId = getRiveResource(animationState, context)

    if (riveResId != null) {
        RiveAnimationPlayer(
            resId = riveResId,
            animationState = animationState,
            modifier = modifier,
        )
    } else {
        // Fallback to shared Compose animation
        com.cloudhaus.sensorapp.ui.animation.ExerciseAnimationRouter(
            animationState = animationState,
            modifier = modifier,
        )
    }
}

@Composable
private fun RiveAnimationPlayer(
    resId: Int,
    animationState: AnimationState,
    modifier: Modifier = Modifier,
) {
    var riveView by remember { mutableStateOf<RiveAnimationView?>(null) }

    // Update Rive inputs when animation state changes
    LaunchedEffect(animationState) {
        riveView?.let { view ->
            applyStateToRive(view, animationState)
        }
    }

    Box(
        modifier = modifier.fillMaxWidth().height(320.dp),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { ctx ->
                RiveAnimationView(ctx).apply {
                    setRiveResource(resId)
                    riveView = this
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }

    DisposableEffect(Unit) {
        onDispose { riveView = null }
    }
}

private fun applyStateToRive(view: RiveAnimationView, state: AnimationState) {
    try {
        when (state) {
            is AnimationState.Candle -> {
                // Input: blowForce (0-100), maps from pressure
                view.setNumberState("State Machine 1", "blowForce", state.pressure.coerceIn(0f, 100f))
            }
            is AnimationState.Windmill -> {
                // Input: speed (0-100)
                val speed = when (state.rotationSpeed) {
                    RotationSpeed.Slow -> 10f
                    RotationSpeed.Medium -> 50f
                    RotationSpeed.Fast -> 90f
                }
                view.setNumberState("State Machine 1", "speed", speed)
            }
            is AnimationState.Tissue -> {
                // Input: blowForce (0-100)
                view.setNumberState("State Machine 1", "blowForce", state.pressure.coerceIn(0f, 100f))
            }
            is AnimationState.Dandelion -> {
                // Input: stage (0-3)
                view.setNumberState("State Machine 1", "stage", state.stage.ordinal.toFloat())
            }
            is AnimationState.Sailboat -> {
                // Input: progress (0-100)
                view.setNumberState("State Machine 1", "progress", state.progress * 100f)
            }
            else -> {} // CountingBreaths, TimedBreaths, Straw don't need Rive
        }
    } catch (_: Exception) {
        // State machine input not found — community .riv files may not have matching inputs.
        // This is expected for placeholder animations; custom .riv files should define these inputs.
    }
}

private fun getRiveResource(state: AnimationState, context: Context): Int? {
    val resName = when (state) {
        is AnimationState.Candle -> "candle"
        is AnimationState.Windmill -> "windmill"
        is AnimationState.Tissue -> "tissue"
        is AnimationState.Dandelion -> "dandelion"
        is AnimationState.Sailboat -> "sailboat"
        else -> return null // Text-based exercises don't need Rive
    }
    val resId = context.resources.getIdentifier(resName, "raw", context.packageName)
    return if (resId != 0) resId else null
}
