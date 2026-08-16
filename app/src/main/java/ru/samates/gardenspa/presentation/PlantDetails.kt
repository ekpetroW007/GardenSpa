package ru.samates.gardenspa.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ru.samates.gardenspa.BookeeperApp
import ru.samates.gardenspa.data.database.entity.resolvedCardId
import ru.samates.gardenspa.domain.recurrenceDescription
import ru.samates.gardenspa.domain.toPlantCards
import ru.samates.gardenspa.domain.toDrugDisplayName
import ru.samates.gardenspa.domain.toDrugDisplayText
import ru.samates.gardenspa.notifications.TreatmentReminderScheduler
import ru.samates.gardenspa.presentation.navigation.AppDestinations
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist
import ru.samates.gardenspa.viewmodel.PlantsViewmodel
import ru.samates.gardenspa.viewmodel.PlantsViewmodelFactory
import ru.samates.gardenspa.viewmodel.ProceduresViewmodel
import ru.samates.gardenspa.viewmodel.ProceduresViewmodelFactory

@OptIn(ExperimentalFoundationApi::class)
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
    var deleteConfirmationOpen by remember { mutableStateOf(false) }

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
                                if (plant.programId != null) {
                                    Text("Готовая программа · версия ${plant.programVersion ?: 1}", color = Leaf300)
                                }
                                Text("Уход начат ${cardRows.minOfOrNull { it.creationDate } ?: plant.creationDate}", color = Mist)
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Сад: ${plant.gardenName}",
                                        color = Leaf300,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.weight(1f).basicMarquee()
                                    )
                                    Text(
                                        text = plant.drugName.toDrugDisplayText(),
                                        color = Leaf300,
                                        maxLines = 1,
                                        softWrap = false,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1f).basicMarquee()
                                    )
                                }
                                Text(
                                    text = if (plant.programId != null) {
                                        "Процедур: ${cardRows.size} · индивидуальное расписание"
                                    } else {
                                        "Процедур: ${cardRows.size}   Частота обработки: ${plant.recurrenceDescription()}"
                                    },
                                    color = Cream,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.fillMaxWidth().basicMarquee()
                                )
                                SecondaryAction(
                                    text = "Редактировать карточку",
                                    onClick = { navController.navigate(AppDestinations.plantEdit(plant.id)) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                DangerAction(
                                    text = "Удалить растение",
                                    onClick = { deleteConfirmationOpen = true },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    item { SectionTitle("Процедуры") }
                    items(cardRows, key = { "card-procedure:${it.id}" }) { procedure ->
                        GlassCard(Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(procedure.taskName, color = Cream, style = MaterialTheme.typography.titleMedium)
                                Text(procedure.drugName.toDrugDisplayName(), color = Leaf300)
                                Text("Дата начала: ${procedure.creationDate}", color = Mist)
                                Text(procedure.recurrenceDescription(), color = Mist)
                                if (procedure.programNote.isNotBlank()) {
                                    Text(procedure.programNote, color = Cream, modifier = Modifier.padding(top = 4.dp))
                                }
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

    if (deleteConfirmationOpen && plant != null) {
        DeleteConfirmationDialog(
            itemName = plant.plantName,
            onConfirm = {
                deleteConfirmationOpen = false
                plantsVm.deletePlantCard(plant) {
                    TreatmentReminderScheduler.refreshNow(app)
                    navController.popBackStack()
                }
            },
            onDismiss = { deleteConfirmationOpen = false }
        )
    }
}

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
