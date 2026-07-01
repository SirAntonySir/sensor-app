package com.cloudhaus.sensorapp

import android.app.Application
import com.cloudhaus.sensorapp.di.androidSensorModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SensorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SensorApp)
            modules(androidSensorModule)
        }
    }
}
