package com.example.myapplication.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.presentation.DrugAdd
import com.example.myapplication.presentation.DrugInfo
import com.example.myapplication.presentation.GardenAdd
import com.example.myapplication.presentation.MainScreen
import com.example.myapplication.presentation.PlantAdd
import com.example.myapplication.presentation.Registration
import com.example.myapplication.viewmodel.UserViewModel

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
        composable(route = AppDestinations.GARDEN_ADD) {
            GardenAdd(navController = navController)
        }
        composable(route = "drugInfoScreen/{drugName}/{purpose}/{consumptionRate}") { backStackEntry ->
            val drugName = backStackEntry.arguments?.getString("drugName")
            val purpose = backStackEntry.arguments?.getString("purpose")
            val consumptionRate = backStackEntry.arguments?.getString("consumptionRate")
            DrugInfo(navController = navController, drugName, purpose, consumptionRate)
        }
        composable(route = AppDestinations.PLANT_ADD) {
            PlantAdd(navController = navController)
        }
    }
}