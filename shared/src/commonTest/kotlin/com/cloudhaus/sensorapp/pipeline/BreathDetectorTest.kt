package com.cloudhaus.sensorapp.pipeline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BreathDetectorTest {

    @Test
    fun velocityCalculation() {
        val velocity = BreathDetector.calculateVelocity(0.5)
        assertTrue(velocity > 0)
        assertEquals(10.0, velocity, 0.01)
    }

    @Test
    fun trapezoidalIntegrationSimple() {
        // Integral of constant 2.0 over [0, 1, 2] = 4.0
        val y = listOf(2.0, 2.0, 2.0)
        val x = listOf(0.0, 1.0, 2.0)
        assertEquals(4.0, BreathDetector.trapezoidalIntegral(y, x), 0.001)
    }

    @Test
    fun trapezoidalIntegrationTriangle() {
        // Triangle: [0, 1, 0] over [0, 1, 2] = 1.0
        val y = listOf(0.0, 1.0, 0.0)
        val x = listOf(0.0, 1.0, 2.0)
        assertEquals(1.0, BreathDetector.trapezoidalIntegral(y, x), 0.001)
    }
}
