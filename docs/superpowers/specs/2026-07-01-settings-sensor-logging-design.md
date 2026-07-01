# Settings Screen + Sensor-Data Logging — Design

**Date:** 2026-07-01
**Scope:** Add a settings screen reachable from the home screen on both Android and iOS, offering: download logged sensor data (CSV), clear the log, and a debug-only "force simulated sensor" toggle. Add a logging layer that records raw pressure readings during exercises. Doubles as a diagnostic for the real-barometer signal path.

## Current state

- No sensor logging exists; exercises create a `SensorSource` + `BreathDetector` and discard raw readings.
- Home screens: Android `DashboardScreen` (`TopAppBar`), iOS `DashboardView` (`.navigationTitle`, inside a `NavigationStack` from `ContentView`).
- The "force simulated sensor" flag is currently per-exercise local state (Android `ExerciseScreen`, iOS `ExerciseView`).
- Android DI is Koin; iOS constructs shared types directly in Swift.

## Design

### 1. Shared logging core (commonMain)

- `SensorLogger` interface:
  - `append(reading: PressureReading, source: String, exercise: String)`
  - `filePath(): String?` — path to the CSV, or null if nothing logged yet
  - `clear()`
  - `sizeBytes(): Long`
- `SensorLogFormat` — pure CSV builder (testable):
  - `header = "timestamp_ms,pressure_hpa,source,exercise"`
  - `line(reading, source, exercise)` — comma-joined, values sanitized (strip commas/newlines from source/exercise).
- `LoggingSensorSource(delegate, logger, source, exercise) : SensorSource` — decorator; `pressureFlow = delegate.pressureFlow.onEach { logger.append(it, source, exercise) }`; `start`/`stop` delegate through.

### 2. Platform loggers

- Android `AndroidSensorLogger(context)` — appends to `context.filesDir/sensor-log.csv`. Writes the header when the file is first created. Thread-safe (synchronized). Koin singleton.
- iOS `IosSensorLogger` — appends to `<Documents>/sensor-log.csv` via `NSFileHandle` (creating with header if absent). Exposes `filePath()` for Swift to share. Constructed in Swift.
- Both enforce a soft cap (~20 MB): once exceeded, `append` becomes a no-op until `clear()`. Prevents unbounded growth at 50 Hz.

### 3. Hook into exercises

Wrap the created source on both platforms:
`LoggingSensorSource(provider.create(forceMock), logger, source = if (usingBarometer) "barometer" else "mock", exercise = name)`.
`BreathDetector` consumes the wrapped source unchanged.

### 4. Settings screen

- Android: `SettingsScreen` composable + `"settings"` nav route; gear `IconButton` in the Dashboard `TopAppBar` `actions`. Rows:
  - **Download sensor data** — share the CSV via `FileProvider` + `Intent.ACTION_SEND` (`text/csv`). Adds a `<provider>` to the manifest and `res/xml/file_paths.xml`. Disabled when the log is empty.
  - **Clear log** — `logger.clear()`.
  - **Force simulated sensor** (`BuildConfig.DEBUG` only) — `Switch` bound to `AndroidAppSettings`.
- iOS: `SettingsView` + gear toolbar button in `DashboardView`. Rows:
  - **Download sensor data** — `UIActivityViewController` with the CSV file URL from `IosSensorLogger.filePath()`. Disabled when empty.
  - **Clear log** — `logger.clear()`.
  - **Force simulated sensor** (`#if DEBUG`) — `Toggle` bound to `@AppStorage("forceMockSensor")`.

### 5. Force-mock becomes a persisted setting

- Android: `AndroidAppSettings(context)` over `SharedPreferences`, exposing `forceMockSensor: StateFlow<Boolean>` + `setForceMockSensor(Boolean)`; Koin singleton. `ExerciseScreen` reads `.value` when building the source; the "barometer unavailable" debug button calls `setForceMockSensor(true)`.
- iOS: `@AppStorage("forceMockSensor")` in SwiftUI, passed into `SharedExerciseEngine(exerciseName:forceMock:)`; the unavailable-screen debug button sets it.

### 6. Testing

- Common unit tests: `SensorLogFormat` (header + line + sanitization) and `LoggingSensorSource` teeing readings into a fake `SensorLogger`.
- Build both apps. Manual smoke: run an exercise → Settings → Download → CSV opens with real readings.

## Trade-offs / YAGNI

Single append-only CSV, manual clear, no rotation and no in-app log viewer. Soft size cap instead of rotation. The force-mock toggle stays debug-gated. Android needs a `FileProvider` (one manifest entry + one xml file); iOS shares the file URL directly.
