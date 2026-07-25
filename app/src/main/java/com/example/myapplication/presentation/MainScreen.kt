package com.example.myapplication.presentation

import android.annotation.SuppressLint
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.viewmodel.MainScreenViewmodel
import com.example.myapplication.viewmodel.UserViewModel

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(viewModel: MainScreenViewmodel = viewModel(), navController: NavController, userViewModel: UserViewModel) {
    val userLogin by userViewModel.userLogin.collectAsState()
    val selectedScreen = viewModel.selectedScreen
    Scaffold(
        topBar = { TopBar(
            selectedScreen,
            userLogin = userLogin
        ) },
        bottomBar = {
            AppButtonBar(
                onClick = {input ->
                    viewModel.changeScreen(input)
                }
            )
        },
    ) { innerPadding ->
        when (selectedScreen) {
            "Профиль" -> Profile(navController)
            "Препараты" -> Drugs(navController, innerPadding)
            "Мои сады" -> MyGardens(navController,innerPadding)
            "Календарь" -> Calendar(innerPadding, navController)
        }
    }
}