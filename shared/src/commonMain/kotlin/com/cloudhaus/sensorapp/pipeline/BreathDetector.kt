package com.cloudhaus.sensorapp.pipeline

import com.cloudhaus.sensorapp.sensor.BarometerThresholds
import com.cloudhaus.sensorapp.sensor.PressureReading
import com.cloudhaus.sensorapp.sensor.SensorSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Processes raw pressure readings into breath events.
 * Ported from breathingapp-main/src/components/screens/Exercise/barometerService.tsx
 */
class BreathDetector(private val sensorSource: SensorSource) {

    private var baselinePressure: Double? = null

    val breathFlow: Flow<BreathEvent> = sensorSource.pressureFlow.map { reading ->
        val baseline = baselinePressure ?: run {
            baselinePressure = reading.pressure
            reading.pressure
        }
        val delta = reading.pressure - baseline
        val velocity = calculateVelocity(delta)

        when {
            delta > BarometerThresholds.MINIMUM_TOLERANCE -> BreathEvent.Exhale(velocity, reading)
            delta < -BarometerThresholds.MINIMUM_TOLERANCE -> BreathEvent.Inhale(velocity, reading)
            else -> BreathEvent.Idle(reading)
        }
    }

    fun resetBaseline() {
        baselinePressure = null
    }

    companion object {
        /** velocity = sqrt(2 * pressureDelta_hPa * 100) -- from original app */
        fun calculateVelocity(pressureDeltaHpa: Double): Double {
            val absDelta = kotlin.math.abs(pressureDeltaHpa)
            return kotlin.math.sqrt(2.0 * absDelta * 100.0)
        }

        /** Trapezoidal integration of velocity over time */
        fun trapezoidalIntegral(y: List<Double>, x: List<Double>): Double {
            require(y.size == x.size) { "y and x must have the same length" }
            var integral = 0.0
            for (i in 0 until y.size - 1) {
                integral += (x[i + 1] - x[i]) * (y[i + 1] + y[i]) / 2.0
            }
            return integral
        }
    }
}

sealed class BreathEvent {
    abstract val reading: PressureReading

    data class Inhale(val velocity: Double, override val reading: PressureReading) : BreathEvent()
    data class Exhale(val velocity: Double, override val reading: PressureReading) : BreathEvent()
    data class Idle(override val reading: PressureReading) : BreathEvent()
}
