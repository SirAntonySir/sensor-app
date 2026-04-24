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

    var body: some View {
        VStack(spacing: 24) {
            switch phase {
            case .instructions:
                instructionsView
            case .countdown:
                countdownView
            case .active:
                activeExerciseView
            case .complete:
                completeView
            }
        }
        .padding()
        .navigationTitle(exerciseName)
        .navigationBarTitleDisplayMode(.inline)
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

    // MARK: - Active Exercise (with shared Compose animation)
    private var activeExerciseView: some View {
        VStack(spacing: 16) {
            if let engine = engine {
                // Shared Compose Multiplatform animation embedded via UIViewControllerRepresentable
                ComposeAnimationView(engineState: engine.stateFlow)
                    .frame(height: 320)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            } else {
                ProgressView()
                    .frame(height: 320)
            }

            // Native SwiftUI controls
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
            Button("Finish") {
                finishExercise()
            }
            .font(.subheadline)
            .foregroundStyle(.secondary)
        }
    }

    // MARK: - Complete
    private var completeView: some View {
        VStack(spacing: 24) {
            Spacer()
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 80))
                .foregroundStyle(.green)
            Text("Exercise Complete!")
                .font(.title.bold())
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
        case "Tissue": return "doc.fill"
        case "Dandelion": return "leaf.fill"
        case "Boat": return "sailboat.fill"
        default: return "lungs.fill"
        }
    }

    private var exerciseDescription: String {
        switch exerciseName {
        case "Candle": return "Blow steadily to extinguish the candle flame."
        case "Windmill": return "Blow to spin the windmill blades as fast as you can."
        case "Tissue": return "Blow the tissue as far as possible."
        case "Dandelion": return "Blow the dandelion seeds away with a sustained breath."
        case "Boat": return "Blow to propel the boat across the water."
        case "MIE": return "Breathe in and out as deeply as you can."
        default: return "Follow the breathing pattern shown on screen."
        }
    }

    private func startCountdown() {
        phase = .countdown
        countdown = 3

        // Create the shared engine
        let wrapper = SharedExerciseEngine(exerciseName: exerciseName)
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
    func start()
    func stop()
    func onVirtualBlow()
}

class SharedExerciseEngine: ExerciseEngineWrapper {
    private let engine: ExerciseEngine
    private let mockSensor: MockSensorSource
    private let breathDetector: BreathDetector

    var stateFlow: any AnyObject { engine.state }

    init(exerciseName: String) {
        mockSensor = MockSensorSource(baselinePressure: 1013.25, amplitude: 0.3, breathCycleMs: 4000)
        mockSensor.start()
        breathDetector = BreathDetector(sensorSource: mockSensor)

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
    func stop() { engine.stop(); mockSensor.stop() }
    func onVirtualBlow() { engine.onVirtualBlow() }

    private static func typeFromName(_ name: String) -> ExerciseType {
        switch name {
        case "Candle": return .candle
        case "Windmill": return .windmill
        case "Tissue": return .tissue
        case "Dandelion": return .dandelion
        case "Boat": return .boat
        case "Straw": return .straw
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
