package com.cloudhaus.sensorapp.sensor

/**
 * Platform-agnostic factory for the active [SensorSource].
 *
 * Lets UI/DI decide between a real hardware barometer and the [MockSensorSource]
 * without knowing platform details:
 * - Android: backed by SensorManager (Sensor.TYPE_PRESSURE).
 * - iOS: not yet wired (still TODO).
 */
interface SensorSourceProvider {
    /** True when the device exposes a usable pressure sensor. */
    val barometerAvailable: Boolean

    /**
     * Build a fresh [SensorSource]. Returns a [MockSensorSource] when
     * [forceMock] is set or no barometer is available; otherwise the real
     * hardware source. The caller owns the returned source's start/stop.
     */
    fun create(forceMock: Boolean = false): SensorSource
}

/**
 * Holds the mock-vs-real selection branch in common code so it can be tested
 * without platform sensor stubs. Platform providers implement [barometerAvailable]
 * and [createReal].
 */
abstract class BaseSensorSourceProvider : SensorSourceProvider {

    /** Build the real hardware-backed source. Only called when a barometer exists. */
    protected abstract fun createReal(): SensorSource

    override fun create(forceMock: Boolean): SensorSource =
        if (forceMock || !barometerAvailable) MockSensorSource() else createReal()
}
