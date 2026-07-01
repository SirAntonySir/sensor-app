package com.cloudhaus.sensorapp.sensor

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeLogger : SensorLogger {
    val entries = mutableListOf<Triple<PressureReading, String, String>>()
    override fun append(reading: PressureReading, source: String, exercise: String) {
        entries += Triple(reading, source, exercise)
    }
    override fun filePath(): String? = null
    override fun clear() { entries.clear() }
    override fun sizeBytes(): Long = 0
}

private class FixedSource(private val readings: List<PressureReading>) : SensorSource {
    override val pressureFlow = flowOf(*readings.toTypedArray())
    override fun start() = Unit
    override fun stop() = Unit
}

class SensorLoggingTest {

    @Test
    fun formatLine_matchesSchema() {
        val line = SensorLogFormat.line(PressureReading(1013.25, 1234L), "barometer", "Candle")
        assertEquals("1234,1013.25,barometer,Candle", line)
    }

    @Test
    fun formatLine_sanitizesCommasAndNewlines() {
        val line = SensorLogFormat.line(PressureReading(1000.0, 5L), "a,b", "c\nd")
        assertEquals("5,1000.0,a b,c d", line)
    }

    @Test
    fun loggingSource_teesEveryReading_andForwardsUnchanged() = runTest {
        val readings = listOf(PressureReading(1000.0, 1L), PressureReading(1001.0, 2L))
        val logger = FakeLogger()
        val src = LoggingSensorSource(FixedSource(readings), logger, "mock", "Dandelion")

        val forwarded = src.pressureFlow.toList()

        assertEquals(readings, forwarded)
        assertEquals(readings, logger.entries.map { it.first })
        assertEquals(listOf("mock", "mock"), logger.entries.map { it.second })
        assertEquals(listOf("Dandelion", "Dandelion"), logger.entries.map { it.third })
    }
}
