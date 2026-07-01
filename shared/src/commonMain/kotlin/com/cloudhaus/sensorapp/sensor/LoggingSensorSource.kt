package com.cloudhaus.sensorapp.sensor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

/**
 * [SensorSource] decorator that tees every reading into a [SensorLogger] as it
 * flows to the consumer. Logging happens on collection, so only readings that
 * actually drive an exercise are recorded. Transparent to [start]/[stop].
 */
class LoggingSensorSource(
    private val delegate: SensorSource,
    private val logger: SensorLogger,
    private val source: String,
    private val exercise: String,
) : SensorSource {

    override val pressureFlow: Flow<PressureReading> =
        delegate.pressureFlow.onEach { logger.append(it, source, exercise) }

    override fun start() = delegate.start()
    override fun stop() = delegate.stop()
}
