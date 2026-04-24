# Implementierungsplan -- KMP-Sensor-App auf macOS

Stand: April 2026 -- Zielsetup: macOS Tahoe 26.2+ auf Apple Silicon

---

## Phase 0 -- System-Voraussetzungen

**Hardware:** Mac mit Apple Silicon (M1 oder neuer), mind. 16 GB RAM (32 GB empfohlen fuer parallele iOS-Simulatoren + Android-Emulator + Gradle-Daemon), mind. 100 GB freier Speicher (Xcode + Simulatoren + Android SDK + Caches fressen schnell 50-80 GB).

**OS:** macOS Tahoe 26.2 oder neuer -- wird fuer Xcode 26.4 vorausgesetzt.

**Apple Developer Account:** $99/Jahr, brauchst du fuer TestFlight, Provisioning Profiles, Push Notifications und App Store. Account *vor* Projektbeginn anlegen, Verifikation kann Tage dauern.

**Google Play Developer Account:** einmalig $25, aehnliche Verzoegerungen moeglich (Identitaetspruefung).

---

## Phase 1 -- Basis-Toolchain installieren

### 1.1 Homebrew
Erstes was drauf kommt -- Paketmanager fuer alles weitere.
```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

### 1.2 Versionsmanager fuer JDK
Statt System-JDK nutze **mise** (oder SDKMAN). Damit kannst du JDK-Versionen pro Projekt pinnen -- wichtig, weil Android Gradle Plugin und Kotlin oft sehr spezifische JDK-Versionen brauchen.
```bash
brew install mise
mise use --global java@temurin-21
```
Empfehlung: **JDK 21 LTS** (Temurin/Eclipse Adoptium). Android Gradle Plugin 9.x verlangt JDK 17+, KMP laeuft sauber auf 21.

**Hinweis:** Die bestehende `avdmanager` Shell-Funktion in `~/.zshrc` setzt JAVA_HOME auf Android Studio JBR. Bei Umstellung auf mise-managed JDK diese Funktion anpassen oder entfernen, um Konflikte zu vermeiden.

### 1.3 Xcode
**Xcode 26.4.1** (Stand April 2026) aus dem Mac App Store oder von developer.apple.com. Achtung: ab April 2026 verlangt der App Store, dass alle Apps mit Xcode 26+ und iOS 26 SDK gebaut werden.

Nach Installation:
```bash
sudo xcode-select --switch /Applications/Xcode.app/Contents/Developer
sudo xcodebuild -license accept
xcodebuild -runFirstLaunch
```

### 1.4 Android Studio
Aktuelle Stable: **Android Studio Panda 3** (2025.3.3). Lade von developer.android.com. Waehrend des Setups SDK Platform 35 (Android 15) und 36 installieren, plus Build-Tools, Platform-Tools, Emulator.

SDK-Pfad ist bereits in `~/.zshrc` konfiguriert:
```bash
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator
```

### 1.5 KMP-Plugin
In Android Studio: Settings -> Plugins -> "Kotlin Multiplatform" installieren. Erfordert Android Studio Narwhal (2025.1.1) oder neuer auf macOS.

### 1.6 KDoctor (Diagnose-Tool von JetBrains)
Prueft, ob deine KMP-Toolchain komplett ist:
```bash
brew install kdoctor
kdoctor
```
Laeuft auch spaeter bei Problemen -- meldet z.B. fehlende CocoaPods, falsche Xcode-Pfade, Ruby-Versionen.

---

## Phase 2 -- Build-Tools & Hilfsmittel

### 2.1 Ruby (fuer Fastlane und CocoaPods)
Niemals System-Ruby benutzen. Mit mise:
```bash
mise use --global ruby@3.3
```

### 2.2 CocoaPods
Wird gebraucht, falls dein KMP-Framework via CocoaPods in den iOS-Build integriert wird (alternative: Swift Package Manager -- seit Kotlin 2.1 stabil).
```bash
gem install cocoapods
```

### 2.3 Fastlane
Fuer CI/CD und lokale Release-Automation.
```bash
gem install fastlane
```

### 2.4 Git + GitHub CLI
Bereits installiert. Repo wird unter GitHub (SirAntonySir) angelegt.

### 2.5 Optional aber sehr nuetzlich
```bash
brew install --cask rectangle    # Window-Management
brew install jq yq               # JSON/YAML-CLI
brew install gradle-completion   # Shell-Completion fuer Gradle
brew install --cask proxyman     # HTTP/BLE-Inspection
```

### 2.6 BLE-Debugging
- **LightBlue** (App Store) -- BLE-Peripherie simulieren und scannen
- **Bluetility** (open source) -- tieferes BLE-Debugging
- **PacketLogger** (Teil von "Additional Tools for Xcode" auf developer.apple.com) -- BLE-Pakete auf iOS-Devices mitschneiden

---

## Phase 3 -- Projektstruktur

```
sensor-app/
|-- shared/
|   |-- sensor-api/        # SensorSource Interface, Datenklassen
|   |-- pipeline/          # Filter, Glaettung, Feature-Extraktion
|   |-- animation-driver/  # State-Maschinen, Interpolation
|   +-- build.gradle.kts
|-- androidApp/
|   |-- src/main/kotlin/
|   +-- build.gradle.kts
|-- iosApp/
|   |-- iosApp.xcodeproj
|   +-- iosApp/
|-- fastlane/
|   |-- Fastfile
|   +-- Matchfile
|-- .github/workflows/
|-- docs/
|   |-- IMPLEMENTATION_PLAN.md    # This file
|   +-- REFERENCE_BREATHINGAPP.md # Reference to original breathing app
|-- gradle/libs.versions.toml    # Version Catalog (zentral)
|-- build.gradle.kts
+-- settings.gradle.kts
```

---

## Phase 4 -- Versionierung

### 4.1 Branching
**Trunk-based** mit kurzlebigen Feature-Branches. Schema:
- `main` -- immer release-faehig, geschuetzt
- `feature/<ticket>-<kurzbeschreibung>` -- Lebensdauer < 1 Woche
- Tags: `v1.2.3` fuer Releases

### 4.2 Semantic Versioning
- App-Version: `MAJOR.MINOR.PATCH` (z.B. 1.4.2)
- SDK-Version: ebenfalls SemVer, **strikt** wenn extern verwendet
- Build-Number: monoton steigender Integer aus CI (`GITHUB_RUN_NUMBER`)

### 4.3 Conventional Commits
```
feat(pipeline): add Kalman filter for IMU data
fix(ble): reconnect after sleep wake
chore(deps): bump kotlin to 2.3.20
```

---

## Phase 5 -- Data Persistence (Supabase)

Anstelle von Firebase Realtime Database wird **Supabase** mit dem offiziellen **Kotlin SDK** verwendet.

### 5.1 Warum Supabase
- Offizielles Kotlin Multiplatform SDK (`io.github.jan-tennert.supabase`)
- PostgreSQL-basiert mit Row Level Security
- Realtime subscriptions (wie Firebase RTDB)
- Auth, Storage, Edge Functions inklusive
- Self-hosting moeglich fuer volle Kontrolle

### 5.2 Datenmodell (migriert vom Breathing-App Firebase-Schema)
```sql
-- users
create table users (
  id uuid primary key default gen_random_uuid(),
  device_id text unique not null,
  first_name text,
  last_name text,
  email text,
  gender text,
  age integer,
  weight integer,
  height integer,
  created_at timestamptz default now()
);

