package com.cloudhaus.sensorapp.ui.animation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cloudhaus.sensorapp.engine.DandelionStage
import org.jetbrains.compose.resources.painterResource
import com.cloudhaus.sensorapp.resources.Res
import com.cloudhaus.sensorapp.resources.*

@Composable
fun DandelionAnimation(
    stage: DandelionStage,
    modifier: Modifier = Modifier,
) {
    val stageDrawable = remember(stage) {
        when (stage) {
            DandelionStage.Still -> Res.drawable.dandelion_0
            DandelionStage.Blow -> Res.drawable.dandelion_1
            DandelionStage.Partial -> Res.drawable.dandelion_2
            DandelionStage.FullBlown -> Res.drawable.dandelion_3
        }
    }

    Box(
        modifier = modifier.fillMaxWidth().height(300.dp),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = stageDrawable, animationSpec = tween(500)) { drawable ->
            Image(
                painter = painterResource(drawable),
                contentDescription = "Dandelion",
                modifier = Modifier.height(250.dp),
            )
        }
    }
}
