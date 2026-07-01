package com.cloudhaus.sensorapp.sensor

/**
 * Records raw pressure readings so they can later be exported for inspection
 * (e.g. verifying the barometer signal path). Platform implementations persist
 * to a CSV file: `AndroidSensorLogger` (filesDir) and `IosSensorLogger`
 * (Documents). [SensorLogFormat] defines the on-disk schema.
 */
interface SensorLogger {
    /** Append one reading, tagged with the active source and exercise. */
    fun append(reading: PressureReading, source: String, exercise: String)

    /** Absolute path to the CSV file, or null if nothing has been logged yet. */
    fun filePath(): String?

    /** Delete all logged data. */
    fun clear()

    /** Current log size in bytes (0 when empty). */
    fun sizeBytes(): Long
}

/** CSV schema for the sensor log. Kept platform-agnostic so it can be unit-tested. */
object SensorLogFormat {
    const val HEADER = "timestamp_ms,pressure_hpa,source,exercise"

    /** ~20 MB soft cap; loggers stop appending past this until cleared. */
    const val MAX_BYTES = 20L * 1024 * 1024

    fun line(reading: PressureReading, source: String, exercise: String): String {
        return "${reading.timestampMs},${reading.pressure},${sanitize(source)},${sanitize(exercise)}"
    }

    /** Strip characters that would break CSV structure. */
    private fun sanitize(value: String): String =
        value.replace(",", " ").replace("\n", " ").replace("\r", " ").trim()
}
