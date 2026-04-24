package com.cloudhaus.sensorapp.ui.animation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cloudhaus.sensorapp.engine.ColorZone
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import com.cloudhaus.sensorapp.resources.Res
import com.cloudhaus.sensorapp.resources.*

@Composable
fun TissueAnimation(
    colorZone: ColorZone,
    modifier: Modifier = Modifier,
) {
    val frames = remember {
        listOf(Res.drawable.tissue_1, Res.drawable.tissue_2, Res.drawable.tissue_3)
    }
    var currentFrame by remember { mutableIntStateOf(0) }

    val frameDelayMs = when (colorZone) {
        ColorZone.Green -> 400L
        ColorZone.Orange -> 300L
        ColorZone.Red -> 100L
    }

    LaunchedEffect(colorZone) {
        while (true) {
            delay(frameDelayMs)
            currentFrame = (currentFrame + 1) % frames.size
        }
    }

    Box(
        modifier = modifier.fillMaxWidth().height(300.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(frames[currentFrame]),
            contentDescription = "Tissue",
            modifier = Modifier.height(250.dp),
        )
    }
}
