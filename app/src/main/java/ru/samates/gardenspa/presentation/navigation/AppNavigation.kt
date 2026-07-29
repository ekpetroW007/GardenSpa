package ru.samates.gardenspa.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.samates.gardenspa.presentation.DrugAdd
import ru.samates.gardenspa.presentation.DrugInfo
import ru.samates.gardenspa.presentation.GardenAdd
import ru.samates.gardenspa.presentation.MainScreen
import ru.samates.gardenspa.presentation.PlantAdd
import ru.samates.gardenspa.presentation.PlantDetails
import ru.samates.gardenspa.presentation.AllPlants
import ru.samates.gardenspa.presentation.Registration
import ru.samates.gardenspa.viewmodel.UserViewModel

@Composable
fun AppNavigation(userViewModel: UserViewModel) {
    val navController = rememberNavController()

    val isRegistered by userViewModel.isRegistered.collectAsState()

    val startDestination = if (isRegistered) {
        AppDestinations.MAINSCREEN_ROUTE
    } else {
        AppDestinations.REGISTRATION_ROUTE
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = AppDestinations.REGISTRATION_ROUTE) {
            Registration(
                navController = navController,
                userViewModel = userViewModel
            )
        }
        composable(route = AppDestinations.MAINSCREEN_ROUTE) {
            MainScreen(navController = navController, userViewModel = userViewModel)
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
        composable(route = AppDestinations.PLANT_ADD) { backStackEntry ->
            PlantAdd(
                navController = navController,
                selectedDate = backStackEntry.arguments?.getString("selectedDate").orEmpty()
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
