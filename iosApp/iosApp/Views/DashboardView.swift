import SwiftUI

struct DashboardView: View {
    @ObservedObject var viewModel: SensorViewModel

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                headerSection
                unitsSection
            }
            .padding()
        }
        .navigationTitle("Sensor App")
        .background(Color(.systemGroupedBackground))
    }

    private var headerSection: some View {
        VStack(spacing: 8) {
            Image(systemName: "lungs.fill")
                .font(.system(size: 60))
                .foregroundStyle(.teal)

            Text("Breathing Exercises")
                .font(.title2.bold())

            Text("Complete exercises to earn medals and unlock new units")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(.vertical, 24)
    }

    private var unitsSection: some View {
        VStack(spacing: 12) {
            ForEach(viewModel.units) { unit in
                NavigationLink {
                    UnitDetailView(unit: unit, viewModel: viewModel)
                } label: {
                    UnitCard(unit: unit)
                }
            }
        }
    }
}

struct UnitCard: View {
    let unit: ExerciseUnit

    var body: some View {
        HStack(spacing: 16) {
            ZStack {
                Circle()
                    .fill(unit.isUnlocked ? Color.teal : Color.gray.opacity(0.3))
                    .frame(width: 44, height: 44)

                if let medal = unit.medal {
                    Text(medal)
                        .font(.title2)
                } else if unit.isUnlocked {
                    Text("\(unit.id)")
                        .font(.headline)
                        .foregroundStyle(.white)
                } else {
                    Image(systemName: "lock.fill")
                        .foregroundStyle(.gray)
                }
            }

            VStack(alignment: .leading, spacing: 4) {
                Text(unit.name)
                    .font(.headline)
                    .foregroundStyle(unit.isUnlocked ? .primary : .secondary)

                Text(unit.exercises.joined(separator: ", "))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            if unit.isUnlocked {
                Image(systemName: "chevron.right")
                    .foregroundStyle(.secondary)
            }
        }
        .padding()
        .background(Color(.secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .opacity(unit.isUnlocked ? 1 : 0.6)
    }
}

#Preview {
    NavigationStack {
        DashboardView(viewModel: SensorViewModel())
    }
}
