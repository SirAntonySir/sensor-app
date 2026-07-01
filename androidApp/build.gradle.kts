plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "21"
            }
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(project(":shared"))
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.runtime)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.navigation.compose)
            implementation(libs.activity.compose)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.compose.material.icons)
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
        }
    }
}

android {
    namespace = "com.cloudhaus.sensorapp"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.cloudhaus.sensorapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}
