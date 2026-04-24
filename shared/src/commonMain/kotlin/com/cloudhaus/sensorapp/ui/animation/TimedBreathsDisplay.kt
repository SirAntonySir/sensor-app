package com.cloudhaus.sensorapp.ui.animation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cloudhaus.sensorapp.engine.TimedBreathPhase
import com.cloudhaus.sensorapp.ui.common.CountdownTimer

@Composable
fun TimedBreathsDisplay(
    phase: TimedBreathPhase,
    remainingSeconds: Int,
    currentRepetition: Int,
    totalRepetitions: Int,
    modifier: Modifier = Modifier,
) {
    val phaseColor = when (phase) {
        TimedBreathPhase.Inhale -> Color(0xFF4CAF50)
        TimedBreathPhase.Hold -> Color(0xFFFF9800)
        TimedBreathPhase.Exhale -> Color(0xFF2196F3)
        TimedBreathPhase.Wait -> Color(0xFF9E9E9E)
    }
    val phaseLabel = when (phase) {
        TimedBreathPhase.Inhale -> "Inhale"
        TimedBreathPhase.Hold -> "Hold"
        TimedBreathPhase.Exhale -> "Exhale"
        TimedBreathPhase.Wait -> "Wait"
    }
    val totalForPhase = when (phase) {
        TimedBreathPhase.Inhale -> 4
        TimedBreathPhase.Hold -> 2
        TimedBreathPhase.Exhale -> 4
        TimedBreathPhase.Wait -> 1
    }

    Column(
        modifier = modifier.fillMaxWidth().height(300.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = phaseLabel,
            style = MaterialTheme.typography.headlineMedium,
            color = phaseColor,
        )
        Spacer(modifier = Modifier.height(16.dp))
        CountdownTimer(
            remainingSeconds = remainingSeconds,
            totalSeconds = totalForPhase,
            color = phaseColor,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Round ${currentRepetition + 1} of $totalRepetitions",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
