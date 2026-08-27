package ru.samates.gardenspa.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ru.samates.gardenspa.presentation.navigation.AppDestinations
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Mist
import ru.samates.gardenspa.viewmodel.UserViewModel

@Composable
fun Registration(navController: NavController, userViewModel: UserViewModel) {
    var login by remember { mutableStateOf("") }
    BotanicalBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("GardenSpa", color = Cream, style = MaterialTheme.typography.displayLarge)
            Text("Уход за садом — спокойно и вовремя", color = Mist)
            Spacer(Modifier.height(36.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Как к вам обращаться?", color = Cream, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "GardenSpa поможет создать сад, добавить растения и покажет, что нужно сделать сегодня.",
                        color = Mist
                    )
                    OutlinedTextField(
                        value = login,
                        onValueChange = { login = it },
                        label = { Text("Имя") },
                        keyboardOptions = SentenceKeyboardOptions,
                        singleLine = true,
                        colors = glassTextFieldColors(),
                        shape = CompactGlassShape,
                        modifier = Modifier.fillMaxWidth()
                    )
                    PrimaryAction(
                        "Начать настройку",
                        onClick = {
                            userViewModel.registerUser(login.trim())
                            navController.navigate(AppDestinations.MAINSCREEN_ROUTE) {
                                popUpTo(AppDestinations.REGISTRATION_ROUTE) { inclusive = true }
                            }
                        },
                        enabled = login.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
