package com.cloudhaus.sensorapp.sensor

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUserDomainMask
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fputs

/**
 * [SensorLogger] that appends readings to `<Documents>/sensor-log.csv`.
 *
 * Uses POSIX append (`fopen "a"`) for line writes — the Foundation
 * `NSFileHandle` factory isn't cleanly exposed in Kotlin/Native. Writes
 * [SensorLogFormat.HEADER] when the file is first created and stops appending
 * past [SensorLogFormat.MAX_BYTES] (until [clear]). [filePath] lets Swift share
 * the file through a `UIActivityViewController`.
 */
@OptIn(ExperimentalForeignApi::class)
class IosSensorLogger : SensorLogger {

    private val fileManager = NSFileManager.defaultManager
    private val path: String = run {
        val docs = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory, NSUserDomainMask, true,
        ).firstOrNull() as? String
        (docs ?: NSTemporaryDirectory().trimEnd('/')) + "/sensor-log.csv"
    }

    override fun append(reading: PressureReading, source: String, exercise: String) {
        if (sizeBytes() > SensorLogFormat.MAX_BYTES) return
        val isNew = !fileManager.fileExistsAtPath(path)
        val fp = fopen(path, "a") ?: return
        try {
            if (isNew) fputs(SensorLogFormat.HEADER + "\n", fp)
            fputs(SensorLogFormat.line(reading, source, exercise) + "\n", fp)
        } finally {
            fclose(fp)
        }
    }

    override fun filePath(): String? =
        if (fileManager.fileExistsAtPath(path) && sizeBytes() > 0) path else null

    override fun clear() {
        if (fileManager.fileExistsAtPath(path)) {
            fileManager.removeItemAtPath(path, error = null)
        }
    }

    override fun sizeBytes(): Long {
        val attrs = fileManager.attributesOfItemAtPath(path, error = null) ?: return 0L
        val size = attrs[NSFileSize] as? NSNumber ?: return 0L
        return size.longLongValue
    }
}
