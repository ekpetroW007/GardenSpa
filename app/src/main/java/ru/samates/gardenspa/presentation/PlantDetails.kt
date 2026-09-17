package ru.samates.gardenspa.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ru.samates.gardenspa.BookeeperApp
import ru.samates.gardenspa.data.database.entity.resolvedCardId
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.domain.ProgramProductCatalog
import java.time.LocalDate
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

@Composable
fun PlantDetails(navController: NavController, plantId: Int) {
    val app = LocalContext.current.applicationContext as BookeeperApp
    val plantsVm: PlantsViewmodel = viewModel(factory = PlantsViewmodelFactory(app.repository))
    val proceduresVm: ProceduresViewmodel = viewModel(factory = ProceduresViewmodelFactory(app.repository))
    val plants by plantsVm.plants.collectAsState()
    val procedures by proceduresVm.procedures.collectAsState()
    val selectedPlant = plants.firstOrNull { it.id == plantId } ?: procedures
        .firstOrNull { it.plantId == plantId && it.plantCardId.isNotBlank() }
        ?.let { archived -> plants.firstOrNull { it.resolvedCardId == archived.plantCardId } }
    val cardRows = selectedPlant?.let { selected ->
        plants.filter { it.resolvedCardId == selected.resolvedCardId }.sortedBy { it.id }
    }.orEmpty()
    val plant = cardRows.firstOrNull()
    val cardPlantIds = cardRows.map { it.id }.toSet()
    val history = procedures.filter {
        (it.plantCardId == plant?.resolvedCardId || it.plantId in cardPlantIds) && it.status == "COMPLETED"
    }.sortedByDescending { it.completedDate }
    var deleteConfirmationOpen by remember { mutableStateOf(false) }
    var productWork by remember { mutableStateOf<PlantEntity?>(null) }
    var afterInspection by remember { mutableStateOf(false) }
    var productSaving by remember { mutableStateOf(false) }
    var productError by remember { mutableStateOf<String?>(null) }

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
                                if (!plant.photoUri.isNullOrBlank()) {
                                    PlantPhoto(plant.photoUri, plant.plantName, Modifier.fillMaxWidth().height(190.dp))
                                }
                                Text(plant.plantName, style = MaterialTheme.typography.headlineLarge, color = Cream)
                                if (plant.programId != null) {
                                    Text("Готовая программа · версия ${plant.programVersion ?: 1}", color = Leaf300)
                                }
                                Text("Уход начат ${cardRows.minOfOrNull { it.creationDate }?.toRussianDateOrSelf() ?: plant.creationDate.toRussianDateOrSelf()}", color = Mist)
                                Text("Сад: ${plant.gardenName}", color = Leaf300)
                                if (plant.programId == null) {
                                    Text(plant.drugName.toDrugDisplayText(), color = Leaf300)
                                }
                                Text(
                                    text = if (plant.programId != null) {
                                        "Работ по уходу: ${cardRows.size} · индивидуальное расписание"
                                    } else {
                                        "Работ по уходу: ${cardRows.size}\nПовтор: ${plant.recurrenceDescription()}"
                                    },
                                    color = Cream,
                                    modifier = Modifier.fillMaxWidth()
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
                    if (ProgramProductCatalog.supports(plant.programId)) {
                        item {
                            GlassCard(Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("После осмотра обнаружена проблема", color = Cream, style = MaterialTheme.typography.titleLarge)
                                    Text("Если болезнь или вредитель подтвердились, добавьте отдельную обработку. Название проблемы можно не указывать.", color = Mist)
                                    SecondaryAction("Выбрать болезнь / вредителя и препарат", {
                                        afterInspection = true
                                        productError = null
                                        productWork = plant
                                    }, Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                    item { SectionTitle("План ухода") }
                    items(cardRows, key = { "card-procedure:${it.id}" }) { procedure ->
                        GlassCard(Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(procedure.taskName, color = Cream, style = MaterialTheme.typography.titleMedium)
                                Text(procedure.drugName.toDrugDisplayName(), color = Leaf300)
                                Text("Дата: ${procedure.creationDate.toRussianDateOrSelf()}", color = Mist)
                                Text(procedure.recurrenceDescription(), color = Mist)
                                if (procedure.programNote.isNotBlank()) {
                                    ExpandableInfo("Инструкция и ограничения", procedure.programNote)
                                }
                                if (ProgramProductCatalog.alternatives(procedure.programId, procedure.programStepId).isNotEmpty()) {
                                    val hasHistory = procedures.any { it.plantId == procedure.id }
                                    if (hasHistory) {
                                        Text("У работы есть история. Для другого средства добавьте отдельную обработку после осмотра.", color = Mist)
                                    } else {
                                        SecondaryAction("Выбрать препарат-аналог", {
                                            afterInspection = false
                                            productError = null
                                            productWork = procedure
                                        }, Modifier.fillMaxWidth())
                                    }
                                }
                            }
                        }
                    }
                    item { SectionTitle("Архив выполненных процедур") }
                    if (history.isEmpty()) {
                        item { EmptyGlassState("История пока пуста", "Выполненные процедуры появятся здесь") }
                    }
                    items(history, key = { it.id }) { procedure ->
                        GlassCard(Modifier.fillMaxWidth()) {
                            Column {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(procedure.procedureName, color = Cream, style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f))
                                    Text("Готово", color = Leaf300, maxLines = 1)
                                }
                                Text("Запланировано: ${procedure.scheduledDate.toRussianDateOrSelf()}", color = Mist)
                                Text("Выполнено: ${procedure.completedDate?.toRussianDateOrSelf() ?: "—"}", color = Mist)
                                Text(if (procedure.drugName.isBlank()) "Препарат не указан" else procedure.drugName.toDrugDisplayText(), color = Leaf300)
                                if (procedure.note.isNotBlank()) {
                                    ExpandableInfo("Инструкция на момент выполнения", procedure.note)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    productWork?.let { work ->
        ProgramProductDialog(
            programId = requireNotNull(work.programId),
            stepId = if (afterInspection) null else work.programStepId,
            initialDate = if (afterInspection) LocalDate.now() else runCatching { LocalDate.parse(work.creationDate) }.getOrDefault(LocalDate.now()),
            initialReminder = work.reminderDaysBefore,
            saving = productSaving, error = productError,
            onDismiss = { if (!productSaving) productWork = null },
            onConfirm = { product, problem, cultivation, date, reminder ->
                productSaving = true
                productError = null
                plantsVm.saveProgramProduct(work, product, date, reminder, afterInspection, problem, cultivation,
                    onSaved = {
                        TreatmentReminderScheduler.refreshNow(app)
                        productSaving = false
                        productWork = null
                    },
                    onError = { productError = it; productSaving = false }
                )
            }
        )
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
                            Text("${card.procedures.size} работ по уходу", color = Mist)
                            }
                            Text("Открыть", color = Leaf300)
                        }
                    }
                }
            }
        }
    }
}
