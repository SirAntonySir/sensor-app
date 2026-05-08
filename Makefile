# Makefile for sensor-app (Kotlin Multiplatform: Android + iOS)
#
# Common targets:
#   make build              # build everything (shared framework + both apps)
#   make run-android        # install + launch on connected Android device/emulator
#   make run-ios            # build + install + launch on booted iOS simulator
#   make test               # shared multiplatform tests
#   make clean              # gradle clean + drop iOS build artifacts
#
# See `make help` for the full list.

SHELL := /bin/bash

# ---- Config -----------------------------------------------------------------

ANDROID_APP_ID    := com.cloudhaus.sensorapp
IOS_BUNDLE_ID     := com.cloudhaus.sensorapp
IOS_SCHEME        := SensorApp
IOS_PROJECT       := iosApp/SensorApp.xcodeproj
IOS_APP_NAME      := SensorApp.app
IOS_CONFIG        := Debug
IOS_SIM_NAME      ?= iPhone 17 Pro
IOS_DERIVED_DATA  := $(HOME)/Library/Developer/Xcode/DerivedData

GRADLE := ./gradlew

# ---- Help -------------------------------------------------------------------

.DEFAULT_GOAL := help

.PHONY: help
help:
	@awk 'BEGIN {FS = ":.*##"; printf "Targets:\n"} /^[a-zA-Z_-]+:.*?##/ { printf "  \033[36m%-22s\033[0m %s\n", $$1, $$2 }' $(MAKEFILE_LIST)

# ---- Shared / KMP -----------------------------------------------------------

.PHONY: shared-ios
shared-ios: ## Build the iOS simulator framework (arm64)
	$(GRADLE) :shared:linkDebugFrameworkIosSimulatorArm64

.PHONY: test
test: ## Run shared multiplatform tests
	$(GRADLE) :shared:allTests

.PHONY: clean
clean: ## Gradle clean + remove iOS build artifacts
	$(GRADLE) clean
	rm -rf iosApp/build
	rm -rf $(IOS_DERIVED_DATA)/SensorApp-*

# ---- Android ----------------------------------------------------------------

.PHONY: build-android
build-android: ## Assemble Android debug APK
	$(GRADLE) :androidApp:assembleDebug

.PHONY: install-android
install-android: ## Install Android debug APK on connected device/emulator
	$(GRADLE) :androidApp:installDebug

.PHONY: run-android
run-android: install-android ## Install + launch the Android app
	adb shell monkey -p $(ANDROID_APP_ID) -c android.intent.category.LAUNCHER 1

.PHONY: stop-android
stop-android: ## Force-stop the Android app
	adb shell am force-stop $(ANDROID_APP_ID)

.PHONY: logcat
logcat: ## Tail logcat filtered to the app
	adb logcat --pid=$$(adb shell pidof -s $(ANDROID_APP_ID))

# ---- iOS --------------------------------------------------------------------

.PHONY: xcodegen
xcodegen: ## Regenerate the Xcode project from project.yml
	cd iosApp && xcodegen generate

.PHONY: ios-boot
ios-boot: ## Boot the target iOS simulator if not already booted
	@if ! xcrun simctl list devices booted | grep -q "$(IOS_SIM_NAME)"; then \
		echo "Booting $(IOS_SIM_NAME)..."; \
		xcrun simctl boot "$(IOS_SIM_NAME)" || true; \
		open -a Simulator; \
	fi

.PHONY: build-ios
build-ios: xcodegen ## Build the iOS app for the booted simulator
	@SIM_ID=$$(xcrun simctl list devices booted | grep -oE '\([0-9A-F-]{36}\)' | head -1 | tr -d '()'); \
	if [ -z "$$SIM_ID" ]; then echo "No booted simulator. Run 'make ios-boot' first."; exit 1; fi; \
	xcodebuild -project $(IOS_PROJECT) -scheme $(IOS_SCHEME) -configuration $(IOS_CONFIG) \
		-destination "platform=iOS Simulator,id=$$SIM_ID" build

.PHONY: install-ios
install-ios: build-ios ## Install the iOS app on the booted simulator
	@APP_PATH=$$(find $(IOS_DERIVED_DATA) -type d -name $(IOS_APP_NAME) -path "*Debug-iphonesimulator*" -print -quit); \
	if [ -z "$$APP_PATH" ]; then echo "Built app not found"; exit 1; fi; \
	xcrun simctl install booted "$$APP_PATH"

.PHONY: run-ios
run-ios: ios-boot install-ios ## Boot sim + build + install + launch the iOS app
	xcrun simctl launch booted $(IOS_BUNDLE_ID)

.PHONY: stop-ios
stop-ios: ## Terminate the iOS app on the booted simulator
	xcrun simctl terminate booted $(IOS_BUNDLE_ID) || true

.PHONY: ios-log
ios-log: ## Stream logs from the iOS app on the booted simulator
	xcrun simctl spawn booted log stream --level=debug --predicate 'process == "$(IOS_SCHEME)"'

# ---- Aggregate --------------------------------------------------------------

.PHONY: build
build: build-android build-ios ## Build both Android and iOS

.PHONY: run
run: ## Alias for run-ios (override with `make run TARGET=android`)
	@$(MAKE) run-$(or $(TARGET),ios)
