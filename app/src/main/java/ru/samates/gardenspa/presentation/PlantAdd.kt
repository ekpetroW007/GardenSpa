package ru.samates.gardenspa.presentation

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ru.samates.gardenspa.BookeeperApp
import ru.samates.gardenspa.data.database.entity.DrugEntity
import ru.samates.gardenspa.data.database.entity.GardenEntity
import ru.samates.gardenspa.data.database.entity.resolvedCardId
import ru.samates.gardenspa.domain.RepeatEndType
import ru.samates.gardenspa.domain.RepeatType
import ru.samates.gardenspa.notifications.TreatmentReminderScheduler
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Danger
import ru.samates.gardenspa.ui.theme.Forest700
import ru.samates.gardenspa.ui.theme.Forest900
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist
import ru.samates.gardenspa.viewmodel.DrugsViewmodel
import ru.samates.gardenspa.viewmodel.DrugsViewmodelFactory
import ru.samates.gardenspa.viewmodel.GardensViewmodel
import ru.samates.gardenspa.viewmodel.GardensViewmodelFactory
import ru.samates.gardenspa.viewmodel.PlantsViewmodel
import ru.samates.gardenspa.viewmodel.PlantsViewmodelFactory
import ru.samates.gardenspa.viewmodel.TasksViewmodel
import ru.samates.gardenspa.viewmodel.TasksViewmodelFactory
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

private val repeatLabels = linkedMapOf(
    RepeatType.NONE to "Не повторять",
    RepeatType.DAILY to "Ежедневно",
    RepeatType.WEEKLY to "Еженедельно",
    RepeatType.MONTHLY to "Ежемесячно",
    RepeatType.YEARLY to "Ежегодно",
    RepeatType.CUSTOM to "Свой вариант"
)

private val endLabels = linkedMapOf(
    RepeatEndType.NEVER to "Бессрочно",
    RepeatEndType.UNTIL_DATE to "До даты",
    RepeatEndType.COUNT to "После количества повторов"
)

private val reminderLabels = linkedMapOf(
    0 to "В день процедуры",
    1 to "За 1 день",
    5 to "За 5 дней"
)

