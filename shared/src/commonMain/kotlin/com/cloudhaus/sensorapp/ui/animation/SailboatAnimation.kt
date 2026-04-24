package com.cloudhaus.sensorapp.ui.animation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import com.cloudhaus.sensorapp.resources.Res
import com.cloudhaus.sensorapp.resources.*

@Composable
fun SailboatAnimation(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val maxOffset = 200.dp
    val boatOffset by animateDpAsState(
        targetValue = maxOffset * progress,
        animationSpec = tween(durationMillis = 300),
    )

    Box(
        modifier = modifier.fillMaxWidth().height(300.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Image(
            painter = painterResource(Res.drawable.sailboat_ocean),
            contentDescription = "Ocean",
            modifier = Modifier.fillMaxWidth().height(120.dp).align(Alignment.BottomCenter),
        )
        Image(
            painter = painterResource(Res.drawable.sailboat_boat),
            contentDescription = "Boat",
            modifier = Modifier
                .size(80.dp)
                .offset(x = 20.dp + boatOffset, y = (-80).dp),
        )
    }
}
