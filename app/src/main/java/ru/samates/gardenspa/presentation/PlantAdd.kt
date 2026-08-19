package ru.samates.gardenspa.presentation

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.onFocusChanged
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
import ru.samates.gardenspa.domain.CareProgramContext
import ru.samates.gardenspa.domain.CareProgramGenerator
import ru.samates.gardenspa.domain.CultivationType
import ru.samates.gardenspa.domain.GeneratedCareProgram
import ru.samates.gardenspa.domain.FolkFertilizerRecipe
import ru.samates.gardenspa.domain.NO_DRUG_REQUIRED_LABEL
import ru.samates.gardenspa.domain.ReminderUnit
import ru.samates.gardenspa.domain.PlantCareCatalog
import ru.samates.gardenspa.domain.PlantNameCatalog
import ru.samates.gardenspa.domain.ProgramStartChoice
import ru.samates.gardenspa.domain.ProgramStartPlanner
import ru.samates.gardenspa.domain.ProgramStartProposal
import ru.samates.gardenspa.domain.ProcedureStep
import ru.samates.gardenspa.domain.ReadyProgramDrugCatalog
import ru.samates.gardenspa.domain.customReminderMinutes
import ru.samates.gardenspa.domain.decodeGardenClimate
import ru.samates.gardenspa.domain.decodeReminderOffsets
import ru.samates.gardenspa.domain.encodeReminderOffsets
import ru.samates.gardenspa.domain.procedureSteps
import ru.samates.gardenspa.domain.reminderOffsetLabel
import ru.samates.gardenspa.notifications.TreatmentReminderScheduler
import ru.samates.gardenspa.presentation.navigation.navigateToCalendar
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
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

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

private val reminderPresets = listOf(0, 10, 30, 60, 24 * 60, 7 * 24 * 60)

