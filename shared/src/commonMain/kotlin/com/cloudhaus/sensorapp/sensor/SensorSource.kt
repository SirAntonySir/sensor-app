package com.cloudhaus.sensorapp.sensor

import kotlinx.coroutines.flow.Flow

/**
 * Platform-agnostic sensor interface.
 * Android: implements via SensorManager (barometer)
 * iOS: implements via CMAltimeter / CMBarometer
 */
interface SensorSource {
    val pressureFlow: Flow<PressureReading>
    fun start()
    fun stop()
}

data class PressureReading(
    val pressure: Double,       // hPa
    val timestampMs: Long,
)

object BarometerThresholds {
    const val MINIMUM_TOLERANCE = 0.10  // hPa
    const val SMALL_TOLERANCE = 0.10    // hPa
    const val MEDIUM_TOLERANCE = 0.5    // hPa
    const val READING_INTERVAL_MS = 20L // 50 Hz
}
