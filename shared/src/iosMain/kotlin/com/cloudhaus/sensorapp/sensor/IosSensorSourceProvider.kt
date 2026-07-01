package com.cloudhaus.sensorapp.sensor

import platform.CoreMotion.CMAltimeter

/**
 * iOS [SensorSourceProvider] backed by CoreMotion's [CMAltimeter]. Reports
 * whether the device exposes barometric pressure and builds an
 * [IosBarometerSource] when it does. Needs no platform context, so Swift can
 * construct it directly.
 */
class IosSensorSourceProvider : BaseSensorSourceProvider() {

    override val barometerAvailable: Boolean
        get() = CMAltimeter.isRelativeAltitudeAvailable()

    override fun createReal(): SensorSource = IosBarometerSource()
}
