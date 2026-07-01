package com.cloudhaus.sensorapp.sensor

import com.cloudhaus.sensorapp.util.currentTimeMillis
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import platform.CoreMotion.CMAltimeter
import platform.Foundation.NSOperationQueue

/**
 * [SensorSource] backed by the iOS barometer via CoreMotion's [CMAltimeter].
 *
 * `CMAltitudeData.pressure` is reported in **kPa**, whereas the rest of the
 * pipeline works in hPa (millibar), so each reading is multiplied by 10.
 * Relative-altitude updates require the `NSMotionUsageDescription` Info.plist
 * key; without it CoreMotion terminates the app.
 *
 * Registration is gated by [start]/[stop] to match [MockSensorSource]. Updates
 * are delivered on the main queue and republished through a buffered
 * [MutableSharedFlow] that drops the oldest reading on overflow.
 */
class IosBarometerSource : SensorSource {

    private val altimeter = CMAltimeter()

    private val _pressureFlow = MutableSharedFlow<PressureReading>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val pressureFlow: Flow<PressureReading> = _pressureFlow.asSharedFlow()

    override fun start() {
        altimeter.startRelativeAltitudeUpdatesToQueue(NSOperationQueue.mainQueue) { data, _ ->
            val kpa = data?.pressure?.doubleValue ?: return@startRelativeAltitudeUpdatesToQueue
            _pressureFlow.tryEmit(PressureReading(kpa * 10.0, currentTimeMillis()))
        }
    }

    override fun stop() {
        altimeter.stopRelativeAltitudeUpdates()
    }
}
