package com.cloudhaus.sensorapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Teal = Color(0xFF27A1A3)
val TealLight = Color(0xFF9CDADB)
val TealDark = Color(0xFF1A7A7C)

private val LightColorScheme = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = TealLight,
    onPrimaryContainer = TealDark,
    secondary = Teal,
)

@Composable
fun SensorAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content,
    )
}