@Composable
fun PlantAdd(navController: NavController, selectedDate: String, plantId: Int? = null) {
    val context = LocalContext.current
    val app = context.applicationContext as BookeeperApp
    val plantsVm: PlantsViewmodel = viewModel(factory = PlantsViewmodelFactory(app.repository))
    val drugsVm: DrugsViewmodel = viewModel(factory = DrugsViewmodelFactory(app.repository))
    val gardensVm: GardensViewmodel = viewModel(factory = GardensViewmodelFactory(app.repository))
    val tasksVm: TasksViewmodel = viewModel(factory = TasksViewmodelFactory(app.repository))
    val drugs by drugsVm.drugs.collectAsState()
    val gardens by gardensVm.gardens.collectAsState()
    val plants by plantsVm.plants.collectAsState()
    val editingPlant = plantId?.let { id -> plants.firstOrNull { it.id == id } }
    val editingRows = editingPlant?.let { selected ->
        plants.filter { it.resolvedCardId == selected.resolvedCardId }.sortedBy { it.id }
    }.orEmpty()
    val requestedDate = remember(selectedDate) {
        runCatching { LocalDate.parse(selectedDate) }.getOrDefault(LocalDate.now())
    }
    val startDate = editingPlant?.creationDate
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: requestedDate
    val editing = plantId != null

    var plantName by remember { mutableStateOf("") }
    var taskNames by remember { mutableStateOf(listOf("")) }
    var selectedDrug by remember { mutableStateOf<DrugEntity?>(null) }
    var selectedGarden by remember { mutableStateOf<GardenEntity?>(null) }
    var repeatType by remember { mutableStateOf(RepeatType.NONE) }
    var intervalText by remember { mutableStateOf("1") }
    var weekDays by remember { mutableStateOf(setOf(startDate.dayOfWeek)) }
    var endType by remember { mutableStateOf(RepeatEndType.NEVER) }
    var endDate by remember { mutableStateOf(startDate.plusMonths(1)) }
    var countText by remember { mutableStateOf("10") }
    var reminderDaysBefore by remember { mutableStateOf(1) }
    var fieldsInitialized by remember(plantId) { mutableStateOf(false) }
    var addDrugDialogOpen by remember { mutableStateOf(false) }
    var pendingNewDrugName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(editingPlant, editingRows, drugs, gardens, fieldsInitialized) {
        if (editingPlant != null && !fieldsInitialized) {
            plantName = editingPlant.plantName
            taskNames = editingRows.map { it.taskName }.ifEmpty { listOf("") }
            selectedDrug = drugs.firstOrNull { it.id == editingPlant.drugId }
            selectedGarden = gardens.firstOrNull { it.id == editingPlant.gardenId }
            repeatType = runCatching { RepeatType.valueOf(editingPlant.repeatType) }.getOrDefault(RepeatType.NONE)
            intervalText = editingPlant.repeatInterval.coerceAtLeast(1).toString()
            weekDays = editingPlant.repeatDaysOfWeek
                .split(",")
                .mapNotNull { it.toIntOrNull() }
                .mapNotNull { value -> DayOfWeek.entries.firstOrNull { it.value == value } }
                .toSet()
                .ifEmpty { setOf(startDate.dayOfWeek) }
            endType = runCatching { RepeatEndType.valueOf(editingPlant.repeatEndType) }
                .getOrDefault(RepeatEndType.NEVER)
            endDate = editingPlant.repeatEndDate
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?: startDate.plusMonths(1)
            countText = editingPlant.repeatCount?.toString() ?: "10"
            reminderDaysBefore = editingPlant.reminderDaysBefore
                .takeIf(reminderLabels::containsKey)
                ?: 1
            fieldsInitialized = true
        }
    }

    LaunchedEffect(drugs, pendingNewDrugName) {
        val newDrugName = pendingNewDrugName ?: return@LaunchedEffect
        drugs
            .filter { it.name.equals(newDrugName, ignoreCase = true) }
            .maxByOrNull { it.id }
            ?.let { newDrug ->
                selectedDrug = newDrug
                pendingNewDrugName = null
            }
    }

    BotanicalBackground {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(
                if (editing) "Редактирование процедуры" else "Новая процедура",
                startDate.toString(),
                onBack = { navController.popBackStack() }
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (editing && editingPlant == null) {
                    EmptyGlassState("Растение не найдено", "Возможно, оно было удалено")
                } else {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Карточка растения", color = Cream, style = MaterialTheme.typography.titleLarge)
                            OutlinedTextField(
                                plantName,
                                { plantName = it },
                                label = { Text("Название растения") },
                                keyboardOptions = SentenceKeyboardOptions,
                                singleLine = true,
                                colors = glassTextFieldColors(),
                                shape = CompactGlassShape,
                                modifier = Modifier.fillMaxWidth()
                            )
                            SelectionMenu(
                                label = "Сад",
                                value = selectedGarden?.name ?: "Не выбран",
                                options = gardens,
                                optionLabel = { it.name },
                                onSelected = { selectedGarden = it }
                            )
                        }
                    }
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Процедуры", color = Cream, style = MaterialTheme.typography.titleLarge)
                            taskNames.forEachIndexed { index, taskName ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = taskName,
                                        onValueChange = { value ->
                                            taskNames = taskNames.toMutableList().also { it[index] = value }
                                        },
                                        label = { Text("Процедура ${index + 1}") },
                                        keyboardOptions = SentenceKeyboardOptions,
                                        singleLine = true,
                                        colors = glassTextFieldColors(),
                                        shape = CompactGlassShape,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (taskNames.size > 1) {
                                        Text(
                                            "×",
                                            color = Danger,
                                            style = MaterialTheme.typography.titleLarge,
                                            modifier = Modifier.clickable {
                                                taskNames = taskNames.filterIndexed { itemIndex, _ -> itemIndex != index }
                                            }
                                        )
                                    }
                                }
                            }
                            SecondaryAction(
                                "+ Добавить процедуру",
                                onClick = { taskNames = taskNames + "" },
                                modifier = Modifier.fillMaxWidth()
                            )
                            SelectionMenu(
                                label = "Препарат",
                                value = selectedDrug?.name ?: "Не выбран",
                                options = drugs,
                                optionLabel = { it.name },
                                onSelected = { selectedDrug = it },
                                addActionLabel = "+ Добавить новый препарат",
                                onAddAction = { addDrugDialogOpen = true }
                            )
                        }
                    }
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Общее расписание", color = Cream, style = MaterialTheme.typography.titleLarge)
                            SelectionMenu(
                                label = "Повтор",
                                value = repeatLabels.getValue(repeatType),
                                options = repeatLabels.keys.toList(),
                                optionLabel = { repeatLabels.getValue(it) },
                                onSelected = { repeatType = it }
                            )
                            SelectionMenu(
                                label = "Напоминание",
                                value = reminderLabels.getValue(reminderDaysBefore),
                                options = reminderLabels.keys.toList(),
                                optionLabel = { reminderLabels.getValue(it) },
                                onSelected = { reminderDaysBefore = it }
                            )
                            if (repeatType == RepeatType.CUSTOM) {
                                OutlinedTextField(
                                    intervalText,
                                    { intervalText = it.filter(Char::isDigit).take(3) },
                                    label = { Text("Интервал (в днях)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = glassTextFieldColors(),
                                    shape = CompactGlassShape,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (repeatType == RepeatType.WEEKLY) {
                                Text("Дни недели", color = Mist)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    DayOfWeek.entries.forEach { day ->
                                        val label = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")[day.value - 1]
                                        FilterChip(
                                            selected = day in weekDays,
                                            onClick = {
                                                weekDays = if (day in weekDays) (weekDays - day).ifEmpty { setOf(day) } else weekDays + day
                                            },
                                            label = { Text(label) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Leaf300,
                                                selectedLabelColor = Color(0xFF071D17),
                                                labelColor = Cream,
                                                containerColor = Forest700
                                            )
                                        )
                                    }
                                }
                            }
                            if (repeatType != RepeatType.NONE) {
                                SelectionMenu(
                                    label = "Окончание",
                                    value = endLabels.getValue(endType),
                                    options = endLabels.keys.toList(),
                                    optionLabel = { endLabels.getValue(it) },
                                    onSelected = { endType = it }
                                )
                                when (endType) {
                                    RepeatEndType.UNTIL_DATE -> SecondaryAction("До $endDate", {
                                        DatePickerDialog(context, { _, y, m, d -> endDate = LocalDate.of(y, m + 1, d) }, endDate.year, endDate.monthValue - 1, endDate.dayOfMonth).apply {
                                            datePicker.minDate = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                        }.show()
                                    }, Modifier.fillMaxWidth())
                                    RepeatEndType.COUNT -> OutlinedTextField(countText, { countText = it.filter(Char::isDigit).take(4) }, label = { Text("Количество повторов") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, colors = glassTextFieldColors(), shape = CompactGlassShape, modifier = Modifier.fillMaxWidth())
                                    RepeatEndType.NEVER -> Unit
                                }
                            }
                        }
                    }
                    PrimaryAction(
                        if (editing) "Сохранить изменения" else "Сохранить процедуру",
                        onClick = {
                            val interval = intervalText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                            val normalizedTaskNames = taskNames.map { it.trim() }
                            plantsVm.savePlantCard(
                                plantId = plantId,
                                plantName = plantName.trim(),
                                taskNames = normalizedTaskNames,
                                wateringInterval = interval,
                                creationDate = startDate.toString(),
                                drugId = selectedDrug?.id,
                                gardenId = selectedGarden?.id,
                                drugName = selectedDrug?.name ?: "Не выбран",
                                gardenName = selectedGarden?.name ?: "Не выбран",
                                repeatType = repeatType.name,
                                repeatInterval = interval,
                                repeatDaysOfWeek = weekDays.sortedBy { it.value }.joinToString(",") { it.value.toString() },
                                repeatEndType = if (repeatType == RepeatType.NONE) RepeatEndType.NEVER.name else endType.name,
                                repeatEndDate = endDate.toString().takeIf { repeatType != RepeatType.NONE && endType == RepeatEndType.UNTIL_DATE },
                                repeatCount = countText.toIntOrNull()?.coerceAtLeast(1)
                                    .takeIf { repeatType != RepeatType.NONE && endType == RepeatEndType.COUNT },
                                reminderDaysBefore = reminderDaysBefore,
                                onSaved = {
                                    normalizedTaskNames.forEach(tasksVm::addTask)
                                    TreatmentReminderScheduler.refreshNow(app)
                                    navController.popBackStack()
                                }
                            )
                        },
                        enabled = plantName.isNotBlank() && taskNames.isNotEmpty() && taskNames.all { it.isNotBlank() },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                    )
                }
            }
        }
    }

    if (addDrugDialogOpen) {
        AddDrugDialog(
            onDismiss = { addDrugDialogOpen = false },
            onSave = { name, purpose, rate ->
                pendingNewDrugName = name.trim()
                drugsVm.addDrug(name.trim(), purpose.trim(), rate.trim())
                addDrugDialogOpen = false
            }
        )
    }
}

