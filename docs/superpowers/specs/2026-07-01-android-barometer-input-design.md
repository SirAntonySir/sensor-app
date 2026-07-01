# Android Barometer Input — Design

**Date:** 2026-07-01
**Scope:** Wire the breathing-exercise pipeline to the real Android barometer, replacing the hardcoded `MockSensorSource`. iOS is out of scope for this change.

## Goal

Drive `BreathDetector`/engines from the device's real pressure sensor (`Sensor.TYPE_PRESSURE`) on Android. Keep emulator/no-barometer development working via an explicit, debug-only fallback to the existing mock.

## Current state

- `SensorSource` (commonMain) exposes `pressureFlow: Flow<PressureReading>` + `start()`/`stop()`. `PressureReading.pressure` is in hPa.
- `MockSensorSource` is the only implementation, instantiated directly at `ExerciseScreen.kt:35`.
- `BreathDetector(sensorSource)` maps readings to breath events; `ExerciseEngineFactory` builds engines from a `BreathDetector`. None of these change.
- Koin 4.0.4 is a declared dependency but is **not** initialized anywhere (no `startKoin`, no Application class, no modules).

## Design

### 1. Shared abstraction (commonMain)

New `SensorSourceProvider`:

```kotlin
interface SensorSourceProvider {
    val barometerAvailable: Boolean
    fun create(forceMock: Boolean = false): SensorSource
}
```

`create()` returns the real barometer source when available and not forced to mock; otherwise a `MockSensorSource`.

### 2. Android implementation (androidMain)

- `AndroidBarometerSource(sensorManager)` implements `SensorSource`. `pressureFlow` is a `callbackFlow` that registers a `SensorEventListener` on `Sensor.TYPE_PRESSURE` at `SENSOR_DELAY_GAME`, emits `PressureReading(event.values[0].toDouble(), timestampMs)`, and unregisters in `awaitClose`. `start()`/`stop()` gate collection. The barometer requires **no runtime permission**.
- `AndroidSensorSourceProvider(sensorManager)` implements `SensorSourceProvider`; `barometerAvailable = sensorManager.getDefaultSensor(TYPE_PRESSURE) != null`; `create(forceMock)` picks mock vs. real.

### 3. Koin bootstrap

- `SensorApp : Application` calls `startKoin { androidContext(this@SensorApp); modules(androidSensorModule) }`; registered via `android:name=".SensorApp"` in the manifest.
- `androidSensorModule` provides `SensorSourceProvider` as a singleton (`AndroidSensorSourceProvider` built from `androidContext().getSystemService(...)`).
- Add `koin-androidx-compose` to `libs.versions.toml` and androidApp deps so composables resolve via `koinInject()`.

### 4. ExerciseScreen UI

Replace the hardcoded mock:

- Inject `SensorSourceProvider` via `koinInject()`.
- Track `forceMock` in `remember { mutableStateOf(false) }`.
- If `provider.barometerAvailable || forceMock` → build the source with `provider.create(forceMock)` and run the exercise as today.
- Else → render a "Barometer unavailable on this device" state instead of the exercise. In `BuildConfig.DEBUG` builds only, that state shows a "Continue with simulated sensor" button that sets `forceMock = true`.

`BreathDetector` and engines are unchanged.

## Testing

- Unit-test `AndroidSensorSourceProvider.create()` selection logic across the `forceMock`/availability matrix (availability made injectable for the test).
- Existing `BreathDetectorTest` continues to cover the pipeline.
- Manual: run on a barometer-equipped device (real readings) and on an emulator (unavailable state → debug button → mock).

## Trade-offs

Bootstrapping Koin adds an Application class, one module, and one catalog entry. Accepted: it's the DI foundation for iOS parity and Supabase wiring later. The dev toggle is intentionally in-memory and debug-gated rather than a persisted setting to keep scope minimal.
