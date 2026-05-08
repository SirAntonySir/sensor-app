package com.cloudhaus.sensorapp.ui.animation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cloudhaus.sensorapp.engine.AnimationState
import com.cloudhaus.sensorapp.engine.DandelionStage
import com.cloudhaus.sensorapp.engine.RotationSpeed
import com.cloudhaus.sensorapp.ui.animation.candle.Candle
import com.cloudhaus.sensorapp.ui.animation.dandelion.Dandelion
import com.cloudhaus.sensorapp.ui.animation.floatball.FloatBall
import com.cloudhaus.sensorapp.ui.animation.pinwheel.Pinwheel

/**
 * Routes [state] to the matching procedural Compose animation. Engines
 * normalize pressure to 0..100 — passed straight through as `breath`.
 *
 * Add new exercises here as they're written: one branch per AnimationState
 * subclass, calling its dedicated @Composable.
 */
@Composable
fun BreathRigView(state: AnimationState, modifier: Modifier = Modifier) {
    when (state) {
        is AnimationState.Dandelion -> {
            // Treat the FullBlown stage as an end-of-breath trigger, so the
            // animation stages a release burst even if breath alone wouldn't
            // push every seed past its detach threshold. Only fire on the
            // rising edge into FullBlown; LaunchedEffect's stage key handles
            // that for us.
            var endTrigger by remember { mutableIntStateOf(0) }
            LaunchedEffect(state.stage) {
                if (state.stage == DandelionStage.FullBlown) endTrigger++
            }
            Dandelion(
                breath = state.pressure,
                endTrigger = endTrigger,
                modifier = modifier,
            )
        }
        is AnimationState.Candle -> {
            // Treat the engine's "isBlownOut" flag as the rising-edge end
            // trigger, so the flame extinguishes + emits the smoke burst.
            var endTrigger by remember { mutableIntStateOf(0) }
            LaunchedEffect(state.isBlownOut) {
                if (state.isBlownOut) endTrigger++
            }
            Candle(
                breath = state.pressure,
                endTrigger = endTrigger,
                modifier = modifier,
            )
        }
        is AnimationState.Windmill -> {
            // Hitting the engine's Fast rotation tier (green zone) bumps the
            // gust trigger so the pinwheel's spin gets a reinforcing kick.
            var endTrigger by remember { mutableIntStateOf(0) }
            LaunchedEffect(state.rotationSpeed) {
                if (state.rotationSpeed == RotationSpeed.Fast) endTrigger++
            }
            Pinwheel(
                breath = state.pressure,
                endTrigger = endTrigger,
                modifier = modifier,
            )
        }
        is AnimationState.FloatBall -> {
            // The engine bumps burstId on each blow click; convert that into
            // the rig's endTrigger so the ball receives a lift impulse.
            var endTrigger by remember { mutableIntStateOf(0) }
            LaunchedEffect(state.burstId) {
                if (state.burstId > 0) endTrigger++
            }
            FloatBall(
                breath = state.pressure,
                endTrigger = endTrigger,
                modifier = modifier,
            )
        }
        else -> NoRigPlaceholder(modifier)
    }
}

@Composable
private fun NoRigPlaceholder(modifier: Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Animation not yet implemented",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
