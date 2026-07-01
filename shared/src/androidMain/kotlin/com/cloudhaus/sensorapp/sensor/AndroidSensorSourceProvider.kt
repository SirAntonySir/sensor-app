package com.cloudhaus.sensorapp.sensor

import android.hardware.Sensor
import android.hardware.SensorManager

/**
 * Android [SensorSourceProvider] backed by [SensorManager]. Reports whether a
 * pressure sensor exists and builds an [AndroidBarometerSource] when it does.
 */
class AndroidSensorSourceProvider(
    private val sensorManager: SensorManager,
) : BaseSensorSourceProvider() {

    override val barometerAvailable: Boolean
        get() = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null

    override fun createReal(): SensorSource = AndroidBarometerSource(sensorManager)
}
