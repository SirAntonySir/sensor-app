package com.cloudhaus.sensorapp.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.cloudhaus.sensorapp.util.currentTimeMillis
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * [SensorSource] backed by the device barometer via [SensorManager].
 *
 * `Sensor.TYPE_PRESSURE` reports ambient pressure in hPa (millibar) directly in
 * `event.values[0]`, matching [PressureReading.pressure]. Registering the
 * listener requires no runtime permission.
 *
 * Registration is gated by [start]/[stop] rather than flow collection so the
 * lifecycle matches [MockSensorSource]. Readings are published through a
 * buffered [MutableSharedFlow] that drops oldest on overflow.
 */
class AndroidBarometerSource(
    private val sensorManager: SensorManager,
    private val samplingPeriodUs: Int = SensorManager.SENSOR_DELAY_GAME,
) : SensorSource {

    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    private val _pressureFlow = MutableSharedFlow<PressureReading>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val pressureFlow: Flow<PressureReading> = _pressureFlow.asSharedFlow()

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            _pressureFlow.tryEmit(
                PressureReading(event.values[0].toDouble(), currentTimeMillis())
            )
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    override fun start() {
        sensor?.let { sensorManager.registerListener(listener, it, samplingPeriodUs) }
    }

    override fun stop() {
        sensorManager.unregisterListener(listener)
    }
}
