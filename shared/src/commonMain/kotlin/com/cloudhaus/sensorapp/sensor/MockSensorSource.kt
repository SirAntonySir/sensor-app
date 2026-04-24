package com.cloudhaus.sensorapp.sensor

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Mock sensor source for testing and emulator development.
 * Emits a sine wave pressure pattern simulating breathing.
 */
class MockSensorSource(
    private val baselinePressure: Double = 1013.25,
    private val amplitude: Double = 0.3,
    private val breathCycleMs: Long = 4000,
) : SensorSource {

    private var running = false

    override val pressureFlow: Flow<PressureReading> = flow {
        val startTime = System.currentTimeMillis()
        while (running) {
            val elapsed = System.currentTimeMillis() - startTime
            val phase = (elapsed % breathCycleMs).toDouble() / breathCycleMs
            val pressure = baselinePressure + amplitude * kotlin.math.sin(2 * Math.PI * phase)
            emit(PressureReading(pressure, System.currentTimeMillis()))
            delay(BarometerThresholds.READING_INTERVAL_MS)
        }
    }

    override fun start() { running = true }
    override fun stop() { running = false }
}
