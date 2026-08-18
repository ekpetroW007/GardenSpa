package ru.samates.gardenspa.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ru.samates.gardenspa.BookeeperApp
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Danger
import ru.samates.gardenspa.viewmodel.GardensViewmodel
import ru.samates.gardenspa.viewmodel.GardensViewmodelFactory

@Composable
fun GardenAdd(navController: NavController) {
    val app = LocalContext.current.applicationContext as BookeeperApp
    val viewModel: GardensViewmodel = viewModel(factory = GardensViewmodelFactory(app.repository))
    var name by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf(false) }

    BotanicalBackground {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Новый сад", "Создайте пространство для растений", onBack = { navController.popBackStack() })
            Column(
                modifier = Modifier.fillMaxSize().padding(18.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Дайте саду понятное имя", style = MaterialTheme.typography.titleLarge, color = Cream)
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Название сада") },
                            keyboardOptions = SentenceKeyboardOptions,
                            singleLine = true,
                            colors = glassTextFieldColors(),
                            shape = CompactGlassShape,
                            modifier = Modifier.fillMaxWidth()
                        )
                        PrimaryAction(
                            text = if (saving) "Создаём…" else "Создать сад",
                            enabled = name.isNotBlank() && !saving,
                            onClick = {
                                saving = true
                                saveError = false
                                viewModel.gardenAdd(
                                    name = name.trim(),
                                    onSaved = {
                                        navController.previousBackStackEntry?.savedStateHandle?.set("selectedScreen", "Мои сады")
                                        navController.popBackStack()
                                    },
                                    onError = { saving = false; saveError = true }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (saveError) Text("Не удалось создать сад. Попробуйте ещё раз.", color = Danger)
                    }
                }
            }
        }
    }
}
