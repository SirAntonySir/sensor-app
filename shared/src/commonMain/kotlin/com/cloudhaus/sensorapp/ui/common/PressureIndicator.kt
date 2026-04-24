package com.cloudhaus.sensorapp.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudhaus.sensorapp.engine.ColorZone

@Composable
fun PressureIndicator(
    pressure: Float,
    maxPressure: Float = 100f,
    colorZone: ColorZone = ColorZone.Red,
    modifier: Modifier = Modifier,
) {
    val normalized = (pressure / maxPressure).coerceIn(0f, 1f)
    val animatedValue by animateFloatAsState(
        targetValue = normalized,
        animationSpec = tween(durationMillis = 200),
    )
    val zoneColor = when (colorZone) {
        ColorZone.Green -> Color(0xFF4CAF50)
        ColorZone.Orange -> Color(0xFFFF9800)
        ColorZone.Red -> Color(0xFFF44336)
    }
    val trackColor = zoneColor.copy(alpha = 0.15f)

    Box(modifier = modifier.size(180.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(160.dp)) {
            val strokeWidth = 10.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = zoneColor,
                startAngle = 135f,
                sweepAngle = animatedValue * 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${pressure.toInt()}",
                fontSize = 32.sp,
                color = zoneColor,
            )
            Text("pressure", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
