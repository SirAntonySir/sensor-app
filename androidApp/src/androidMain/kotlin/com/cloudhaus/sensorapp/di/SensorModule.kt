package com.cloudhaus.sensorapp.di

import android.content.Context
import android.hardware.SensorManager
import com.cloudhaus.sensorapp.sensor.AndroidSensorLogger
import com.cloudhaus.sensorapp.sensor.AndroidSensorSourceProvider
import com.cloudhaus.sensorapp.sensor.SensorLogger
import com.cloudhaus.sensorapp.sensor.SensorSourceProvider
import com.cloudhaus.sensorapp.settings.AndroidAppSettings
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidSensorModule = module {
    single<SensorSourceProvider> {
        val sensorManager = androidContext()
            .getSystemService(Context.SENSOR_SERVICE) as SensorManager
        AndroidSensorSourceProvider(sensorManager)
    }
    single<SensorLogger> { AndroidSensorLogger(androidContext()) }
    single { AndroidAppSettings(androidContext()) }
}
