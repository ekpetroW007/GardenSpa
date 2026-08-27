package ru.samates.gardenspa.others

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import ru.samates.gardenspa.presentation.PreferencesManager
import ru.samates.gardenspa.presentation.navigation.AppNavigation
import ru.samates.gardenspa.ui.theme.MyApplicationTheme
import ru.samates.gardenspa.viewmodel.UserViewModel

class MainActivity : ComponentActivity() {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var userViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferencesManager = PreferencesManager(this)
        userViewModel = UserViewModel(preferencesManager, (application as ru.samates.gardenspa.BookeeperApp).climateService)

        setContent {
            val largeInterface by userViewModel.largeInterface.collectAsState()
            val highContrast by userViewModel.highContrast.collectAsState()
            MyApplicationTheme(largeInterface = largeInterface, highContrast = highContrast) {
                AppNavigation(userViewModel = userViewModel)
            }
        }
    }

}