-- exercise progression
create table progression (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references users(id),
  unit_id integer not null,
  no_of_completions integer default 0,
  difficulties jsonb default '{}',
  created_at timestamptz default now()
);

-- exercise results
create table exercise_results (
  id uuid primary key default gen_random_uuid(),
  progression_id uuid references progression(id),
  exercise_name text not null,
  result_data jsonb not null,
  recorded_at timestamptz default now()
);

-- survey responses
create table survey_responses (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references users(id),
  responses jsonb not null,
  submitted_at timestamptz default now()
);
```

### 5.3 KMP Integration
```toml
# In gradle/libs.versions.toml
[versions]
supabase = "3.1.0"
ktor = "3.3.0"

[libraries]
supabase-postgrest = { module = "io.github.jan-tennert.supabase:postgrest-kt", version.ref = "supabase" }
supabase-realtime = { module = "io.github.jan-tennert.supabase:realtime-kt", version.ref = "supabase" }
supabase-auth = { module = "io.github.jan-tennert.supabase:auth-kt", version.ref = "supabase" }
ktor-client-cio = { module = "io.ktor:ktor-client-cio", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
```

### 5.4 Auth-Strategie
- Device-basierte anonyme Auth (wie bisher) via Supabase Anonymous Sign-In
- Optional spaeter: Email/Password oder OAuth upgrade
- Row Level Security Policies auf `device_id` bzw. `auth.uid()`

---

## Phase 6 -- Code-Qualitaet

### 6.1 Linter / Formatter
- **ktlint** (Kotlin, alles in `shared/` + `androidApp/`)
- **detekt** (statische Code-Analyse Kotlin)
- **SwiftLint** (fuer `iosApp/`) -- via `brew install swiftlint`
- **SwiftFormat** -- via `brew install swiftformat`

### 6.2 Pre-commit Hooks
```bash
brew install pre-commit
```

### 6.3 Tests
- Shared Module: kotlin.test + Turbine (fuer Flow-Tests) + MockK
- Android: JUnit 5, Espresso fuer UI
- iOS: XCTest
- Mock-`SensorSource` mit aufgezeichneten Sensor-CSVs als Fixtures

---

## Phase 7 -- CI/CD

### 7.1 GitHub Actions
Workflows:
- `pr-check.yml` -- Build + Tests bei jedem PR
- `release-android.yml` -- Tag-getriggert, baut AAB, signiert, laedt zu Play Internal Testing
- `release-ios.yml` -- Tag-getriggert, laeuft auf macos-14, Match fuer Signing, Upload zu TestFlight

### 7.2 Code Signing -- Fastlane Match
```bash
fastlane match init
fastlane match appstore
```

### 7.3 Distribution
- iOS: TestFlight
- Android: Play Console Internal Testing -> Closed Testing -> Production

---

## Phase 8 -- Observability & Monitoring

### 8.1 Crash-Reporting
**Sentry** (cross-platform, gut in KMP integriert).

### 8.2 Sensor-Telemetrie
- Tatsaechliche Sampling-Rate (Hz)
- Drop-Rate (verworfene Samples)
- BLE-Verbindungsstabilitaet (Disconnects/h, Reconnect-Latenz)
- Latenz Sensor -> Animations-Frame
- Firmware-Version des verbundenen Sensors

---

## Phase 9 -- Empfohlene Reihenfolge der Umsetzung

1. **Tag 1:** Phase 0-2 komplett, `kdoctor` gruen
2. **Tag 2:** KMP-Projekt steht, beide Apps starten auf Sim/Emulator
3. **Tag 3-5:** `sensor-api`-Modul, Mock-`SensorSource`, erste Pipeline-Funktion mit Tests
4. **Woche 2:** Animation-Driver shared, beide UIs reagieren auf Mock-Daten
5. **Woche 3:** Echte BLE-Anbindung (iOS), Android-interne Sensoren (Barometer)
6. **Woche 4:** Supabase-Integration, Auth + Datenpersistenz
7. **Woche 5:** SDK-Modul abspalten, GitHub Actions PR-Check aufsetzen
8. **Woche 6:** Match + TestFlight + Play Internal funktionieren end-to-end
9. **Woche 7:** Sentry, Telemetrie, Performance-Tuning
10. **Woche 8+:** Exercise-Migration vom Breathing-App, Beta-Tester

---

## Versions-Snapshot (April 2026)

| Komponente | Version | Quelle |
|---|---|---|
| macOS | Tahoe 26.2+ | System |
| Xcode | 26.4.1 | App Store |
| Android Studio | Panda 3 (2025.3.3) | developer.android.com |
| JDK | Temurin 21 LTS | mise |
| Kotlin | 2.3.20 | Version Catalog |
| Compose Multiplatform | 1.10.3 | Version Catalog |
| Android Gradle Plugin | 9.0.0 | Version Catalog |
| Supabase Kotlin SDK | 3.1.0 | Version Catalog |
| Ktor | 3.3.0 | Version Catalog |
| Ruby | 3.3 | mise |
| Fastlane | latest | gem |
| CocoaPods | latest | gem |

Alle Versionen vor Projektstart noch einmal gegen die offiziellen Release-Notes pruefen.
