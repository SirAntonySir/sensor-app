package com.cloudhaus.sensorapp.sensor

import android.content.Context
import java.io.File

/**
 * [SensorLogger] that appends readings to `filesDir/sensor-log.csv`. Writes the
 * [SensorLogFormat.HEADER] when the file is first created and stops appending
 * once the file passes [SensorLogFormat.MAX_BYTES] (until [clear]).
 */
class AndroidSensorLogger(context: Context) : SensorLogger {

    private val file = File(context.filesDir, "sensor-log.csv")
    private val lock = Any()

    override fun append(reading: PressureReading, source: String, exercise: String) {
        synchronized(lock) {
            if (file.length() > SensorLogFormat.MAX_BYTES) return
            val newFile = !file.exists()
            file.appendText(
                buildString {
                    if (newFile) append(SensorLogFormat.HEADER).append('\n')
                    append(SensorLogFormat.line(reading, source, exercise)).append('\n')
                }
            )
        }
    }

    override fun filePath(): String? = synchronized(lock) {
        if (file.exists() && file.length() > 0) file.absolutePath else null
    }

    override fun clear() = synchronized(lock) {
        if (file.exists()) file.delete()
        Unit
    }

    override fun sizeBytes(): Long = synchronized(lock) {
        if (file.exists()) file.length() else 0L
    }
}
