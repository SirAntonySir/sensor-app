package com.cloudhaus.sensorapp.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persisted app settings backed by SharedPreferences. Currently just the
 * debug-only "force simulated sensor" flag, exposed as a [StateFlow] so Compose
 * recomposes when it changes.
 */
class AndroidAppSettings(context: Context) {

    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _forceMockSensor = MutableStateFlow(prefs.getBoolean(KEY_FORCE_MOCK, false))
    val forceMockSensor: StateFlow<Boolean> = _forceMockSensor.asStateFlow()

    fun setForceMockSensor(value: Boolean) {
        prefs.edit().putBoolean(KEY_FORCE_MOCK, value).apply()
        _forceMockSensor.value = value
    }

    private companion object {
        const val KEY_FORCE_MOCK = "force_mock_sensor"
    }
}
