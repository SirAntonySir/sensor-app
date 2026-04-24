package com.cloudhaus.sensorapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cloudhaus.sensorapp.navigation.SensorNavGraph
import com.cloudhaus.sensorapp.ui.theme.SensorAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SensorAppTheme {
                SensorNavGraph()
            }
        }
    }
}
