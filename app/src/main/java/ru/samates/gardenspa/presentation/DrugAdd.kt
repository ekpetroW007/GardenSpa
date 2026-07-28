package ru.samates.gardenspa.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ru.samates.gardenspa.BookeeperApp
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Mist
import ru.samates.gardenspa.viewmodel.DrugsViewmodel
import ru.samates.gardenspa.viewmodel.DrugsViewmodelFactory

@Composable
fun DrugAdd(navController: NavController) {
    val app = LocalContext.current.applicationContext as BookeeperApp
    val viewModel: DrugsViewmodel = viewModel(factory = DrugsViewmodelFactory(app.repository))
    var name by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }

    BotanicalBackground {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Новый препарат", "Добавьте памятку по применению", onBack = { navController.popBackStack() })
            Column(
                modifier = Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Основная информация", style = MaterialTheme.typography.titleLarge, color = Cream)
                        Text("Данные будут доступны при создании процедуры", color = Mist)
                        OutlinedTextField(name, { name = it }, label = { Text("Название") }, singleLine = true, colors = glassTextFieldColors(), shape = CompactGlassShape, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(purpose, { purpose = it }, label = { Text("Назначение") }, minLines = 3, colors = glassTextFieldColors(), shape = CompactGlassShape, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(rate, { rate = it }, label = { Text("Норма расхода") }, singleLine = true, colors = glassTextFieldColors(), shape = CompactGlassShape, modifier = Modifier.fillMaxWidth())
                        PrimaryAction(
                            "Сохранить препарат",
                            onClick = {
                                viewModel.addDrug(name.trim(), purpose.trim(), rate.trim())
                                navController.popBackStack()
                            },
                            enabled = name.isNotBlank() && purpose.isNotBlank() && rate.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
