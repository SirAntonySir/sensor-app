package com.cloudhaus.sensorapp.sensor

import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame

/** A stand-in "real" source so we can tell it apart from the mock in assertions. */
private class FakeRealSource : SensorSource {
    override val pressureFlow get() = throw NotImplementedError()
    override fun start() = Unit
    override fun stop() = Unit
}

private class TestProvider(
    override val barometerAvailable: Boolean,
    val real: SensorSource = FakeRealSource(),
) : BaseSensorSourceProvider() {
    override fun createReal(): SensorSource = real
}

class SensorSourceProviderTest {

    @Test
    fun usesRealSource_whenAvailableAndNotForced() {
        val provider = TestProvider(barometerAvailable = true)
        assertSame(provider.real, provider.create(forceMock = false))
    }

    @Test
    fun usesMock_whenForced_evenIfAvailable() {
        val provider = TestProvider(barometerAvailable = true)
        assertIs<MockSensorSource>(provider.create(forceMock = true))
    }

    @Test
    fun usesMock_whenBarometerUnavailable() {
        val provider = TestProvider(barometerAvailable = false)
        assertIs<MockSensorSource>(provider.create(forceMock = false))
    }
}