@Composable
private fun <T> SelectionMenu(
    label: String,
    value: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
    addActionLabel: String? = null,
    onAddAction: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Text("⌄", color = Leaf300) },
            colors = glassTextFieldColors(),
            shape = CompactGlassShape,
            modifier = Modifier.fillMaxWidth()
        )
        Box(Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (options.isEmpty()) {
                DropdownMenuItem(text = { Text("Список пока пуст") }, onClick = { expanded = false })
            }
            options.forEach { option ->
                DropdownMenuItem(text = { Text(optionLabel(option)) }, onClick = { onSelected(option); expanded = false })
            }
            if (addActionLabel != null && onAddAction != null) {
                DropdownMenuItem(
                    text = { Text(addActionLabel, color = Leaf300) },
                    onClick = {
                        expanded = false
                        onAddAction()
                    }
                )
            }
        }
    }
}

@Composable
private fun AddDrugDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, purpose: String, rate: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Forest900,
        titleContentColor = Cream,
        textContentColor = Cream,
        title = { Text("Новый препарат") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("После сохранения препарат появится здесь и на вкладке «Препараты».", color = Mist)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    keyboardOptions = SentenceKeyboardOptions,
                    singleLine = true,
                    colors = glassTextFieldColors(),
                    shape = CompactGlassShape,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    label = { Text("Назначение") },
                    keyboardOptions = SentenceKeyboardOptions,
                    minLines = 2,
                    colors = glassTextFieldColors(),
                    shape = CompactGlassShape,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rate,
                    onValueChange = { rate = it },
                    label = { Text("Норма расхода") },
                    keyboardOptions = SentenceKeyboardOptions,
                    singleLine = true,
                    colors = glassTextFieldColors(),
                    shape = CompactGlassShape,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, purpose, rate) },
                enabled = name.isNotBlank() && purpose.isNotBlank() && rate.isNotBlank()
            ) {
                Text("Добавить", color = Leaf300)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = Mist)
            }
        }
    )
}
