package ru.samates.gardenspa.presentation

import androidx.compose.foundation.layout.padding
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
    val screenState = rememberSaveableStateHolder()
    val onBack: (() -> Unit)? = if (viewModel.canGoBack) viewModel::goBack else null
    BackHandler(enabled = viewModel.canGoBack) { viewModel.goBack() }
    BotanicalBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (selectedScreen == "Сегодня") {
                    if (onBack == null) TopBar() else ScreenHeader("GardenSpa", onBack = onBack)
                }
            },
            bottomBar = {
                AppButtonBar(selectedScreen = selectedScreen, onClick = viewModel::changeScreen)
            }
        ) { innerPadding ->
            screenState.SaveableStateProvider(selectedScreen) {
            when (selectedScreen) {
                "Сегодня", "Главная", "Профиль" -> Profile(
                    navController = navController,
                    onScreenSelected = viewModel::changeScreen,
                    modifier = Modifier.padding(innerPadding),
                    userViewModel = userViewModel
                )
                "Справочник" -> ReferenceHub(innerPadding, onBack, viewModel::changeScreen)
                "Мои средства" -> Drugs(navController, innerPadding, section = null, onBack = onBack)
                "Препараты" -> Drugs(navController, innerPadding, onBack = onBack)
                "Удобрения" -> Drugs(navController, innerPadding, ru.samates.gardenspa.domain.ProductSection.FERTILIZER, onBack)
                "Рецепты" -> FolkRecipes(innerPadding, onBack = onBack)
                "Баковые смеси" -> FolkRecipes(innerPadding, tankMixes = true, onBack = onBack)
                "Сады", "Мои сады" -> MyGardens(navController, innerPadding, onBack)
                "Календарь" -> Calendar(innerPadding, navController, onBack)
            }
            }
        }
    }
}
