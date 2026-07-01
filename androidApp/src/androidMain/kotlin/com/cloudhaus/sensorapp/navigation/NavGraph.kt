package com.cloudhaus.sensorapp.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cloudhaus.sensorapp.ui.dashboard.DashboardScreen
import com.cloudhaus.sensorapp.ui.exercise.ExerciseScreen
import com.cloudhaus.sensorapp.ui.settings.SettingsScreen
import com.cloudhaus.sensorapp.ui.unit.UnitDetailScreen
import com.cloudhaus.sensorapp.viewmodel.SensorViewModel

@Composable
fun SensorNavGraph(viewModel: SensorViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                viewModel = viewModel,
                onUnitClick = { unitId ->
                    navController.navigate("unit/$unitId")
                },
                onSettingsClick = { navController.navigate("settings") },
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            "unit/{unitId}",
            arguments = listOf(navArgument("unitId") { type = NavType.IntType }),
        ) { backStackEntry ->
            val unitId = backStackEntry.arguments?.getInt("unitId") ?: 1
            UnitDetailScreen(
                unitId = unitId,
                viewModel = viewModel,
                onExerciseClick = { exerciseName ->
                    navController.navigate("exercise/$unitId/$exerciseName")
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            "exercise/{unitId}/{exerciseName}",
            arguments = listOf(
                navArgument("unitId") { type = NavType.IntType },
                navArgument("exerciseName") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val unitId = backStackEntry.arguments?.getInt("unitId") ?: 1
            val exerciseName = backStackEntry.arguments?.getString("exerciseName") ?: ""
            ExerciseScreen(
                exerciseName = exerciseName,
                unitId = unitId,
                viewModel = viewModel,
                onFinish = { navController.popBackStack("dashboard", inclusive = false) },
            )
        }
    }
}