@Composable
fun PlantAdd(
    navController: NavController,
    selectedDate: String,
    plantId: Int? = null,
    preselectedGardenId: Int? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as BookeeperApp
    val plantsVm: PlantsViewmodel = viewModel(factory = PlantsViewmodelFactory(app.repository))
    val drugsVm: DrugsViewmodel = viewModel(factory = DrugsViewmodelFactory(app.repository))
    val gardensVm: GardensViewmodel = viewModel(factory = GardensViewmodelFactory(app.repository))
    val tasksVm: TasksViewmodel = viewModel(factory = TasksViewmodelFactory(app.repository))
    val drugs by drugsVm.drugs.collectAsState()
    val recipes by drugsVm.recipes.collectAsState()
    val gardens by gardensVm.gardens.collectAsState()
    val plants by plantsVm.plants.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val programGenerator = remember { CareProgramGenerator() }
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
    val editingProgram = editingRows.isNotEmpty() && editingRows.any { it.programId != null }

    var plantName by remember { mutableStateOf("") }
    var plantDetails by remember { mutableStateOf("") }
    var selectedPlantCanonicalName by remember { mutableStateOf<String?>(null) }
    var selectedPlantTemplateId by remember { mutableStateOf<String?>(null) }
    var plantSuggestionsExpanded by remember { mutableStateOf(false) }
    var taskNames by remember { mutableStateOf(listOf("")) }
    var selectedDrug by remember { mutableStateOf<DrugEntity?>(null) }
    var selectedGarden by remember { mutableStateOf<GardenEntity?>(null) }
    var repeatType by remember { mutableStateOf(RepeatType.NONE) }
    var intervalText by remember { mutableStateOf("1") }
    var weekDays by remember { mutableStateOf(setOf(startDate.dayOfWeek)) }
    var endType by remember { mutableStateOf(RepeatEndType.NEVER) }
    var endDate by remember { mutableStateOf(startDate.plusMonths(1)) }
    var countText by remember { mutableStateOf("10") }
    var reminderOffsets by remember { mutableStateOf(listOf(24 * 60)) }
    var reminderDialogIndex by remember { mutableStateOf<Int?>(null) }
    var fieldsInitialized by remember(plantId) { mutableStateOf(false) }
    var addGardenDialogOpen by remember { mutableStateOf(false) }
    var addDrugDialogOpen by remember { mutableStateOf(false) }
    var recipePickerOpen by remember { mutableStateOf(false) }
    var pendingNewGardenName by remember { mutableStateOf<String?>(null) }
    var pendingNewDrugName by remember { mutableStateOf<String?>(null) }
    var importedTaskDates by remember { mutableStateOf<List<LocalDate>>(emptyList()) }
    var cultivationType by remember { mutableStateOf(CultivationType.OPEN_GROUND) }
    var programStartDate by remember(startDate) { mutableStateOf(startDate) }
    var generatedProgram by remember { mutableStateOf<GeneratedCareProgram?>(null) }
    var selectedProgramDrugs by remember { mutableStateOf<Map<String, DrugEntity>>(emptyMap()) }
    var programLoading by remember { mutableStateOf(false) }
    var programImporting by remember { mutableStateOf(false) }
    var programError by remember { mutableStateOf<String?>(null) }
    var pendingStartProposal by remember { mutableStateOf<ProgramStartProposal?>(null) }
    var currentStep by remember(plantId) { mutableStateOf(ProcedureStep.PLANT) }
    var useReadyProgram by remember(plantId) { mutableStateOf(editingProgram) }
    val plantSuggestions = remember(plantName) { PlantNameCatalog.suggestions(plantName) }
    val matchedTemplate = remember(selectedPlantTemplateId) { selectedPlantTemplateId?.let(PlantCareCatalog::findById) }
    val flowSteps = procedureSteps(matchedTemplate != null, useReadyProgram)
    val currentStepIndex = flowSteps.indexOf(currentStep).coerceAtLeast(0)
    val selectedClimate = selectedGarden?.climateData?.decodeGardenClimate()
    val gardenLocation = selectedClimate?.location
    val climateFingerprint = selectedClimate?.fingerprint

    LaunchedEffect(editingPlant, editingRows, drugs, gardens, fieldsInitialized) {
        if (editingPlant != null && !fieldsInitialized) {
            plantName = editingPlant.plantName
            plantDetails = editingPlant.plantDetails
            val catalogPlant = PlantNameCatalog.findExact(editingPlant.plantName)
            selectedPlantCanonicalName = catalogPlant?.canonicalName ?: editingPlant.plantName
            selectedPlantTemplateId = editingPlant.programId ?: catalogPlant?.careTemplateId
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
            reminderOffsets = decodeReminderOffsets(editingPlant.reminderOffsetsMinutes, editingPlant.reminderDaysBefore)
            importedTaskDates = editingRows.map { row ->
                runCatching { LocalDate.parse(row.creationDate) }.getOrDefault(startDate)
            }
            fieldsInitialized = true
        }
    }

    LaunchedEffect(gardens, preselectedGardenId, editing) {
        if (!editing && selectedGarden == null && preselectedGardenId != null) {
            selectedGarden = gardens.firstOrNull { it.id == preselectedGardenId }
        }
    }

    LaunchedEffect(matchedTemplate?.id) {
        val supported = matchedTemplate?.supportedCultivationTypes.orEmpty()
        if (supported.isNotEmpty() && cultivationType !in supported) {
            cultivationType = supported.first()
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

    LaunchedEffect(gardens, pendingNewGardenName) {
        val newGardenName = pendingNewGardenName ?: return@LaunchedEffect
        gardens
            .filter { it.name.equals(newGardenName, ignoreCase = true) }
            .maxByOrNull { it.id }
            ?.let { newGarden ->
                selectedGarden = newGarden
                pendingNewGardenName = null
            }
    }

    BotanicalBackground {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(
                if (editing) "Редактирование процедуры" else "Новая процедура",
                if (editing) startDate.toString() else "Шаг ${currentStepIndex + 1} из ${flowSteps.size}",
                onBack = {
                    if (editing || currentStepIndex == 0) navController.popBackStack()
                    else currentStep = flowSteps[currentStepIndex - 1]
                }
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (editing && editingPlant == null) {
                    EmptyGlassState("Растение не найдено", "Возможно, оно было удалено")
                } else {
                    if (editing || currentStep == ProcedureStep.PLANT) GlassCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Карточка растения", color = Cream, style = MaterialTheme.typography.titleLarge)
                            Column(Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = plantName,
                                    onValueChange = { value ->
                                        plantName = value
                                        if (selectedPlantCanonicalName != value) {
                                            selectedPlantCanonicalName = null
                                            selectedPlantTemplateId = null
                                            useReadyProgram = false
                                        }
                                        plantSuggestionsExpanded = value.isNotBlank()
                                    },
                                    label = { Text("Вид растения") },
                                    placeholder = { Text("Начните вводить: помидор, роза…") },
                                    keyboardOptions = SentenceKeyboardOptions,
                                    singleLine = true,
                                    colors = glassTextFieldColors(),
                                    shape = CompactGlassShape,
                                    modifier = Modifier.fillMaxWidth().onFocusChanged { state ->
                                        if (state.isFocused && plantSuggestions.isNotEmpty()) plantSuggestionsExpanded = true
                                    }
                                )
                                if (plantSuggestionsExpanded && plantSuggestions.isNotEmpty()) {
                                    plantSuggestions.forEach { suggestion ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(suggestion.canonicalName)
                                                    Text(
                                                        if (suggestion.careTemplateId != null) "Есть готовая годовая программа" else "Каталог растений",
                                                        color = Mist,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            },
                                            onClick = {
                                                plantName = suggestion.canonicalName
                                                selectedPlantCanonicalName = suggestion.canonicalName
                                                selectedPlantTemplateId = suggestion.careTemplateId
                                                plantSuggestionsExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            if (selectedPlantCanonicalName != null) {
                                OutlinedTextField(
                                    value = plantDetails,
                                    onValueChange = { plantDetails = it },
                                    label = { Text("Сорт или комментарий") },
                                    placeholder = { Text("Например: Розовый мёд, куст у беседки") },
                                    keyboardOptions = SentenceKeyboardOptions,
                                    minLines = 2,
                                    maxLines = 4,
                                    colors = glassTextFieldColors(),
                                    shape = CompactGlassShape,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text("Поле необязательное — можно указать сорт или оставить свою заметку.", color = Mist, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if (editing || currentStep == ProcedureStep.GARDEN) {
                        GlassCard(Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Сад", color = Cream, style = MaterialTheme.typography.titleLarge)
                                SelectionMenu(
                                    label = "Выберите сад",
                                    value = selectedGarden?.name ?: "Не выбрано",
                                    options = gardens,
                                    optionLabel = { it.name },
                                    onSelected = { selectedGarden = it },
                                    addActionLabel = "+ Добавить сад",
                                    onAddAction = { addGardenDialogOpen = true }
                                )
                                if (useReadyProgram && selectedGarden != null && selectedClimate == null) {
                                    Text("Для готовой программы сначала настройте климат этого сада на карточке сада.", color = Danger)
                                }
                            }
                        }
                    }
                    if (!editing && currentStep == ProcedureStep.PROGRAM && matchedTemplate != null) {
                        GlassCard(Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Готовая программа", color = Cream, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "Для растения «${matchedTemplate.canonicalName}» доступна программа ухода.",
                                    color = Leaf300
                                )
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    matchedTemplate.supportedCultivationTypes.forEach { type ->
                                        FilterChip(
                                            selected = cultivationType == type,
                                            onClick = { cultivationType = type },
                                            label = { Text(type.displayName) },
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Leaf300, selectedLabelColor = Color(0xFF071D17), labelColor = Cream, containerColor = Forest700)
                                        )
                                    }
                                }
                                SecondaryAction(
                                    "Начать: ${programStartDate.format(programDateFormatter)}",
                                    onClick = {
                                        DatePickerDialog(context, { _, year, month, day -> programStartDate = LocalDate.of(year, month + 1, day) }, programStartDate.year, programStartDate.monthValue - 1, programStartDate.dayOfMonth).show()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text("Даты процедур будут рассчитаны после выбора сада по его климату.", color = Mist)
                            }
                        }
                    }
                    if (editing || currentStep == ProcedureStep.PROCEDURE) GlassCard(Modifier.fillMaxWidth()) {
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
                                        if (!editingProgram) {
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
                                if (editingProgram) {
                                    val taskDate = importedTaskDates.getOrNull(index) ?: startDate
                                    SecondaryAction(
                                        "Дата: $taskDate",
                                        onClick = {
                                            DatePickerDialog(
                                                context,
                                                { _, year, month, day ->
                                                    importedTaskDates = importedTaskDates.toMutableList().also {
                                                        it[index] = LocalDate.of(year, month + 1, day)
                                                    }
                                                },
                                                taskDate.year,
                                                taskDate.monthValue - 1,
                                                taskDate.dayOfMonth
                                            ).show()
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    editingRows.getOrNull(index)?.programNote
                                        ?.takeIf(String::isNotBlank)
                                        ?.let { Text(it, color = Mist) }
                                }
                            }
                            if (!editingProgram) {
                            SecondaryAction(
                                "+ Добавить процедуру",
                                onClick = { taskNames = taskNames + "" },
                                modifier = Modifier.fillMaxWidth()
                            )
                            }
                        }
                    }
                    if (!editingProgram && (editing || currentStep == ProcedureStep.DRUG)) {
                        GlassCard(Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Препарат", color = Cream, style = MaterialTheme.typography.titleLarge)
                                SelectionMenu(
                                    label = "Выберите препарат",
                                    value = selectedDrug?.name ?: "Не выбрано",
                                    options = drugs,
                                    optionLabel = { it.name },
                                    onSelected = { selectedDrug = it },
                                    addActionLabel = "+ Добавить новый препарат",
                                    onAddAction = { addDrugDialogOpen = true },
                                    extraActionLabel = "Выбрать из готовых рецептов",
                                    onExtraAction = { recipePickerOpen = true }
                                )
                            }
                        }
                    }
                    if (editing || currentStep == ProcedureStep.REMINDERS) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(if (useReadyProgram) "Напоминания" else "Повтор и напоминание", color = Cream, style = MaterialTheme.typography.titleLarge)
                            if (!useReadyProgram) {
                            SelectionMenu(
                                label = "Повтор",
                                value = repeatLabels.getValue(repeatType),
                                options = repeatLabels.keys.toList(),
                                optionLabel = { repeatLabels.getValue(it) },
                                onSelected = { repeatType = it }
                            )
                            }
                            Text("Уведомления", color = Mist)
                            if (reminderOffsets.isEmpty()) Text("Отключены", color = Mist)
                            reminderOffsets.forEachIndexed { index, minutes ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SecondaryAction(reminderOffsetLabel(minutes), { reminderDialogIndex = index }, Modifier.weight(1f))
                                    Text("×", color = Danger, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.clickable { reminderOffsets = reminderOffsets.toMutableList().also { it.removeAt(index) } })
                                }
                            }
                            SecondaryAction("+ Добавить уведомление", { reminderDialogIndex = reminderOffsets.size }, Modifier.fillMaxWidth())
                            Text("Время процедуры для уведомлений — 09:00", color = Mist, style = MaterialTheme.typography.bodySmall)
                            if (useReadyProgram && programLoading) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    CircularProgressIndicator(color = Leaf300)
                                    Text("Рассчитываем программу по климату сада…", color = Mist)
                                }
                            }
                            if (useReadyProgram) programError?.let { Text(it, color = Danger) }
                            if (!useReadyProgram && repeatType == RepeatType.CUSTOM) {
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
                            if (!useReadyProgram && repeatType == RepeatType.WEEKLY) {
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
                            if (!useReadyProgram && repeatType != RepeatType.NONE) {
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
                    }
                    if (!editing && currentStep == ProcedureStep.PROGRAM) {
                        Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SecondaryAction("Вручную", { useReadyProgram = false; currentStep = ProcedureStep.GARDEN }, Modifier.weight(1f))
                            PrimaryAction("Готовая программа", { useReadyProgram = true; currentStep = ProcedureStep.GARDEN }, Modifier.weight(1f))
                        }
                    } else PrimaryAction(
                        when {
                            editingProgram -> "Сохранить программу"
                            editing -> "Сохранить изменения"
                            currentStepIndex < flowSteps.lastIndex -> "Далее"
                            useReadyProgram -> "Добавить программу"
                            else -> "Сохранить процедуру"
                        },
                        onClick = {
                            if (!editing && currentStepIndex < flowSteps.lastIndex) {
                                currentStep = flowSteps[currentStepIndex + 1]
                                return@PrimaryAction
                            }
                            if (!editing && useReadyProgram) {
                                val template = matchedTemplate ?: return@PrimaryAction
                                val climate = climateFingerprint ?: return@PrimaryAction
                                pendingStartProposal = ProgramStartPlanner.propose(template, cultivationType, climate, programStartDate)
                                return@PrimaryAction
                            }
                            val interval = intervalText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                            val normalizedTaskNames = taskNames.map { it.trim() }
                            if (editingProgram) {
                                val reminderData = encodeReminderOffsets(reminderOffsets)
                                plantsVm.updateImportedProgramCard(
                                    plantName = plantName,
                                    plantDetails = plantDetails,
                                    existingRows = editingRows,
                                    taskNames = normalizedTaskNames,
                                    taskDates = importedTaskDates,
                                    gardenId = selectedGarden?.id,
                                    gardenName = selectedGarden?.name ?: "Не выбрано",
                                    reminderOffsetsMinutes = reminderData,
                                    onSaved = {
                                        TreatmentReminderScheduler.refreshNow(app)
                                        navController.popBackStack()
                                    }
                                )
                                return@PrimaryAction
                            }
                            val reminderData = encodeReminderOffsets(reminderOffsets)
                            plantsVm.savePlantCard(
                                plantId = plantId,
                                plantName = plantName.trim(),
                                plantDetails = plantDetails,
                                taskNames = normalizedTaskNames,
                                wateringInterval = interval,
                                creationDate = startDate.toString(),
                                drugId = selectedDrug?.id,
                                gardenId = selectedGarden?.id,
                                drugName = selectedDrug?.name ?: "Не выбрано",
                                gardenName = selectedGarden?.name ?: "Не выбрано",
                                repeatType = repeatType.name,
                                repeatInterval = interval,
                                repeatDaysOfWeek = weekDays.sortedBy { it.value }.joinToString(",") { it.value.toString() },
                                repeatEndType = if (repeatType == RepeatType.NONE) RepeatEndType.NEVER.name else endType.name,
                                repeatEndDate = endDate.toString().takeIf { repeatType != RepeatType.NONE && endType == RepeatEndType.UNTIL_DATE },
                                repeatCount = countText.toIntOrNull()?.coerceAtLeast(1)
                                    .takeIf { repeatType != RepeatType.NONE && endType == RepeatEndType.COUNT },
                                reminderDaysBefore = reminderOffsets.firstOrNull()?.div(24 * 60) ?: 0,
                                reminderOffsetsMinutes = reminderData,
                                onSaved = {
                                    normalizedTaskNames.forEach(tasksVm::addTask)
                                    TreatmentReminderScheduler.refreshNow(app)
                                    if (editing) navController.popBackStack() else navController.navigateToCalendar()
                                }
                            )
                        },
                        enabled = if (editing) {
                            plantName.isNotBlank() && taskNames.isNotEmpty() && taskNames.all { it.isNotBlank() } && (!editingProgram || importedTaskDates.size == taskNames.size) && pendingNewDrugName == null
                        } else when (currentStep) {
                            ProcedureStep.PLANT -> plantName.isNotBlank()
                            ProcedureStep.GARDEN -> selectedGarden != null && (!useReadyProgram || selectedClimate != null)
                            ProcedureStep.PROCEDURE -> taskNames.isNotEmpty() && taskNames.all { it.isNotBlank() }
                            ProcedureStep.DRUG -> pendingNewDrugName == null
                            ProcedureStep.REMINDERS -> !programLoading && (!useReadyProgram || selectedClimate != null)
                            ProcedureStep.PROGRAM -> true
                        },
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

    if (addGardenDialogOpen) {
        AddGardenDialog(
            onDismiss = { addGardenDialogOpen = false },
            onSave = { name ->
                pendingNewGardenName = name.trim()
                gardensVm.gardenAdd(name.trim())
                addGardenDialogOpen = false
            }
        )
    }

    if (recipePickerOpen) {
        RecipePickerDialog(
            recipes = recipes,
            addedDrugNames = drugs.map { it.name.lowercase() }.toSet(),
            onDismiss = { recipePickerOpen = false },
            onSelect = { recipe ->
                val existingDrug = drugs.firstOrNull { it.name.equals(recipe.name, ignoreCase = true) }
                if (existingDrug != null) {
                    selectedDrug = existingDrug
                } else {
                    pendingNewDrugName = recipe.name
                    drugsVm.addDrug(recipe.name, recipe.purposeForDrug(), recipe.consumptionRate)
                }
                recipePickerOpen = false
            }
        )
    }

    reminderDialogIndex?.let { index ->
        ReminderPickerDialog(
            currentMinutes = reminderOffsets.getOrNull(index),
            onDismiss = { reminderDialogIndex = null },
            onSave = { minutes ->
                reminderOffsets = reminderOffsets.toMutableList().apply {
                    if (index in indices) this[index] = minutes else add(minutes)
                }.distinct().sorted()
                reminderDialogIndex = null
            }
        )
    }

    fun calculateProgramForStart(scheduleStartDate: LocalDate, includeOnlyOnOrAfter: LocalDate? = null) {
        val template = matchedTemplate ?: return
        val location = gardenLocation ?: return
        val climate = climateFingerprint ?: return
        pendingStartProposal = null
        programStartDate = includeOnlyOnOrAfter ?: scheduleStartDate
        programLoading = true
        programError = null
        coroutineScope.launch {
            val forecast = runCatching { app.climateService.loadForecast(location) }.getOrDefault(emptyList())
            runCatching {
                programGenerator.generate(
                    template = template,
                    context = CareProgramContext(
                        startDate = scheduleStartDate,
                        cultivationType = cultivationType,
                        climate = climate,
                        forecast = forecast,
                        includeOnlyOnOrAfter = includeOnlyOnOrAfter
                    )
                )
            }.onSuccess {
                generatedProgram = it
                selectedProgramDrugs = emptyMap()
            }
                .onFailure { programError = it.message ?: "Не удалось составить программу" }
            programLoading = false
        }
    }

    generatedProgram?.let { program ->
        CareProgramPreviewDialog(
            program = program,
            drugs = drugs,
            selectedDrugs = selectedProgramDrugs,
            importing = programImporting,
            hasGarden = selectedGarden != null,
            onDrugSelected = { stepId, drug -> selectedProgramDrugs = selectedProgramDrugs + (stepId to drug) },
            onDismiss = {
                if (!programImporting) {
                    generatedProgram = null
                    currentStep = ProcedureStep.PROGRAM
                }
            },
            onNextYear = {
                generatedProgram = null
                calculateProgramForStart(program.recommendedStartDate.plusYears(1))
            },
            onImport = {
                programImporting = true
                val reminderData = encodeReminderOffsets(reminderOffsets)
                plantsVm.importCareProgram(
                    program = program,
                    selectedDrugs = selectedProgramDrugs,
                    plantDetails = plantDetails,
                    gardenId = selectedGarden?.id,
                    gardenName = selectedGarden?.name ?: "Не выбрано",
                    reminderDaysBefore = reminderOffsets.firstOrNull()?.div(24 * 60) ?: 0,
                    reminderOffsetsMinutes = reminderData,
                    onSaved = { taskTitles ->
                        taskTitles.forEach(tasksVm::addTask)
                        TreatmentReminderScheduler.refreshNow(app)
                        programImporting = false
                        generatedProgram = null
                        navController.navigateToCalendar()
                    },
                    onError = { message ->
                        programImporting = false
                        programError = message
                    }
                )
            }
        )
    }

    pendingStartProposal?.let { proposal ->
        ProgramStartChoiceDialog(
            proposal = proposal,
            onDismiss = { pendingStartProposal = null },
            onChoice = { choice ->
                if (choice == ProgramStartChoice.USER_DATE) {
                    calculateProgramForStart(proposal.recommendedDate, proposal.resolve(choice))
                } else {
                    calculateProgramForStart(proposal.resolve(choice))
                }
            }
        )
    }
}

private val programDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

@Composable
private fun ReminderPickerDialog(currentMinutes: Int?, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    val initialMinutes = currentMinutes?.takeIf { it > 0 } ?: 10
    val initialUnit = remember(initialMinutes) { ReminderUnit.entries.lastOrNull { initialMinutes % it.minutes == 0 } ?: ReminderUnit.MINUTES }
    var customMode by remember(currentMinutes) { mutableStateOf(currentMinutes != null && currentMinutes !in reminderPresets) }
    var customValue by remember(currentMinutes) { mutableStateOf((initialMinutes / initialUnit.minutes).toString()) }
    var customUnit by remember(currentMinutes) { mutableStateOf(initialUnit) }
    val customMinutes = customValue.toIntOrNull()?.let { customReminderMinutes(it, customUnit) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Forest900,
        titleContentColor = Cream,
        textContentColor = Cream,
        title = { Text(if (customMode) "Своё уведомление" else "Когда напомнить") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (customMode) {
                    OutlinedTextField(
                        value = customValue,
                        onValueChange = { customValue = it.filter(Char::isDigit).take(7) },
                        label = { Text("За сколько") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = customValue.isNotBlank() && customMinutes == null,
                        singleLine = true,
                        colors = glassTextFieldColors(),
                        shape = CompactGlassShape,
                        modifier = Modifier.fillMaxWidth()
                    )
                    SelectionMenu("Единица времени", customUnit.title, ReminderUnit.entries, ReminderUnit::title, { customUnit = it })
                    Text("Можно настроить от 1 минуты до 1 года до процедуры.", color = Mist, style = MaterialTheme.typography.bodySmall)
                } else {
                    reminderPresets.forEach { minutes -> SecondaryAction(reminderOffsetLabel(minutes), { onSave(minutes) }, Modifier.fillMaxWidth()) }
                    SecondaryAction("Свой вариант…", { customMode = true }, Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            if (customMode) TextButton(onClick = { customMinutes?.let(onSave) }, enabled = customMinutes != null) { Text("Готово", color = Leaf300) }
        },
        dismissButton = {
            TextButton(onClick = { if (customMode) customMode = false else onDismiss() }) { Text(if (customMode) "Назад" else "Отмена", color = Mist) }
        }
    )
}

@Composable
private fun ProgramStartChoiceDialog(
    proposal: ProgramStartProposal,
    onDismiss: () -> Unit,
    onChoice: (ProgramStartChoice) -> Unit
) {
    val recommendedText = proposal.recommendedDate.format(programDateFormatter)
    val userStartText = proposal.resolve(ProgramStartChoice.USER_DATE).format(programDateFormatter)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Forest900,
        titleContentColor = Cream,
        textContentColor = Cream,
        title = { Text("Когда начать программу?") },
        text = {
            Text(
                if (proposal.recommendationHasPassed) {
                    "Рекомендуемая дата начала работ для вашего региона ($recommendedText) уже прошла. " +
                        "Перенести программу на следующий год или добавить только оставшиеся работы с выбранной даты?"
                } else {
                    "Запланировать полный сезон с рекомендуемой даты — $recommendedText? При выборе своей даты прошлые процедуры добавлены не будут."
                },
                color = Mist
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onChoice(
                        if (proposal.recommendationHasPassed) ProgramStartChoice.NEXT_YEAR
                        else ProgramStartChoice.RECOMMENDED_DATE
                    )
                }
            ) {
                Text(
                    if (proposal.recommendationHasPassed) "Начать в следующем году" else "Да, на $recommendedText",
                    color = Leaf300
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { onChoice(ProgramStartChoice.USER_DATE) }) {
                Text("Только оставшиеся с $userStartText", color = Mist)
            }
        }
    )
}

@Composable
private fun CareProgramPreviewDialog(
    program: GeneratedCareProgram,
    drugs: List<DrugEntity>,
    selectedDrugs: Map<String, DrugEntity>,
    importing: Boolean,
    hasGarden: Boolean,
    onDrugSelected: (String, DrugEntity) -> Unit,
    onDismiss: () -> Unit,
    onNextYear: () -> Unit,
    onImport: () -> Unit
) {
    val missingDrugCount = program.steps.count { it.productDescription != null && it.templateStepId !in selectedDrugs }
    val canImport = hasGarden && missingDrugCount == 0
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Forest900,
        titleContentColor = Cream,
        textContentColor = Cream,
        title = { Text("Программа для ${program.plantName}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("${program.cultivationType.displayName} · ${program.naturalZone.displayName} · ${program.climateSummary}", color = Leaf300)
                Text(
                    "Рекомендуемое начало работ: ${program.recommendedStartDate.format(programDateFormatter)}",
                    color = Mist
                )
                Text("Завершение сезона: ${program.recommendedEndDate.format(programDateFormatter)}", color = Mist)
                program.remainingFromDate?.let { date ->
                    Text("Показаны только процедуры, оставшиеся с ${date.format(programDateFormatter)}.", color = Leaf300)
                }
                program.warning?.let { Text(it, color = Danger) }
                if (program.steps.isEmpty()) {
                    Text("Процедур на этот год не осталось. Можно добавить процедуры на следующий год или добавить их вручную.", color = Mist)
                } else if (!hasGarden) {
                    Text("Перед добавлением в календарь выберите или добавьте сад.", color = Danger)
                } else if (missingDrugCount > 0) {
                    Text("Выберите препарат для каждого этапа: осталось $missingDrugCount.", color = Danger)
                }
                program.steps.forEach { step ->
                    val selectedDrug = selectedDrugs[step.templateStepId]
                    val recommendedDrugs = remember(program.templateId, step.templateStepId, drugs) {
                        ReadyProgramDrugCatalog.recommendedFor(program.templateId, step.templateStepId, drugs)
                    }
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(step.title, color = Cream, style = MaterialTheme.typography.titleMedium)
                            Text(step.scheduledDate.toString(), color = Leaf300)
                            val recurrence = step.recurrence
                            val intervalUnit = when (recurrence?.type) {
                                RepeatType.WEEKLY -> "нед."
                                RepeatType.MONTHLY -> "мес."
                                RepeatType.YEARLY -> "г."
                                else -> "дн."
                            }
                            Text(if (recurrence == null) "Кратность: однократно" else "Кратность: ${recurrence.count}; интервал: ${recurrence.interval} $intervalUnit", color = Mist)
                            step.productDescription?.let { description ->
                                Text("Какое средство потребуется", color = Leaf300)
                                Text(description, color = Cream)
                                SelectionMenu(
                                    label = "Препарат для этапа",
                                    value = selectedDrug?.name ?: "Выберите препарат",
                                    options = recommendedDrugs,
                                    optionLabel = DrugEntity::name,
                                    onSelected = { onDrugSelected(step.templateStepId, it) }
                                )
                                if (recommendedDrugs.isEmpty()) Text("В справочнике пока нет подходящих препаратов для этого этапа.", color = Danger)
                                selectedDrug?.let { drug ->
                                    Text("Назначение: ${drug.purpose.ifBlank { "Не указано" }}", color = Cream)
                                    Text("Норма применения: ${drug.consumptionRate.ifBlank { "Не указана" }}", color = Mist)
                                }
                            } ?: OutlinedTextField(
                                value = NO_DRUG_REQUIRED_LABEL,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Препарат") },
                                colors = glassTextFieldColors(),
                                shape = CompactGlassShape,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(step.explanation, color = Mist)
                            if (step.needsWeatherConfirmation) {
                                Text("Проверьте погоду перед выполнением", color = Danger)
                            }
                        }
                    }
                }
                Text(
                    "После добавления каждую процедуру можно переименовать или перенести вручную.",
                    color = Mist
                )
                Text(
                    "Для каждого этапа показаны подходящие варианты разных производителей. Перед применением сверяйтесь с актуальной инструкцией на упаковке.",
                    color = Mist
                )
                if (importing) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(color = Leaf300)
                        Text("Добавляем в календарь…", color = Mist)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = if (program.steps.isEmpty()) onNextYear else onImport,
                enabled = !importing && (program.steps.isEmpty() || canImport)
            ) {
                Text(if (program.steps.isEmpty()) "Добавить на следующий год" else "Добавить в календарь", color = Leaf300)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !importing) {
                Text(
                    when {
                        program.steps.isEmpty() -> "Добавить вручную"
                        !hasGarden -> "Вернуться к настройке"
                        else -> "Отказаться от программы"
                    },
                    color = Mist
                )
            }
        }
    )
}

@Composable
private fun <T> SelectionMenu(
    label: String,
    value: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
    addActionLabel: String? = null,
    onAddAction: (() -> Unit)? = null,
    extraActionLabel: String? = null,
    onExtraAction: (() -> Unit)? = null
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
            if (extraActionLabel != null && onExtraAction != null) {
                DropdownMenuItem(
                    text = { Text(extraActionLabel, color = Leaf300) },
                    onClick = {
                        expanded = false
                        onExtraAction()
                    }
                )
            }
        }
    }
}

@Composable
private fun RecipePickerDialog(
    recipes: List<FolkFertilizerRecipe>,
    addedDrugNames: Set<String>,
    onDismiss: () -> Unit,
    onSelect: (FolkFertilizerRecipe) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Forest900,
        titleContentColor = Cream,
        textContentColor = Cream,
        title = { Text("Готовые рецепты") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(recipes, key = FolkFertilizerRecipe::id) { recipe ->
                    GlassCard(Modifier.fillMaxWidth(), onClick = { onSelect(recipe) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(recipe.name, color = Cream, style = MaterialTheme.typography.titleMedium)
                            Text(recipe.purpose, color = Mist, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                            Text(if (recipe.name.lowercase() in addedDrugNames) "Уже добавлен — выбрать" else "Добавить и выбрать", color = Leaf300)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = Mist)
            }
        }
    )
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
                Text("После сохранения препарат появится здесь и в «Справочнике».", color = Mist)
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
                    label = { Text("Назначение (необязательно)") },
                    keyboardOptions = SentenceKeyboardOptions,
                    minLines = 2,
                    colors = glassTextFieldColors(),
                    shape = CompactGlassShape,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rate,
                    onValueChange = { rate = it },
                    label = { Text("Норма расхода (необязательно)") },
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
                enabled = name.isNotBlank()
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

@Composable
private fun AddGardenDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Forest900,
        titleContentColor = Cream,
        textContentColor = Cream,
        title = { Text("Новый сад") },
        text = {
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
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) {
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
