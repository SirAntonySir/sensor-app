package com.cloudhaus.sensorapp.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.cloudhaus.sensorapp.engine.RotationSpeed
import org.jetbrains.compose.resources.painterResource
import com.cloudhaus.sensorapp.resources.Res
import com.cloudhaus.sensorapp.resources.*

@Composable
fun WindmillAnimation(
    rotationSpeed: RotationSpeed,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = rotationSpeed.durationMs,
                easing = LinearEasing,
            ),
        ),
    )

    Box(
        modifier = modifier.fillMaxWidth().height(300.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.windmill_body),
            contentDescription = "Windmill body",
            modifier = Modifier.height(280.dp),
        )
        Image(
            painter = painterResource(Res.drawable.windmill_blades),
            contentDescription = "Windmill blades",
            modifier = Modifier
                .size(140.dp)
                .offset(y = (-60).dp)
                .rotate(rotation),
        )
    }
}
