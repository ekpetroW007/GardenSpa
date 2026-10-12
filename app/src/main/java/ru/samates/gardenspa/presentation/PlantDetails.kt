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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ru.samates.gardenspa.BookeeperApp
import ru.samates.gardenspa.data.database.entity.resolvedCardId
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.domain.ProgramProductCatalog
import ru.samates.gardenspa.domain.ProgramVariants
import ru.samates.gardenspa.domain.ScheduledTreatment
import ru.samates.gardenspa.domain.nextSeasonScheduledDate
import ru.samates.gardenspa.domain.nextSeasonImportKey
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
import ru.samates.gardenspa.ui.theme.Danger
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
    var repeatSaving by remember { mutableStateOf(false) }
    var repeatError by remember { mutableStateOf<String?>(null) }
    var nextSeasonId by rememberSaveable { mutableStateOf<Int?>(null) }
    var nextSeasonSaving by remember { mutableStateOf(false) }
    var nextSeasonError by remember { mutableStateOf<String?>(null) }
    var variantSaving by remember { mutableStateOf(false) }
    var commentId by rememberSaveable { mutableStateOf<Int?>(null) }
    var commentDraft by rememberSaveable { mutableStateOf("") }
    val completeWork = rememberCompleteWorkAction(proceduresVm,
        hasNextSeason = { record -> plants.any { it.programImportKey == record.nextSeasonImportKey } })
    val undoCompletion = rememberUndoCompletionAction(proceduresVm,
        hasNextSeason = { record -> plants.any { it.programImportKey == record.nextSeasonImportKey } })

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
                    repeatError?.let { message -> item { Text(message, color = Danger) } }
                    items(cardRows.sortedBy { row ->
                        procedures.firstOrNull { it.plantId == row.id && it.scheduledDate == row.creationDate }
                            ?.rescheduledDate ?: row.creationDate
                    }, key = { "card-procedure:${it.id}" }) { procedure ->
                        val recorded = procedures.firstOrNull {
                            it.plantId == procedure.id && it.scheduledDate == procedure.creationDate
                        }
                        val plannedDate = recorded?.rescheduledDate ?: procedure.creationDate
                        val treatment = ScheduledTreatment(procedure, LocalDate.parse(procedure.creationDate),
                            LocalDate.parse(plannedDate), recorded?.status == "COMPLETED", recorded?.rescheduledDate != null)
                        GlassCard(Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(procedure.taskName, color = Cream, style = MaterialTheme.typography.titleMedium)
                                if (!ProgramVariants.isVariantWork(procedure)) {
                                    ProgramProductSummary(procedure.programStepId, procedure.drugName,
                                        procedure.programNote, procedure.taskName, personal = procedure.drugId != null)
                                }
                                Text("Дата: ${plannedDate.toRussianDateOrSelf()}", color = Mist)
                                if (ProgramVariants.isVariantWork(procedure)) {
                                    ProgramVariantChooser(procedure.programStepId, procedure.selectedVariantId, procedure.variantChoices,
                                        onSelect = { variant, choices ->
                                            variantSaving = true
                                            plantsVm.selectProgramVariant(procedure.id, variant, choices,
                                                onSaved = { variantSaving = false; repeatError = null; TreatmentReminderScheduler.refreshNow(app) },
                                                onError = { variantSaving = false; repeatError = it.message ?: "Не удалось выбрать вариант" })
                                        }, enabled = !variantSaving,
                                        completed = recorded?.status == "COMPLETED",
                                        completedYears = ProgramVariants.completedYears(procedure, history),
                                        onComplete = { completeWork(treatment) },
                                        onUndoCompletion = { recorded?.let(undoCompletion) })
                                } else if (treatment.completed) {
                                    Row { TwigCheckMark(); Text("Выполнено", color = Leaf300) }
                                    SecondaryAction("Отменить выполнение", { recorded?.let(undoCompletion) },
                                        Modifier.fillMaxWidth().semantics { contentDescription = "Отменить выполнение: ${procedure.taskName}" })
                                } else {
                                    PrimaryAction("Выполнено", { completeWork(treatment) },
                                        Modifier.fillMaxWidth().semantics { contentDescription = "Выполнить работу: ${procedure.taskName}" })
                                }
                                if (procedure.programId != null) {
                                    ProgramRepeatSelector(procedure.recurrenceDescription(), onSelected = { interval ->
                                        repeatSaving = true
                                        repeatError = null
                                        plantsVm.updateProgramRepeat(procedure, interval,
                                            onSaved = { repeatSaving = false; TreatmentReminderScheduler.refreshNow(app) },
                                            onError = { repeatSaving = false; repeatError = it })
                                    }, enabled = !repeatSaving)
                                } else Text(procedure.recurrenceDescription(), color = Mist)
                                if (procedure.programNote.isNotBlank()) {
                                    ExpandableInfo("Инструкция и ограничения", procedure.programNote)
                                }
                                if (!ProgramVariants.isVariantWork(procedure) && ProgramProductCatalog.alternatives(procedure.programId, procedure.programStepId).isNotEmpty()) {
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
                        GlassCard(Modifier.fillMaxWidth().testTag("archive:${procedure.id}")) {
                            Column {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(procedure.procedureName, color = Cream, style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f))
                                    Text("Готово", color = Leaf300, maxLines = 1)
                                }
                                Text("Запланировано: ${procedure.scheduledDate.toRussianDateOrSelf()}", color = Mist)
                                Text("Выполнено: ${procedure.completedDate?.toRussianDateOrSelf() ?: "—"}", color = Mist)
                                Text(if (procedure.drugName.isBlank()) "Препарат не указан" else procedure.drugName.toDrugDisplayText(), color = Leaf300)
                                SecondaryAction("Отменить выполнение", { undoCompletion(procedure) }, Modifier.fillMaxWidth())
                                if (procedure.comment.isNotBlank()) Text("Комментарий: ${procedure.comment}", color = Cream)
                                SecondaryAction(if (procedure.comment.isBlank()) "Добавить комментарий" else "Изменить комментарий", {
                                    commentDraft = procedure.comment; commentId = procedure.id
                                }, Modifier.fillMaxWidth())
                                if (procedure.note.isNotBlank()) {
                                    ExpandableInfo("Инструкция на момент выполнения", procedure.note)
                                }
                                val nextSeasonDate = procedure.nextSeasonScheduledDate(plants, procedures)
                                if (nextSeasonDate != null) {
                                    Text("Запланировано на ${nextSeasonDate.toRussianDate()}", color = Leaf300)
                                } else {
                                    SecondaryAction("Сохранить на следующий сезон", {
                                        nextSeasonId = procedure.id
                                        nextSeasonError = null
                                    }, Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    history.firstOrNull { it.id == nextSeasonId }?.let { archived ->
        NextSeasonDialog(archived, nextSeasonSaving, nextSeasonError,
            onDismiss = { comment ->
                nextSeasonSaving = true
                proceduresVm.updateCompletionComment(archived.id, comment,
                    onSaved = { nextSeasonSaving = false; nextSeasonId = null; nextSeasonError = null },
                    onError = { nextSeasonSaving = false; nextSeasonError = it.message })
            },
            onConfirm = { comment ->
                nextSeasonSaving = true
                nextSeasonError = null
                proceduresVm.updateCompletionComment(archived.id, comment, onSaved = {
                proceduresVm.scheduleNextSeason(archived.id,
                    onSaved = {
                        nextSeasonSaving = false
                        nextSeasonId = null
                        TreatmentReminderScheduler.refreshNow(app)
                    },
                    onError = { nextSeasonSaving = false; nextSeasonError = it.message ?: "Не удалось сохранить работу" })
                }, onError = { nextSeasonSaving = false; nextSeasonError = it.message })
            })
    }

    commentId?.let { id ->
        AlertDialog(onDismissRequest = { if (!nextSeasonSaving) commentId = null },
            containerColor = ru.samates.gardenspa.ui.theme.Forest900,
            title = { Text("Комментарий к выполненной работе", color = Cream) },
            text = { Column {
                OutlinedTextField(commentDraft, { commentDraft = it.take(2000) }, minLines = 3, maxLines = 6,
                    colors = glassTextFieldColors(), enabled = !nextSeasonSaving)
                nextSeasonError?.let { Text(it, color = Danger) }
            } },
            confirmButton = { TextButton(enabled = !nextSeasonSaving, onClick = {
                nextSeasonSaving = true
                proceduresVm.updateCompletionComment(id, commentDraft,
                    onSaved = { nextSeasonSaving = false; commentId = null; nextSeasonError = null },
                    onError = { nextSeasonSaving = false; nextSeasonError = it.message })
            }) { Text("Сохранить", color = Leaf300) } },
            dismissButton = { TextButton(enabled = !nextSeasonSaving, onClick = { commentId = null }) { Text("Отмена", color = Mist) } })
    }

    productWork?.let { work ->
        ProgramProductDialog(
            programId = requireNotNull(work.programId),
            stepId = if (afterInspection) null else work.programStepId,
            initialDate = if (afterInspection) LocalDate.now() else runCatching { LocalDate.parse(work.creationDate) }.getOrDefault(LocalDate.now()),
            initialReminder = work.reminderDaysBefore,
            saving = productSaving, error = productError,
            onDismiss = { if (!productSaving) productWork = null },
            onCustomConfirm = { drug, problem, cultivation, date, reminder ->
                productSaving = true; productError = null
                plantsVm.saveProgramProduct(work, "", date, reminder, afterInspection, problem, cultivation,
                    onSaved = {
                        TreatmentReminderScheduler.refreshNow(app)
                        productSaving = false; productWork = null
                    }, onError = { productError = it; productSaving = false }, personalProduct = drug)
            },
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
