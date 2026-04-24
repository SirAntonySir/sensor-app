package com.cloudhaus.sensorapp.ui.animation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudhaus.sensorapp.engine.BreathPhase

@Composable
fun CountingBreathsDisplay(
    currentCount: Int,
    totalCount: Int,
    breathPhase: BreathPhase,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().height(300.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (breathPhase == BreathPhase.Inhale) "Inhale" else "Exhale",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "$currentCount",
            fontSize = 72.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "of $totalCount breaths",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
