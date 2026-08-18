package ru.samates.gardenspa.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController

@Composable
fun MainScreen(
    navController: NavController,
    selectedScreen: String,
    onScreenSelected: (String) -> Unit
) {
    val currentScreen = if (selectedScreen == "Препараты" || selectedScreen == "Рецепты") "Справочник" else selectedScreen
    BotanicalBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (currentScreen == "Главная") {
                    TopBar()
                }
            },
            bottomBar = {
                AppButtonBar(selectedScreen = currentScreen, onClick = onScreenSelected)
            }
        ) { innerPadding ->
            when (currentScreen) {
                "Главная", "Профиль" -> Profile(
                    navController = navController,
                    onScreenSelected = onScreenSelected,
                    modifier = Modifier.padding(innerPadding)
                )
                "Справочник" -> Drugs(navController, innerPadding)
                "Мои сады" -> MyGardens(navController, innerPadding)
                "Календарь" -> Calendar(innerPadding, navController)
            }
        }
    }
}
