import SwiftUI
import shared

struct ExerciseView: View {
    let exerciseName: String
    let unitId: Int
    @ObservedObject var viewModel: SensorViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var phase: ExercisePhase = .instructions
    @State private var countdown = 3
    @State private var engine: (any ExerciseEngineWrapper)?
    @State private var intensity: Double = 0
    @State private var forceMock = false

    private var barometerAvailable: Bool {
        IosSensorSourceProvider().barometerAvailable
    }

    var body: some View {
        Group {
            if !barometerAvailable && !forceMock {
                barometerUnavailableView
            } else {
                exerciseContent
            }
        }
        .navigationTitle(exerciseName)
        .navigationBarTitleDisplayMode(.inline)
    }

    private var exerciseContent: some View {
        ZStack {
            // Persistent animation layer — same call site across .active and
            // .complete so the underlying Compose VC is reused and seed/frame
            // state survives the phase change.
            if (phase == .active || phase == .complete), let engine {
                ComposeAnimationView(engineState: engine.stateFlow)
                    .ignoresSafeArea()
            }

            VStack(spacing: 24) {
                switch phase {
                case .instructions:
                    instructionsView
                case .countdown:
                    countdownView
                case .active:
                    activeOverlay
                case .complete:
                    completeOverlay
                }
            }
            .padding()
        }
    }

    // MARK: - Barometer unavailable
    private var barometerUnavailableView: some View {
        VStack(spacing: 24) {
            Spacer()
            Image(systemName: "sensor.tag.radiowaves.forward.slash")
                .font(.system(size: 72))
                .foregroundStyle(.red)
            Text("Barometer unavailable")
                .font(.title2.bold())
            Text("This device has no pressure sensor, so breathing exercises can't be driven by the barometer.")
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            Spacer()
            #if DEBUG
            Button {
                forceMock = true
            } label: {
                Text("Continue with simulated sensor")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.teal)
                    .foregroundStyle(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            #endif
            Button {
                dismiss()
            } label: {
                Text("Back")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.teal))
            }
        }
        .padding()
    }

    // MARK: - Instructions
    private var instructionsView: some View {
        VStack(spacing: 24) {
            Spacer()
            Image(systemName: exerciseIcon)
                .font(.system(size: 80))
                .foregroundStyle(.teal)
            Text("Get ready for \(exerciseName)")
                .font(.title2.bold())
            Text(exerciseDescription)
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            Spacer()
            Button {
                startCountdown()
            } label: {
                Text("Start Exercise")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.teal)
                    .foregroundStyle(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
    }

    // MARK: - Countdown
    private var countdownView: some View {
        VStack {
            Spacer()
            Text("\(countdown)")
                .font(.system(size: 120, weight: .bold, design: .rounded))
                .foregroundStyle(.teal)
                .contentTransition(.numericText())
            Text("Get ready...")
                .font(.title3)
                .foregroundStyle(.secondary)
            Spacer()
        }
    }

    // MARK: - Active overlay (controls only — the animation lives in the
    // ZStack above so it can persist across phases).
    private var activeOverlay: some View {
        VStack(spacing: 12) {
            Spacer()
            HStack(spacing: 6) {
                Image(systemName: (engine?.usingBarometer ?? false) ? "barometer" : "testtube.2")
                Text((engine?.usingBarometer ?? false) ? "Barometer" : "Simulated sensor")
                    .font(.caption.weight(.semibold))
            }
            .foregroundStyle((engine?.usingBarometer ?? false) ? Color.teal : Color.orange)
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background(.ultraThinMaterial, in: Capsule())
            if exerciseName == "Dandelion" || exerciseName == "Candle" || exerciseName == "Windmill" || exerciseName == "FloatBall" {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Intensity: \(Int(intensity))")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    Slider(
                        value: Binding(
                            get: { intensity },
                            set: { newValue in
                                intensity = newValue
                                engine?.onVirtualIntensity(Float(newValue))
                            }
                        ),
                        in: 0...100
                    )
                }
            } else {
                Button {
                    engine?.onVirtualBlow()
                } label: {
                    Label("Blow", systemImage: "wind")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.teal)
                        .foregroundStyle(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
        }
    }

    // MARK: - Complete overlay (result card on top of persistent animation).
    private var completeOverlay: some View {
        VStack(spacing: 24) {
            Spacer()
            VStack(spacing: 12) {
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 56))
                    .foregroundStyle(.green)
                Text("Exercise Complete!")
                    .font(.title2.bold())
            }
            .padding(24)
            .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16))
            Spacer()
            Button {
                dismiss()
            } label: {
                Text("Done")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.teal)
                    .foregroundStyle(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
    }

    // MARK: - Helpers
    private var exerciseIcon: String {
        switch exerciseName {
        case "Candle": return "flame.fill"
        case "Windmill": return "wind"
        case "Dandelion": return "leaf.fill"
        case "FloatBall": return "circle.fill"
        default: return "lungs.fill"
        }
    }

    private var exerciseDescription: String {
        switch exerciseName {
        case "Candle": return "Blow steadily to extinguish the candle flame."
        case "Windmill": return "Blow to spin the windmill blades as fast as you can."
        case "Dandelion": return "Blow the dandelion seeds away with a sustained breath."
        case "FloatBall": return "Sustain a steady breath to keep the ball floating at the top."
        case "MIE": return "Breathe in and out as deeply as you can."
        default: return "Follow the breathing pattern shown on screen."
        }
    }

    private func startCountdown() {
        phase = .countdown
        countdown = 3

        // Create the shared engine
        let wrapper = SharedExerciseEngine(exerciseName: exerciseName, forceMock: forceMock)
        engine = wrapper

        Task {
            for _ in 1...3 {
                try? await Task.sleep(for: .seconds(1))
                countdown -= 1
            }
            wrapper.start()
            phase = .active
        }
    }

    private func finishExercise() {
        engine?.stop()
        viewModel.recordCompletion(exercise: exerciseName, unitId: unitId)
        phase = .complete
    }
}

