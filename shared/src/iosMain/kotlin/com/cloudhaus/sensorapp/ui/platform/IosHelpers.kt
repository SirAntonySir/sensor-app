package com.cloudhaus.sensorapp.ui.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun createMainScope(): CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
