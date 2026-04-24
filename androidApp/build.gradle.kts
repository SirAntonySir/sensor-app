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
        }
    }
}

android {
    namespace = "com.cloudhaus.sensorapp"
    compileSdk = 35
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
    }
}
