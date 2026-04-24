package com.cloudhaus.sensorapp.ui.animation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.cloudhaus.sensorapp.engine.ColorZone
import org.jetbrains.compose.resources.painterResource
import com.cloudhaus.sensorapp.resources.Res
import com.cloudhaus.sensorapp.resources.*

@Composable
fun CandleAnimation(
    isBlownOut: Boolean,
    flameScale: Float,
    colorZone: ColorZone,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val flicker by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
    )

    Box(
        modifier = modifier.fillMaxWidth().height(300.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Image(
            painter = painterResource(Res.drawable.candle_body),
            contentDescription = "Candle",
            modifier = Modifier.height(200.dp),
        )

        Crossfade(targetState = isBlownOut, modifier = Modifier.align(Alignment.TopCenter)) { blown ->
            if (blown) {
                Image(
                    painter = painterResource(Res.drawable.candle_smoke),
                    contentDescription = "Smoke",
                    modifier = Modifier.size(80.dp),
                )
            } else {
                Image(
                    painter = painterResource(Res.drawable.candle_flame),
                    contentDescription = "Flame",
                    modifier = Modifier
                        .size(80.dp)
                        .scale(flameScale * flicker),
                )
            }
        }
    }
}
