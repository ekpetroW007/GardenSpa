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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
fun DrugAdd(navController: NavController, drugId: Int? = null) {
    val app = LocalContext.current.applicationContext as BookeeperApp
    val viewModel: DrugsViewmodel = viewModel(factory = DrugsViewmodelFactory(app.repository))
    val drugs by viewModel.drugs.collectAsState()
    val drug = drugId?.let { id -> drugs.firstOrNull { it.id == id } }
    val editing = drugId != null
    var name by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var fieldsInitialized by remember(drugId) { mutableStateOf(false) }

    LaunchedEffect(drug, fieldsInitialized) {
        if (drug != null && !fieldsInitialized) {
            name = drug.name
            purpose = drug.purpose
            rate = drug.consumptionRate
            fieldsInitialized = true
        }
    }

    BotanicalBackground {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(
                if (editing) "Редактирование препарата" else "Новый препарат",
                if (editing) "Измените нужные параметры" else "Добавьте памятку по применению",
                onBack = { navController.popBackStack() }
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (editing && drug == null) {
                    EmptyGlassState("Препарат не найден", "Возможно, он был удалён")
                } else {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("Основная информация", style = MaterialTheme.typography.titleLarge, color = Cream)
                            Text("Данные будут доступны при создании процедуры", color = Mist)
                            OutlinedTextField(name, { name = it }, label = { Text("Название") }, keyboardOptions = SentenceKeyboardOptions, singleLine = true, colors = glassTextFieldColors(), shape = CompactGlassShape, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(purpose, { purpose = it }, label = { Text("Назначение") }, keyboardOptions = SentenceKeyboardOptions, minLines = 3, colors = glassTextFieldColors(), shape = CompactGlassShape, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(rate, { rate = it }, label = { Text("Норма расхода") }, keyboardOptions = SentenceKeyboardOptions, singleLine = true, colors = glassTextFieldColors(), shape = CompactGlassShape, modifier = Modifier.fillMaxWidth())
                            PrimaryAction(
                                if (editing) "Сохранить изменения" else "Сохранить препарат",
                                onClick = {
                                    if (drugId == null) {
                                        viewModel.addDrug(name.trim(), purpose.trim(), rate.trim())
                                        navController.popBackStack()
                                    } else {
                                        viewModel.updateDrug(
                                            id = drugId,
                                            name = name.trim(),
                                            purpose = purpose.trim(),
                                            consumptionRate = rate.trim(),
                                            onUpdated = { navController.popBackStack() }
                                        )
                                    }
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
}
