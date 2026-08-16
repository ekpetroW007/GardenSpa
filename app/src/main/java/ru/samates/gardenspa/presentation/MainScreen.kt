package ru.samates.gardenspa.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ru.samates.gardenspa.viewmodel.MainScreenViewmodel
import ru.samates.gardenspa.viewmodel.UserViewModel

@Composable
fun MainScreen(
    viewModel: MainScreenViewmodel = viewModel(),
    navController: NavController,
    userViewModel: UserViewModel
) {
    val selectedScreen = viewModel.selectedScreen
    BotanicalBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (selectedScreen == "Главная") {
                    TopBar()
                }
            },
            bottomBar = {
                AppButtonBar(selectedScreen = selectedScreen, onClick = viewModel::changeScreen)
            }
        ) { innerPadding ->
            when (selectedScreen) {
                "Главная", "Профиль" -> Profile(
                    navController = navController,
                    onScreenSelected = viewModel::changeScreen,
                    modifier = Modifier.padding(innerPadding),
                    userViewModel = userViewModel
                )
                "Препараты" -> Drugs(navController, innerPadding)
                "Рецепты" -> FolkRecipes(innerPadding)
                "Мои сады" -> MyGardens(navController, innerPadding)
                "Календарь" -> Calendar(innerPadding, navController)
            }
        }
    }
}
