package ru.samates.gardenspa.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import ru.samates.gardenspa.presentation.DrugAdd
import ru.samates.gardenspa.presentation.DrugInfo
import ru.samates.gardenspa.presentation.GardenAdd
import ru.samates.gardenspa.presentation.MainScreen
import ru.samates.gardenspa.presentation.PlantAdd
import ru.samates.gardenspa.presentation.PlantDetails
import ru.samates.gardenspa.presentation.AllPlants

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppDestinations.MAINSCREEN_ROUTE
    ) {
        composable(route = AppDestinations.MAINSCREEN_ROUTE) { backStackEntry ->
            val selectedScreen by backStackEntry.savedStateHandle.getStateFlow("selectedScreen", "Главная").collectAsState()
            MainScreen(
                navController = navController,
                selectedScreen = selectedScreen,
                onScreenSelected = { backStackEntry.savedStateHandle["selectedScreen"] = it }
            )
        }
        composable(route = AppDestinations.DRUG_ADD_ROUTE) {
            DrugAdd(navController = navController)
        }
        composable(route = AppDestinations.DRUG_EDIT_ROUTE) { backStackEntry ->
            val drugId = backStackEntry.arguments?.getString("drugId")?.toIntOrNull() ?: return@composable
            DrugAdd(navController = navController, drugId = drugId)
        }
        composable(route = AppDestinations.GARDEN_ADD) {
            GardenAdd(navController = navController)
        }
        composable(route = "drugInfoScreen/{drugName}/{purpose}/{consumptionRate}") { backStackEntry ->
            val drugName = backStackEntry.arguments?.getString("drugName")
            val purpose = backStackEntry.arguments?.getString("purpose")
            val consumptionRate = backStackEntry.arguments?.getString("consumptionRate")
            DrugInfo(navController = navController, drugName, purpose, consumptionRate)
        }
        composable(
            route = AppDestinations.PLANT_ADD,
            arguments = listOf(
                navArgument("gardenId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            PlantAdd(
                navController = navController,
                selectedDate = backStackEntry.arguments?.getString("selectedDate").orEmpty(),
                preselectedGardenId = backStackEntry.arguments?.getInt("gardenId")?.takeIf { it > 0 }
            )
        }
        composable(route = AppDestinations.PLANT_EDIT) { backStackEntry ->
            val plantId = backStackEntry.arguments?.getString("plantId")?.toIntOrNull() ?: return@composable
            PlantAdd(
                navController = navController,
                selectedDate = "",
                plantId = plantId,
                preselectedGardenId = null
            )
        }
        composable(route = AppDestinations.PLANT_DETAILS) { backStackEntry ->
            val plantId = backStackEntry.arguments?.getString("plantId")?.toIntOrNull() ?: return@composable
            PlantDetails(navController = navController, plantId = plantId)
        }
        composable(route = AppDestinations.ALL_PLANTS) {
            AllPlants(navController = navController)
        }
    }
}