// MARK: - Compose Animation Bridge

struct ComposeAnimationView: UIViewControllerRepresentable {
    let engineState: any AnyObject // Kotlinx StateFlow

    func makeUIViewController(context: Context) -> UIViewController {
        ComposeViewControllersKt.ExerciseAnimationViewController(
            engineStateFlow: engineState as! Kotlinx_coroutines_coreStateFlow
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// MARK: - Engine Wrapper

protocol ExerciseEngineWrapper {
    var stateFlow: any AnyObject { get }
    var usingBarometer: Bool { get }
    func start()
    func stop()
    func onVirtualBlow()
    func onVirtualIntensity(_ intensity: Float)
}

class SharedExerciseEngine: ExerciseEngineWrapper {
    private let engine: ExerciseEngine
    private let sensor: SensorSource
    private let breathDetector: BreathDetector
    let usingBarometer: Bool

    var stateFlow: any AnyObject { engine.state }

    init(exerciseName: String, forceMock: Bool) {
        let provider = IosSensorSourceProvider()
        usingBarometer = !forceMock && provider.barometerAvailable
        sensor = provider.create(forceMock: forceMock)
        sensor.start()
        breathDetector = BreathDetector(sensorSource: sensor)

        let exerciseType = SharedExerciseEngine.typeFromName(exerciseName)
        let config = SharedExerciseEngine.configFromName(exerciseName)

        let scope = IosHelpersKt.createMainScope()
        engine = ExerciseEngineFactory.shared.create(
            type: exerciseType,
            breathDetector: breathDetector,
            config: config,
            difficulty: 0,
            scope: scope
        )
    }

    func start() { engine.start(calibration: nil) }
    func stop() { engine.stop(); sensor.stop() }
    func onVirtualBlow() { engine.onVirtualBlow() }
    func onVirtualIntensity(_ intensity: Float) { engine.onVirtualIntensity(intensity: intensity) }

    private static func typeFromName(_ name: String) -> ExerciseType {
        switch name {
        case "Candle": return .candle
        case "Windmill": return .windmill
        case "Dandelion": return .dandelion
        case "FloatBall": return .floatball
        case "MIE": return .countingbreaths
        default: return .timedbreaths
        }
    }

    private static func configFromName(_ name: String) -> StepConfig {
        if name.contains("-") {
            let parts = name.split(separator: "-").compactMap { Int32($0) }
            return StepConfig(
                breathType: nil,
                breathCount: 5,
                inhaleDuration: parts.count > 0 ? KotlinInt(int: parts[0]) : 4,
                exhaleDuration: parts.count > 2 ? KotlinInt(int: parts[2]) : 4,
                holdDuration: parts.count > 1 ? KotlinInt(int: parts[1]) : 0
            )
        }
        return StepConfig(breathType: nil, breathCount: 5, inhaleDuration: nil, exhaleDuration: nil, holdDuration: nil)
    }
}

// MARK: - Supporting Types

enum ExercisePhase {
    case instructions, countdown, active, complete
}

struct StatRow: View {
    let label: String
    let value: String
    var body: some View {
        HStack {
            Text(label).foregroundStyle(.secondary)
            Spacer()
            Text(value).font(.headline)
        }
    }
}
