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
import com.cloudhaus.sensorapp.engine.StrawFrame
import org.jetbrains.compose.resources.painterResource
import com.cloudhaus.sensorapp.resources.Res
import com.cloudhaus.sensorapp.resources.*

@Composable
fun StrawAnimation(
    frame: StrawFrame,
    modifier: Modifier = Modifier,
) {
    val drawable = remember(frame) {
        when (frame) {
            StrawFrame.Idle -> Res.drawable.straw_shoot_1
            StrawFrame.Shoot1 -> Res.drawable.straw_shoot_1
            StrawFrame.Shoot2 -> Res.drawable.straw_shoot_2
            StrawFrame.Suck1 -> Res.drawable.straw_suck_1
            StrawFrame.Suck2 -> Res.drawable.straw_suck_2
            StrawFrame.Suck3 -> Res.drawable.straw_suck_3
        }
    }

    Box(
        modifier = modifier.fillMaxWidth().height(300.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.straw_paperball),
            contentDescription = "Paperball",
            modifier = Modifier.size(60.dp).align(Alignment.TopCenter),
        )
        Crossfade(targetState = drawable, animationSpec = tween(200)) { res ->
            Image(
                painter = painterResource(res),
                contentDescription = "Straw",
                modifier = Modifier.height(200.dp),
            )
        }
    }
}
