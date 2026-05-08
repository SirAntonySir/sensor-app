package com.cloudhaus.sensorapp.ui.platform

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.cloudhaus.sensorapp.engine.ExerciseState
import com.cloudhaus.sensorapp.ui.animation.ExerciseAnimationRouter
import kotlinx.coroutines.flow.StateFlow
import platform.UIKit.UIViewController

@OptIn(androidx.compose.runtime.ExperimentalComposeApi::class)
fun ExerciseAnimationViewController(
    engineStateFlow: StateFlow<ExerciseState>,
): UIViewController = ComposeUIViewController(
    configure = {
        enforceStrictPlistSanityCheck = false
        opaque = false
    }
) {
    val engineState by engineStateFlow.collectAsState()
    when (val state = engineState) {
        is ExerciseState.Active -> ExerciseAnimationRouter(
            animationState = state.animationState,
            modifier = Modifier.fillMaxSize(),
        )
        else -> {}
    }
}
