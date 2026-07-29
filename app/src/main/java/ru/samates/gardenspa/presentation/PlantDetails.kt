package ru.samates.gardenspa.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ru.samates.gardenspa.BookeeperApp
import ru.samates.gardenspa.data.database.entity.resolvedCardId
import ru.samates.gardenspa.domain.RepeatType
import ru.samates.gardenspa.domain.recurrenceDescription
import ru.samates.gardenspa.domain.toPlantCards
import ru.samates.gardenspa.notifications.TreatmentReminderScheduler
import ru.samates.gardenspa.presentation.navigation.AppDestinations
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Forest700
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist
import ru.samates.gardenspa.viewmodel.PlantsViewmodel
import ru.samates.gardenspa.viewmodel.PlantsViewmodelFactory
import ru.samates.gardenspa.viewmodel.ProceduresViewmodel
import ru.samates.gardenspa.viewmodel.ProceduresViewmodelFactory

@Composable
fun PlantDetails(navController: NavController, plantId: Int) {
    val app = LocalContext.current.applicationContext as BookeeperApp
    val plantsVm: PlantsViewmodel = viewModel(factory = PlantsViewmodelFactory(app.repository))
    val proceduresVm: ProceduresViewmodel = viewModel(factory = ProceduresViewmodelFactory(app.repository))
    val plants by plantsVm.plants.collectAsState()
    val procedures by proceduresVm.procedures.collectAsState()
    val selectedPlant = plants.firstOrNull { it.id == plantId }
    val cardRows = selectedPlant?.let { selected ->
        plants.filter { it.resolvedCardId == selected.resolvedCardId }.sortedBy { it.id }
    }.orEmpty()
    val plant = cardRows.firstOrNull()
    val cardPlantIds = cardRows.map { it.id }.toSet()
    val history = procedures.filter { it.plantId in cardPlantIds && it.status == "COMPLETED" }
    var editorOpen by remember { mutableStateOf(false) }
    var scheduleType by remember { mutableStateOf(RepeatType.MONTHLY) }
    var intervalText by remember { mutableStateOf("1") }
    var savedMessage by remember { mutableStateOf(false) }

    LaunchedEffect(plant?.repeatType, plant?.repeatInterval, editorOpen) {
        if (!editorOpen && plant != null) {
            scheduleType = runCatching { RepeatType.valueOf(plant.repeatType) }
                .getOrDefault(RepeatType.MONTHLY)
                .takeUnless { it == RepeatType.NONE } ?: RepeatType.MONTHLY
            intervalText = plant.repeatInterval.coerceAtLeast(1).toString()
        }
    }

    BotanicalBackground {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Карточка растения", plant?.gardenName, onBack = { navController.popBackStack() })
            if (plant == null) {
                Column(Modifier.padding(18.dp)) { EmptyGlassState("Растение не найдено", "Возможно, оно было удалено") }
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        GlassCard(Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(plant.plantName, style = MaterialTheme.typography.headlineLarge, color = Cream)
                                Text("Уход начат ${plant.creationDate}", color = Mist)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Metric(plant.gardenName, "сад", Modifier.weight(1f))
                                    Metric(plant.drugName, "препарат", Modifier.weight(1f))
                                }
                                Text(
                                    "Процедур: ${cardRows.size} · ${plant.recurrenceDescription()}",
                                    color = Cream
                                )
                                SecondaryAction(
                                    text = "Редактировать карточку",
                                    onClick = { navController.navigate(AppDestinations.plantEdit(plant.id)) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                SecondaryAction(
                                    text = if (editorOpen) "Закрыть настройку" else "Настроить удобрение",
                                    onClick = {
                                        editorOpen = !editorOpen
                                        savedMessage = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (savedMessage) {
                                    Text("График обновлён", color = Leaf300)
                                }
                            }
                        }
                    }
                    if (editorOpen) {
                        item {
                            FertilizingPeriodEditor(
                                selectedType = scheduleType,
                                intervalText = intervalText,
                                onTypeSelected = { scheduleType = it },
                                onIntervalChanged = { intervalText = it.filter(Char::isDigit).take(3) },
                                onPreset = { type, interval ->
                                    scheduleType = type
                                    intervalText = interval.toString()
                                },
                                onCancel = { editorOpen = false },
                                onSave = {
                                    val interval = intervalText.toIntOrNull()?.coerceIn(1, 365) ?: 1
                                    plantsVm.updateFertilizingPeriod(cardRows, scheduleType, interval) {
                                        TreatmentReminderScheduler.refreshNow(app)
                                        savedMessage = true
                                    }
                                    editorOpen = false
                                }
                            )
                        }
                    }
                    item { SectionTitle("Процедуры") }
                    items(cardRows, key = { "card-procedure:${it.id}" }) { procedure ->
                        GlassCard(Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(procedure.taskName, color = Cream, style = MaterialTheme.typography.titleMedium)
                                Text(procedure.drugName, color = Leaf300)
                                Text(procedure.recurrenceDescription(), color = Mist)
                            }
                        }
                    }
                    item { SectionTitle("История ухода") }
                    if (history.isEmpty()) {
                        item { EmptyGlassState("История пока пуста", "Выполненные процедуры появятся здесь") }
                    }
                    items(history, key = { it.id }) { procedure ->
                        GlassCard(Modifier.fillMaxWidth()) {
                            Column {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(procedure.procedureName, color = Cream, style = MaterialTheme.typography.titleMedium)
                                    Text("Готово", color = Leaf300)
                                }
                                Text("Запланировано: ${procedure.scheduledDate}", color = Mist)
                                Text("Выполнено: ${procedure.completedDate ?: "—"}", color = Mist)
                                if (procedure.note.isNotBlank()) Text(procedure.note, color = Cream, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FertilizingPeriodEditor(
    selectedType: RepeatType,
    intervalText: String,
    onTypeSelected: (RepeatType) -> Unit,
    onIntervalChanged: (String) -> Unit,
    onPreset: (RepeatType, Int) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    val interval = intervalText.toIntOrNull()
    val unit = when (selectedType) {
        RepeatType.DAILY -> "дн."
        RepeatType.WEEKLY -> "нед."
        RepeatType.MONTHLY -> "мес."
        RepeatType.YEARLY -> "г."
        RepeatType.CUSTOM -> "дн."
        RepeatType.NONE -> ""
    }
    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Периодичность удобрения", color = Cream, style = MaterialTheme.typography.titleLarge)
            Text("Выберите готовый вариант или задайте свой интервал", color = Mist)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple("7 дней", RepeatType.DAILY, 7),
                    Triple("14 дней", RepeatType.DAILY, 14),
                    Triple("30 дней", RepeatType.DAILY, 30)
                ).forEach { preset ->
                    FilterChip(
                        selected = selectedType == preset.second && interval == preset.third,
                        onClick = { onPreset(preset.second, preset.third) },
                        label = { Text(preset.first) },
                        modifier = Modifier.weight(1f),
                        colors = fertilizingChipColors()
                    )
                }
            }
            Text("Единица периода", color = Mist, style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    RepeatType.DAILY to "Дни",
                    RepeatType.WEEKLY to "Недели",
                    RepeatType.MONTHLY to "Месяцы",
                    RepeatType.YEARLY to "Годы"
                ).forEach { option ->
                    FilterChip(
                        selected = selectedType == option.first,
                        onClick = { onTypeSelected(option.first) },
                        label = { Text(option.second) },
                        modifier = Modifier.weight(1f),
                        colors = fertilizingChipColors()
                    )
                }
            }
            OutlinedTextField(
                value = intervalText,
                onValueChange = onIntervalChanged,
                label = { Text("Каждые ($unit)") },
                supportingText = { Text("От 1 до 365", color = Mist) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = glassTextFieldColors(),
                shape = CompactGlassShape,
                modifier = Modifier.fillMaxWidth()
            )
            if (interval != null && interval > 0) {
                Text("Удобрять каждые $interval $unit · без даты окончания", color = Leaf300)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryAction("Отмена", onCancel, Modifier.weight(1f))
                PrimaryAction(
                    "Сохранить",
                    onSave,
                    Modifier.weight(1f),
                    enabled = interval != null && interval in 1..365
                )
            }
        }
    }
}

@Composable
private fun fertilizingChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = Leaf300,
    selectedLabelColor = Color(0xFF071D17),
    containerColor = Forest700,
    labelColor = Cream
)

@Composable
fun AllPlants(navController: NavController) {
    val app = LocalContext.current.applicationContext as BookeeperApp
    val plantsVm: PlantsViewmodel = viewModel(factory = PlantsViewmodelFactory(app.repository))
    val plants by plantsVm.plants.collectAsState()
    val plantCards = plants.toPlantCards()
    BotanicalBackground {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Все растения", "${plantCards.size} в вашей коллекции", onBack = { navController.popBackStack() })
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (plantCards.isEmpty()) item { EmptyGlassState("Растений пока нет", "Добавьте первое растение из календаря") }
                items(plantCards, key = { it.cardId }) { card ->
                    val plant = card.primary
                    GlassCard(Modifier.fillMaxWidth(), onClick = { navController.navigate(AppDestinations.plantDetails(plant.id)) }) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(plant.plantName, style = MaterialTheme.typography.titleLarge, color = Cream)
                                Text(plant.gardenName, color = Leaf300)
                                Text(card.procedures.joinToString(" · ") { it.taskName }, color = Mist, maxLines = 2)
                            }
                            Text("›", color = Leaf300, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }
        }
    }
}
