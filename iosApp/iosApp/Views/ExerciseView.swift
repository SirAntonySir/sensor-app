import SwiftUI

struct ExerciseView: View {
    let exerciseName: String
    let unitId: Int
    @ObservedObject var viewModel: SensorViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var phase: ExercisePhase = .instructions
    @State private var countdown = 3
    @State private var pressureDisplay: Double = 0
    @State private var timer: Timer?
    @State private var breathCount = 0
    @State private var result: ExerciseResultData?

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

    // MARK: - Active Exercise
    private var activeExerciseView: some View {
        VStack(spacing: 32) {
            Spacer()

            // Pressure indicator
            ZStack {
                Circle()
                    .stroke(Color.teal.opacity(0.2), lineWidth: 8)
                    .frame(width: 200, height: 200)

                Circle()
                    .trim(from: 0, to: min(pressureDisplay / 100, 1.0))
                    .stroke(Color.teal, style: StrokeStyle(lineWidth: 8, lineCap: .round))
                    .frame(width: 200, height: 200)
                    .rotationEffect(.degrees(-90))
                    .animation(.easeInOut(duration: 0.3), value: pressureDisplay)

                VStack {
                    Text(String(format: "%.1f", pressureDisplay))
                        .font(.system(size: 36, weight: .bold, design: .rounded))
                    Text("pressure")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            Text("Breaths: \(breathCount)")
                .font(.title3)

            Spacer()

            // Virtual blow button (for simulator)
            Button {
                simulateBlow()
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

            if let result = result {
                VStack(spacing: 8) {
                    if let capacity = result.capacity {
                        StatRow(label: "Capacity", value: String(format: "%.1f", capacity))
                    }
                    StatRow(label: "Breaths", value: "\(result.breathCount)")
                }
                .padding()
                .background(Color(.secondarySystemGroupedBackground))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }

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
        Task {
            for _ in 1...3 {
                try? await Task.sleep(for: .seconds(1))
                countdown -= 1
            }
            phase = .active
            startSensorReading()
        }
    }

    private func startSensorReading() {
        Task {
            while phase == .active {
                let time = Date().timeIntervalSince1970
                pressureDisplay = 30 + 20 * sin(time * 2)
                try? await Task.sleep(for: .milliseconds(100))
            }
        }
    }

    private func simulateBlow() {
        breathCount += 1
        withAnimation(.easeOut(duration: 0.3)) {
            pressureDisplay = Double.random(in: 60...95)
        }
    }

    private func finishExercise() {
        timer?.invalidate()
        timer = nil

        let resultData = ExerciseResultData(
            exerciseName: exerciseName,
            capacity: pressureDisplay,
            breathCount: breathCount
        )
        result = resultData
        viewModel.recordCompletion(exercise: exerciseName, unitId: unitId)
        phase = .complete
    }
}

struct StatRow: View {
    let label: String
    let value: String

    var body: some View {
        HStack {
            Text(label)
                .foregroundStyle(.secondary)
            Spacer()
            Text(value)
                .font(.headline)
        }
    }
}

enum ExercisePhase {
    case instructions, countdown, active, complete
}

struct ExerciseResultData {
    let exerciseName: String
    let capacity: Double?
    let breathCount: Int
}
