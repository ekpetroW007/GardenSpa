package ru.samates.gardenspa.presentation

import android.app.DatePickerDialog
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ru.samates.gardenspa.BookeeperApp
import ru.samates.gardenspa.data.database.entity.DrugEntity
import ru.samates.gardenspa.data.database.entity.GardenEntity
import ru.samates.gardenspa.data.database.entity.resolvedCardId
import ru.samates.gardenspa.data.database.entity.climateOrNull
import ru.samates.gardenspa.data.database.entity.locationOrNull
import ru.samates.gardenspa.domain.RepeatEndType
import ru.samates.gardenspa.domain.PlantTaskDraft
import ru.samates.gardenspa.domain.RepeatType
import ru.samates.gardenspa.domain.CareProgramContext
import ru.samates.gardenspa.domain.CareProgramGenerator
import ru.samates.gardenspa.domain.CultivationType
import ru.samates.gardenspa.domain.GeneratedCareProgram
import ru.samates.gardenspa.domain.NO_REMAINING_CARE_MESSAGE
import ru.samates.gardenspa.domain.PlantCareCatalog
import ru.samates.gardenspa.domain.PlantNameCatalog
import ru.samates.gardenspa.domain.ProgramStartChoice
import ru.samates.gardenspa.domain.ProgramStartPlanner
import ru.samates.gardenspa.domain.ProgramStartProposal
import ru.samates.gardenspa.domain.ProgramProductCatalog
import ru.samates.gardenspa.domain.withProduct
import ru.samates.gardenspa.domain.withProgramProduct
import ru.samates.gardenspa.domain.problemTreatment
import ru.samates.gardenspa.domain.toDrugDisplayName
import ru.samates.gardenspa.domain.ProgramReferenceCatalog
import ru.samates.gardenspa.domain.asPersonalProduct
import ru.samates.gardenspa.domain.withPersonalProduct
import ru.samates.gardenspa.domain.careNote
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.viewmodel.ProceduresViewmodel
import ru.samates.gardenspa.viewmodel.ProceduresViewmodelFactory
import ru.samates.gardenspa.domain.withProblemTreatment
import ru.samates.gardenspa.notifications.TreatmentReminderScheduler
import ru.samates.gardenspa.presentation.navigation.AppDestinations
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
import ru.samates.gardenspa.viewmodel.UserViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlantAdd(
    navController: NavController,
    selectedDate: String,
    plantId: Int? = null,
    preselectedGardenId: Int? = null,
    userViewModel: UserViewModel
) {
    val context = LocalContext.current
    val app = context.applicationContext as BookeeperApp
    val plantsVm: PlantsViewmodel = viewModel(factory = PlantsViewmodelFactory(app.repository))
    val drugsVm: DrugsViewmodel = viewModel(factory = DrugsViewmodelFactory(app.repository))
    val gardensVm: GardensViewmodel = viewModel(factory = GardensViewmodelFactory(app.repository))
    val tasksVm: TasksViewmodel = viewModel(factory = TasksViewmodelFactory(app.repository))
    val drugs by drugsVm.drugs.collectAsState()
    val gardens by gardensVm.gardens.collectAsState()
    val plants by plantsVm.plants.collectAsState()
    val proceduresVm: ProceduresViewmodel = viewModel(factory = ProceduresViewmodelFactory(app.repository))
    val procedureHistory by proceduresVm.procedures.collectAsState()
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
    var taskDrafts by remember(plantId, requestedDate) { mutableStateOf(listOf(PlantTaskDraft("", startDate))) }
    var taskDatePickerIndex by remember { mutableStateOf<Int?>(null) }
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
    var programRowsDraft by remember(plantId) { mutableStateOf<List<PlantEntity>>(emptyList()) }
    var editProductIndex by remember { mutableStateOf<Int?>(null) }
    var editAfterInspection by remember { mutableStateOf(false) }
    var programSaving by remember { mutableStateOf(false) }
    var addDrugDialogOpen by remember { mutableStateOf(false) }
    var newDrugForRow by remember { mutableStateOf<Int?>(null) }
    var drugSaving by remember { mutableStateOf(false) }
    var drugError by remember { mutableStateOf<String?>(null) }
    val availableDrugs = remember(drugs) {
        (drugs + ProgramReferenceCatalog.allEntries().map { it.asPersonalProduct() }).distinctBy { it.name to it.consumptionRate }
    }
    var cultivationType by remember { mutableStateOf(CultivationType.OPEN_GROUND) }
    var programStartDate by remember(startDate) { mutableStateOf(startDate) }
    var generatedProgram by remember { mutableStateOf<GeneratedCareProgram?>(null) }
    var programLoading by remember { mutableStateOf(false) }
    var programImporting by remember { mutableStateOf(false) }
    var programError by remember { mutableStateOf<String?>(null) }
    var pendingStartProposal by remember { mutableStateOf<ProgramStartProposal?>(null) }
    var unavailableContinuationNextYear by remember { mutableStateOf<LocalDate?>(null) }
    var manualSetupOpen by remember(plantId) { mutableStateOf(editing) }
    var manualScrollRequest by remember(plantId) { mutableIntStateOf(0) }
    val manualSetupRequester = remember { BringIntoViewRequester() }
    var photoUri by rememberSaveable(plantId) { mutableStateOf<String?>(null) }
    var photoLoading by remember { mutableStateOf(false) }
    var photoError by remember { mutableStateOf<String?>(null) }
    var plantSuggestionsExpanded by remember { mutableStateOf(false) }
    val plantNameSuggestions = remember(plantName) { PlantNameCatalog.namesStartingWith(plantName) }
    val matchedTemplate = remember(plantName) { PlantCareCatalog.find(plantName) }
    val selectedLocation = selectedGarden?.locationOrNull()
    val selectedClimate = selectedGarden?.climateOrNull()
    val invalidRepeatEnd = !editingProgram && repeatType != RepeatType.NONE &&
        endType == RepeatEndType.UNTIL_DATE && taskDrafts.any { it.startDate.isAfter(endDate) }
    val invalidRepeatCount = !editingProgram && repeatType != RepeatType.NONE &&
        endType == RepeatEndType.COUNT && (countText.toIntOrNull() ?: 0) < 1

    fun openManualSetup() {
        manualSetupOpen = true
        manualScrollRequest++
    }

    LaunchedEffect(manualScrollRequest) {
        if (manualScrollRequest > 0) {
            // Wait until the newly revealed fields have been laid out.
            withFrameNanos { }
            manualSetupRequester.bringIntoView()
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            photoLoading = true
            photoError = null
            coroutineScope.launch {
                try {
                    photoUri = storePlantPhoto(context, uri)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    photoError = "Не удалось сохранить фото. Выберите другое изображение или проверьте свободное место."
                } finally {
                    photoLoading = false
                }
            }
        }
    }
    val voiceInput = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { plantName = it }
        }
    }

    LaunchedEffect(editingPlant, editingRows, drugs, gardens, fieldsInitialized) {
        if (editingPlant != null && !fieldsInitialized) {
            programRowsDraft = editingRows
            plantName = editingPlant.plantName
            taskDrafts = editingRows.map { row ->
                PlantTaskDraft(
                    name = row.taskName,
                    startDate = runCatching { LocalDate.parse(row.creationDate) }.getOrDefault(startDate),
                    id = row.id
                )
            }
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
            if (photoUri == null) photoUri = editingPlant.photoUri
            fieldsInitialized = true
        }
    }

    LaunchedEffect(gardens, preselectedGardenId, editing, editingPlant?.id) {
        if (selectedGarden == null) {
            selectedGarden = if (editing) gardens.firstOrNull { it.id == editingPlant?.gardenId }
                else gardens.firstOrNull { it.id == preselectedGardenId } ?: gardens.singleOrNull()
        }
    }

    LaunchedEffect(drugs, editingPlant?.id) {
        if (editing && selectedDrug == null) selectedDrug = drugs.firstOrNull { it.id == editingPlant?.drugId }
    }

    LaunchedEffect(matchedTemplate?.id) {
        val supported = matchedTemplate?.supportedCultivationTypes.orEmpty()
        if (supported.isNotEmpty() && cultivationType !in supported) {
            cultivationType = supported.first()
        }
    }

    fun calculateProgramForStart(
        effectiveStartDate: LocalDate,
        continuationNextYearDate: LocalDate? = null
    ) {
        val template = matchedTemplate ?: return
        val location = selectedLocation ?: return
        val climate = selectedClimate ?: return
        pendingStartProposal = null
        unavailableContinuationNextYear = null
        programStartDate = effectiveStartDate
        programLoading = true
        programError = null
        coroutineScope.launch {
            val forecast = runCatching {
                app.climateService.loadForecast(location)
            }.getOrDefault(emptyList())
            runCatching {
                programGenerator.generate(
                    template = template,
                    context = CareProgramContext(
                        startDate = effectiveStartDate,
                        cultivationType = cultivationType,
                        climate = climate,
                        forecast = forecast
                    )
                )
            }.onSuccess {
                unavailableContinuationNextYear = null
                generatedProgram = it
            }.onFailure {
                val message = it.message ?: "Не удалось составить программу"
                if (message == NO_REMAINING_CARE_MESSAGE && continuationNextYearDate != null) {
                    unavailableContinuationNextYear = continuationNextYearDate
                    programError = null
                } else {
                    unavailableContinuationNextYear = null
                    programError = message
                }
            }
            programLoading = false
        }
    }

    BotanicalBackground {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(
                if (editing) "Изменить растение" else "Добавить растение",
                if (editing) "Карточка и работы по уходу" else "Выберите растение, сад и способ ухода",
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
                            Text("1. Что вы выращиваете?", color = Cream, style = MaterialTheme.typography.titleLarge)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                OutlinedTextField(
                                    plantName,
                                    {
                                        plantName = it
                                        plantSuggestionsExpanded = it.isNotBlank()
                                    },
                                    label = { Text("Например, томат или яблоня") },
                                    keyboardOptions = SentenceKeyboardOptions,
                                    singleLine = true,
                                    colors = glassTextFieldColors(),
                                    shape = CompactGlassShape,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (plantSuggestionsExpanded && plantNameSuggestions.isNotEmpty()) {
                                    Surface(
                                        color = Forest900,
                                        shape = CompactGlassShape,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 240.dp)
                                                .semantics { contentDescription = "Подсказки растений" }
                                        ) {
                                            items(plantNameSuggestions, key = { it }) { suggestion ->
                                                Text(
                                                    text = suggestion,
                                                    color = Cream,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            plantName = suggestion
                                                            plantSuggestionsExpanded = false
                                                        }
                                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (photoUri != null) {
                                PlantPhoto(photoUri, plantName.ifBlank { "растение" }, Modifier.fillMaxWidth().height(170.dp))
                            }
                            if (photoLoading) Text("Сохраняем фото…", color = Mist)
                            photoError?.let { Text(it, color = Danger) }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SecondaryAction(
                                    if (photoUri == null) "Добавить фото" else "Сменить фото",
                                    { if (!photoLoading) photoPicker.launch(arrayOf("image/*")) },
                                    Modifier.weight(1f)
                                )
                                SecondaryAction(
                                    "Продиктовать",
                                    {
                                        voiceInput.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Назовите растение")
                                        })
                                    },
                                    Modifier.weight(1f)
                                )
                            }
                            if (!editing) {
                                Text("Популярные растения", color = Mist)
                                listOf("Томат", "Огурец", "Яблоня", "Груша", "Малина", "Смородина", "Газон", "Роза").chunked(2).forEach { names ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        names.forEach { name ->
                                            SecondaryAction(
                                                name,
                                                {
                                                    plantName = name
                                                    plantSuggestionsExpanded = false
                                                },
                                                Modifier.weight(1f)
                                            )
                                        }
                                        if (names.size == 1) Box(Modifier.weight(1f))
                                    }
                                }
                            }
                            Text("2. В каком саду?", color = Cream, style = MaterialTheme.typography.titleLarge)
                            SelectionMenu(
                                label = "Сад",
                                value = selectedGarden?.name ?: "Выберите сад",
                                options = gardens,
                                optionLabel = { it.name },
                                onSelected = { selectedGarden = it },
                                addActionLabel = "+ Создать новый сад",
                                onAddAction = { navController.navigate(AppDestinations.GARDEN_ADD) }
                            )
                        }
                    }
                    if (!editing && matchedTemplate != null) {
                        GlassCard(Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("3. Готовая программа ухода", color = Cream, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "Для растения «${matchedTemplate.canonicalName}» доступна программа ухода.",
                                    color = Leaf300
                                )
                                if (selectedGarden == null) {
                                    Text("Сначала выберите сад.", color = Mist)
                                } else if (selectedLocation == null || selectedClimate == null) {
                                    Text(
                                        "Для этого сада пока не указано место. Укажите его, чтобы подобрать сроки ухода.",
                                        color = Mist
                                    )
                                    SecondaryAction(
                                        "Указать место сада",
                                        onClick = { selectedGarden?.let { navController.navigate(AppDestinations.gardenLocation(it.id)) } },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    Text(
                                        "${selectedLocation?.localityName} · ${selectedClimate?.displayName()}",
                                        color = Mist
                                    )
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        matchedTemplate.supportedCultivationTypes.forEach { type ->
                                            FilterChip(
                                                selected = cultivationType == type,
                                                onClick = { cultivationType = type },
                                                label = { Text(type.displayName) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Leaf300,
                                                    selectedLabelColor = Color(0xFF071D17),
                                                    labelColor = Cream,
                                                    containerColor = Forest700
                                                )
                                            )
                                        }
                                    }
                                    SecondaryAction(
                                        "Желаемая дата: ${programStartDate.toRussianDate()}",
                                        onClick = {
                                            DatePickerDialog(
                                                context,
                                                { _, year, month, day -> programStartDate = LocalDate.of(year, month + 1, day) },
                                                programStartDate.year,
                                                programStartDate.monthValue - 1,
                                                programStartDate.dayOfMonth
                                            ).show()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !programLoading
                                    )
                                    if (programLoading) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            CircularProgressIndicator(color = Leaf300)
                                            Text("Уточняем даты по климату и прогнозу…", color = Mist)
                                        }
                                    }
                                    programError?.let { Text(it, color = Danger) }
                                    PrimaryAction(
                                        "Показать программу ухода",
                                        onClick = {
                                            val climate = selectedClimate ?: return@PrimaryAction
                                            pendingStartProposal = ProgramStartPlanner.propose(
                                                template = matchedTemplate,
                                                cultivationType = cultivationType,
                                                climate = climate,
                                                selectedDate = programStartDate
                                            )
                                        },
                                        enabled = !programLoading,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    SecondaryAction(
                                        "Настроить уход самостоятельно",
                                        onClick = ::openManualSetup,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                    if (!editing && matchedTemplate == null && plantName.isNotBlank()) {
                        GlassCard(Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Готовой программы пока нет", color = Cream, style = MaterialTheme.typography.titleLarge)
                                Text("Вы можете самостоятельно добавить нужные работы по уходу.", color = Mist)
                                PrimaryAction("Настроить уход самостоятельно", ::openManualSetup, Modifier.fillMaxWidth())
                            }
                        }
                    }
                    if (editing || manualSetupOpen) GlassCard(
                        Modifier.fillMaxWidth().bringIntoViewRequester(manualSetupRequester)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Работы по уходу", color = Cream, style = MaterialTheme.typography.titleLarge)
                            taskDrafts.forEachIndexed { index, task ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = task.name,
                                        onValueChange = { value ->
                                            taskDrafts = taskDrafts.toMutableList().also { it[index] = task.copy(name = value) }
                                        },
                                        label = { Text("Работа ${index + 1}") },
                                        keyboardOptions = SentenceKeyboardOptions,
                                        singleLine = true,
                                        colors = glassTextFieldColors(),
                                        shape = CompactGlassShape,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (taskDrafts.size > 1) {
                                        if (!editingProgram) {
                                        TextButton(
                                            onClick = {
                                                taskDrafts = taskDrafts.filterIndexed { itemIndex, _ -> itemIndex != index }
                                            }
                                        ) { Text("Удалить", color = Danger) }
                                        }
                                    }
                                }
                                SecondaryAction(
                                    "Начало: ${task.startDate.toRussianDate()}",
                                    onClick = { taskDatePickerIndex = index },
                                    modifier = Modifier.fillMaxWidth().semantics {
                                        contentDescription = "Дата начала работы ${index + 1}"
                                    }
                                )
                                if (editingProgram) {
                                    programRowsDraft.getOrNull(index)?.let { row ->
                                        Text(row.drugName.toDrugDisplayName(), color = Leaf300)
                                        if (row.programNote.isNotBlank()) ExpandableInfo("Инструкция и ограничения", row.programNote)
                                        val hasHistory = row.id > 0 && procedureHistory.any { it.plantId == row.id }
                                        if (hasHistory) {
                                            Text("У работы есть история. Другую обработку добавьте отдельно после осмотра.", color = Mist)
                                        } else {
                                            if (ProgramProductCatalog.alternatives(row.programId, row.programStepId).isNotEmpty()) {
                                                SecondaryAction("Добавить или заменить препарат", { editProductIndex = index }, Modifier.fillMaxWidth())
                                            }
                                            SelectionMenu(
                                                label = "Мой препарат", value = "Выбрать из моих средств", options = drugs,
                                                optionLabel = { it.name }, onSelected = { drug ->
                                                    programRowsDraft = programRowsDraft.toMutableList().also { rows ->
                                                        rows[index] = row.withPersonalProduct(drug, taskDrafts[index].startDate, row.reminderDaysBefore)
                                                    }
                                                    taskDrafts = taskDrafts.toMutableList().also { tasks ->
                                                        tasks[index] = tasks[index].copy(name = programRowsDraft[index].taskName)
                                                    }
                                                }, addActionLabel = "+ Добавить новый препарат",
                                                onAddAction = { newDrugForRow = index; addDrugDialogOpen = true })
                                        }
                                    }
                                }
                            }
                            if (editingProgram && ProgramProductCatalog.supports(editingPlant?.programId)) {
                                SecondaryAction("После осмотра обнаружена проблема", { editAfterInspection = true }, Modifier.fillMaxWidth())
                            }
                            if (!editingProgram) {
                            SecondaryAction(
                                "+ Добавить ещё одну работу",
                                onClick = { taskDrafts = taskDrafts + PlantTaskDraft("", startDate) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            }
                            if (!editingProgram) {
                            SelectionMenu(
                                label = "Средство",
                                value = selectedDrug?.name ?: "Препарат не требуется",
                                options = availableDrugs,
                                optionLabel = { it.name },
                                onSelected = { drug ->
                                    if (drug.id > 0) selectedDrug = drug
                                    else {
                                        drugSaving = true; drugError = null
                                        drugsVm.addDrug(drug.name, drug.purpose, drug.consumptionRate, drug.applicationMethod,
                                            onSaved = { selectedDrug = it; drugSaving = false },
                                            onError = { drugError = it; drugSaving = false })
                                    }
                                },
                                addActionLabel = "+ Добавить новый препарат",
                                onAddAction = { newDrugForRow = null; addDrugDialogOpen = true }
                            )
                            if (drugSaving) Text("Сохраняем средство…", color = Mist)
                            drugError?.let { Text(it, color = Danger) }
                            }
                        }
                    }
                    if (editing || manualSetupOpen) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (!editingProgram) {
                            SelectionMenu(
                                label = "Повтор",
                                value = repeatLabels.getValue(repeatType),
                                options = repeatLabels.keys.toList(),
                                optionLabel = { repeatLabels.getValue(it) },
                                onSelected = {
                                    repeatType = it
                                    // A custom interval in days must not silently become an interval in weeks/months.
                                    if (it != RepeatType.CUSTOM) intervalText = "1"
                                }
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
                                Text("Дни повторения процедуры", color = Mist)
                                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                    label = "Окончание работ",
                                    value = endLabels.getValue(endType),
                                    options = endLabels.keys.toList(),
                                    optionLabel = { endLabels.getValue(it) },
                                    onSelected = { endType = it }
                                )
                                when (endType) {
                                    RepeatEndType.UNTIL_DATE -> SecondaryAction("До ${endDate.toRussianDate()}", {
                                        DatePickerDialog(context, { _, y, m, d -> endDate = LocalDate.of(y, m + 1, d) }, endDate.year, endDate.monthValue - 1, endDate.dayOfMonth).apply {
                                            datePicker.minDate = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                        }.show()
                                    }, Modifier.fillMaxWidth())
                                    RepeatEndType.COUNT -> {
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            TextButton(
                                                onClick = { countText = ((countText.toIntOrNull() ?: 1) - 1).coerceAtLeast(1).toString() },
                                                enabled = (countText.toIntOrNull() ?: 0) > 1,
                                                modifier = Modifier.semantics { contentDescription = "Уменьшить количество повторов" }
                                            ) { Text("−", color = Leaf300) }
                                            OutlinedTextField(
                                                countText, { countText = it.filter(Char::isDigit).take(4) },
                                                label = { Text("Количество повторов") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true, isError = invalidRepeatCount,
                                                colors = glassTextFieldColors(), shape = CompactGlassShape,
                                                modifier = Modifier.weight(1f)
                                            )
                                            TextButton(
                                                onClick = { countText = ((countText.toIntOrNull() ?: 0) + 1).coerceIn(1, 9999).toString() },
                                                enabled = (countText.toIntOrNull() ?: 0) < 9999,
                                                modifier = Modifier.semantics { contentDescription = "Увеличить количество повторов" }
                                            ) { Text("+", color = Leaf300) }
                                        }
                                        Text("Всего выполнений, включая первое", color = Mist)
                                        SecondaryAction("Убрать ограничение повторов", { endType = RepeatEndType.NEVER }, Modifier.fillMaxWidth())
                                    }
                                    RepeatEndType.NEVER -> Unit
                                }
                            }
                            }
                            SelectionMenu(
                                label = "Напоминание",
                                value = reminderLabels.getValue(reminderDaysBefore),
                                options = reminderLabels.keys.toList(),
                                optionLabel = { reminderLabels.getValue(it) },
                                onSelected = { reminderDaysBefore = it }
                            )
                        }
                    }
                    }
                    if ((editing || manualSetupOpen) && invalidRepeatEnd) {
                        Text("Дата окончания повторов не может быть раньше даты начала любой из работ.", color = Danger)
                    }
                    if (editing || manualSetupOpen) PrimaryAction(
                        if (editingProgram) "Сохранить программу" else if (editing) "Сохранить изменения" else "Сохранить растение",
                        onClick = {
                            val interval = if (repeatType == RepeatType.CUSTOM) {
                                intervalText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                            } else 1
                            val normalizedTaskNames = taskDrafts.map { it.name.trim() }
                            if (editingProgram) {
                                programSaving = true
                                programError = null
                                plantsVm.updateImportedProgramCard(
                                    plantName = plantName,
                                    existingRows = programRowsDraft,
                                    taskNames = normalizedTaskNames,
                                    taskDates = taskDrafts.map { it.startDate },
                                    gardenId = selectedGarden?.id,
                                    gardenName = selectedGarden?.name.orEmpty(),
                                    reminderDaysBefore = reminderDaysBefore,
                                    photoUri = photoUri,
                                    onSaved = {
                                        programSaving = false
                                        TreatmentReminderScheduler.refreshNow(app)
                                        navController.popBackStack()
                                    },
                                    onError = { programSaving = false; programError = it }
                                )
                                return@PrimaryAction
                            }
                            plantsVm.savePlantCard(
                                plantId = plantId,
                                plantName = plantName.trim(),
                                tasks = taskDrafts,
                                wateringInterval = interval,
                                drugId = selectedDrug?.id,
                                drugNote = selectedDrug?.careNote,
                                gardenId = selectedGarden?.id,
                                drugName = selectedDrug?.name ?: "Препарат не требуется",
                                gardenName = selectedGarden?.name.orEmpty(),
                                repeatType = repeatType.name,
                                repeatInterval = interval,
                                repeatDaysOfWeek = weekDays.sortedBy { it.value }.joinToString(",") { it.value.toString() },
                                repeatEndType = if (repeatType == RepeatType.NONE) RepeatEndType.NEVER.name else endType.name,
                                repeatEndDate = endDate.toString().takeIf { repeatType != RepeatType.NONE && endType == RepeatEndType.UNTIL_DATE },
                                repeatCount = countText.toIntOrNull()?.coerceAtLeast(1)
                                    .takeIf { repeatType != RepeatType.NONE && endType == RepeatEndType.COUNT },
                                reminderDaysBefore = reminderDaysBefore,
                                photoUri = photoUri,
                                onSaved = {
                                    normalizedTaskNames.forEach(tasksVm::addTask)
                                    TreatmentReminderScheduler.refreshNow(app)
                                    navController.popBackStack()
                                }
                            )
                        },
                        enabled = !programSaving && !drugSaving && plantName.isNotBlank() && selectedGarden != null && taskDrafts.isNotEmpty() &&
                            taskDrafts.all { it.name.isNotBlank() } && !invalidRepeatEnd && !invalidRepeatCount && !photoLoading,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                    )
                    if (editingProgram) programError?.let { Text(it, color = Danger) }
                }
            }
        }
    }

    if (addDrugDialogOpen) {
        AddDrugDialog(
            onDismiss = { addDrugDialogOpen = false },
            onSaved = { drug ->
                val index = newDrugForRow
                if (index == null) selectedDrug = drug
                else programRowsDraft = programRowsDraft.toMutableList().also {
                    it[index] = it[index].withPersonalProduct(drug, taskDrafts[index].startDate, it[index].reminderDaysBefore)
                }
                if (index != null) taskDrafts = taskDrafts.toMutableList().also {
                    it[index] = it[index].copy(name = programRowsDraft[index].taskName)
                }
                newDrugForRow = null
                addDrugDialogOpen = false
            }
        )
    }

    if (editAfterInspection || editProductIndex != null) {
        val index = editProductIndex
        val row = if (editAfterInspection) programRowsDraft.first() else programRowsDraft[requireNotNull(index)]
        ProgramProductDialog(programId = requireNotNull(row.programId),
            stepId = if (editAfterInspection) null else row.programStepId,
            initialDate = if (editAfterInspection) LocalDate.now() else taskDrafts[requireNotNull(index)].startDate,
            initialReminder = row.reminderDaysBefore,
            onDismiss = { editProductIndex = null; editAfterInspection = false },
            onCustomConfirm = { drug, problem, _, date, reminder ->
                val updated = row.withPersonalProduct(drug, date, reminder, editAfterInspection, problem)
                if (editAfterInspection) {
                    programRowsDraft = programRowsDraft + updated
                    taskDrafts = taskDrafts + PlantTaskDraft(updated.taskName, date)
                } else {
                    val position = requireNotNull(index)
                    programRowsDraft = programRowsDraft.toMutableList().also { it[position] = updated }
                    taskDrafts = taskDrafts.toMutableList().also { it[position] = it[position].copy(name = updated.taskName, startDate = date) }
                }
                editProductIndex = null; editAfterInspection = false
            },
            onConfirm = { product, problem, cultivation, date, reminder ->
                if (editAfterInspection) {
                    val added = row.problemTreatment(problem, product, cultivation, date, reminder)
                    programRowsDraft = programRowsDraft + added
                    taskDrafts = taskDrafts + PlantTaskDraft(added.taskName, date)
                } else {
                    val position = requireNotNull(index)
                    val updated = row.withProgramProduct(product, date, reminder)
                    programRowsDraft = programRowsDraft.toMutableList().also { it[position] = updated }
                    taskDrafts = taskDrafts.toMutableList().also {
                        it[position] = it[position].copy(name = updated.taskName, startDate = date)
                    }
                }
                editProductIndex = null
                editAfterInspection = false
            })
    }

    generatedProgram?.let { program ->
        CareProgramPreviewDialog(
            program = program,
            onProgramChange = { generatedProgram = it },
            error = programError,
            importing = programImporting || photoLoading,
            onDismiss = { if (!programImporting) generatedProgram = null },
            onImport = {
                programImporting = true
                plantsVm.importCareProgram(
                    program = program,
                    gardenId = selectedGarden?.id,
                    gardenName = selectedGarden?.name.orEmpty(),
                    reminderDaysBefore = reminderDaysBefore,
                    photoUri = photoUri,
                    onSaved = { taskTitles ->
                        taskTitles.forEach(tasksVm::addTask)
                        TreatmentReminderScheduler.refreshNow(app)
                        programImporting = false
                        generatedProgram = null
                        navController.popBackStack()
                    },
                    onError = { message ->
                        programImporting = false
                        programError = message
                    }
                )
            }
        )
    }

    taskDatePickerIndex?.let { index ->
        taskDrafts.getOrNull(index)?.let { task ->
            GardenDatePickerDialog(
                title = "Начало работы ${index + 1}",
                initialDate = task.startDate,
                onDateSelected = { date ->
                    taskDrafts = taskDrafts.toMutableList().also { it[index] = task.copy(startDate = date) }
                    taskDatePickerIndex = null
                },
                onDismiss = { taskDatePickerIndex = null }
            )
        }
    }

    pendingStartProposal?.let { proposal ->
        ProgramStartChoiceDialog(
            proposal = proposal,
            onDismiss = { pendingStartProposal = null },
            onChoice = { choice ->
                val continuationNextYearDate = if (choice == ProgramStartChoice.USER_DATE) {
                    proposal.resolve(ProgramStartChoice.NEXT_YEAR)
                } else {
                    null
                }
                calculateProgramForStart(
                    effectiveStartDate = proposal.resolve(choice),
                    continuationNextYearDate = continuationNextYearDate
                )
            }
        )
    }

    unavailableContinuationNextYear?.let { nextYearDate ->
        ProgramContinuationUnavailableDialog(
            nextYearDate = nextYearDate,
            onNextYear = {
                unavailableContinuationNextYear = null
                calculateProgramForStart(nextYearDate)
            },
            onManual = {
                unavailableContinuationNextYear = null
                openManualSetup()
            },
            onDismiss = { unavailableContinuationNextYear = null }
        )
    }
}

private val programDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru"))

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
                        "Перенести программу на следующий год или продолжить с выбранной даты? " +
                        "При продолжении пропущенные работы будут запланированы на следующий год."
                } else {
                    "Запланировать начало работ на рекомендуемую дату — $recommendedText?"
                },
                color = Mist
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onChoice(
                        if (proposal.recommendationHasPassed) {
                            ProgramStartChoice.NEXT_YEAR
                        } else {
                            ProgramStartChoice.RECOMMENDED_DATE
                        }
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
                Text(
                    if (proposal.selectedDate.isAfter(proposal.recommendedDate)) {
                        "Продолжить с моей даты — $userStartText"
                    } else {
                        "Начать с моей даты — $userStartText"
                    },
                    color = Mist
                )
            }
        }
    )
}

@Composable
private fun ProgramContinuationUnavailableDialog(
    nextYearDate: LocalDate,
    onNextYear: () -> Unit,
    onManual: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Forest900,
        titleContentColor = Cream,
        textContentColor = Cream,
        title = { Text("Программа на этот сезон завершена") },
        text = { Text(NO_REMAINING_CARE_MESSAGE, color = Mist) },
        confirmButton = {
            TextButton(onClick = onNextYear) {
                Text("Начать ${nextYearDate.format(programDateFormatter)}", color = Leaf300)
            }
        },
        dismissButton = {
            TextButton(onClick = onManual) {
                Text("Настроить вручную", color = Mist)
            }
        }
    )
}

@Composable
private fun CareProgramPreviewDialog(
    program: GeneratedCareProgram,
    onProgramChange: (GeneratedCareProgram) -> Unit,
    error: String?,
    importing: Boolean,
    onDismiss: () -> Unit,
    onImport: () -> Unit
) {
    var productStepIndex by remember { mutableStateOf<Int?>(null) }
    var problemTreatmentOpen by remember { mutableStateOf(false) }
    val originalSteps = remember(program.instanceId) { program.steps }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            color = Forest900,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
        ) {
            Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Программа для ${program.plantName}", color = Cream, style = MaterialTheme.typography.headlineMedium)
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                Text("${program.cultivationType.displayName} · ${program.climateSummary}", color = Leaf300)
                Text(
                    "Рекомендуемое начало работ: ${program.recommendedStartDate.format(programDateFormatter)}",
                    color = Mist
                )
                program.warning?.let { Text(it, color = Danger) }
                program.steps.forEachIndexed { index, step ->
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(step.title, color = Cream, style = MaterialTheme.typography.titleMedium)
                            Text(step.scheduledDate.toRussianDate(), color = Leaf300)
                            step.recurrence?.let { recurrence ->
                                Text("Повтор: ${recurrence.count} раз", color = Mist)
                            }
                            step.productDescription?.let { description ->
                                Text("Какое средство потребуется", color = Leaf300)
                                Text(description, color = Cream)
                            }
                            Text(step.explanation, color = Mist)
                            if (step.note.isNotBlank()) {
                                ExpandableInfo("Инструкция и ограничения", step.note)
                            }
                            if (step.needsWeatherConfirmation) {
                                Text("Проверьте погоду перед выполнением", color = Danger)
                            }
                            if (ProgramProductCatalog.alternatives(program.templateId, step.templateStepId).isNotEmpty()) {
                                SecondaryAction("Выбрать препарат-аналог", { productStepIndex = index },
                                    Modifier.fillMaxWidth(), enabled = !importing)
                                if (index < originalSteps.size && step != originalSteps[index]) {
                                    SecondaryAction("Вернуть исходное средство и повторы", {
                                        onProgramChange(program.copy(steps = program.steps.toMutableList().also { it[index] = originalSteps[index] }))
                                    }, Modifier.fillMaxWidth(), enabled = !importing)
                                }
                            }
                            if (step.templateStepId.startsWith("problem:")) {
                                SecondaryAction("Убрать дополнительную обработку", {
                                    onProgramChange(program.copy(steps = program.steps.filterIndexed { i, _ -> i != index }))
                                }, Modifier.fillMaxWidth(), enabled = !importing)
                            }
                        }
                    }
                }
                if (ProgramProductCatalog.supports(program.templateId)) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("После осмотра обнаружена проблема", color = Cream, style = MaterialTheme.typography.titleMedium)
                            Text("Если болезнь или вредитель подтверждены, добавьте отдельную обработку. Указывать название проблемы необязательно.", color = Mist)
                            SecondaryAction("Выбрать болезнь / вредителя и препарат", { problemTreatmentOpen = true },
                                Modifier.fillMaxWidth(), enabled = !importing)
                        }
                    }
                }
                error?.let { Text(it, color = Danger) }
                Text(
                    "После добавления каждую работу можно переименовать или перенести вручную.",
                    color = Mist
                )
                if (importing) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(color = Leaf300)
                        Text("Добавляем в календарь…", color = Mist)
                    }
                }
                }
                PrimaryAction("Добавить растение и работы", onImport, Modifier.fillMaxWidth(), enabled = !importing)
                SecondaryAction("Вернуться", onDismiss, Modifier.fillMaxWidth(), enabled = !importing)
            }
        }
    }
    productStepIndex?.let { index ->
        ProgramProductDialog(
            programId = program.templateId, stepId = program.steps[index].templateStepId,
            initialDate = program.steps[index].scheduledDate, minimumDate = program.chosenStartDate,
            initialCultivation = program.cultivationType,
            initialReminder = program.steps[index].reminderDaysBefore ?: 1,
            onDismiss = { productStepIndex = null },
            onCustomConfirm = { drug, _, _, date, reminder ->
                onProgramChange(program.withPersonalProduct(index, drug, date, reminder)); productStepIndex = null
            },
            onConfirm = { product, _, _, date, reminder ->
                onProgramChange(program.withProduct(index, product, date, reminder))
                productStepIndex = null
            }
        )
    }
    if (problemTreatmentOpen) {
        ProgramProductDialog(
            programId = program.templateId, initialCultivation = program.cultivationType,
            initialDate = maxOf(LocalDate.now(), program.chosenStartDate),
            minimumDate = maxOf(LocalDate.now(), program.chosenStartDate),
            onDismiss = { problemTreatmentOpen = false },
            onCustomConfirm = { drug, problem, _, date, reminder ->
                onProgramChange(program.withPersonalProduct(null, drug, date, reminder, problem)); problemTreatmentOpen = false
            },
            onConfirm = { product, problem, _, date, reminder ->
                onProgramChange(program.withProblemTreatment(problem, product, date, reminder))
                problemTreatmentOpen = false
            }
        )
    }
}

@Composable
internal fun <T> SelectionMenu(
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
            trailingIcon = {
                Text("Выбрать", color = Leaf300, style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp), maxLines = 1)
            },
            singleLine = true,
            colors = glassTextFieldColors(),
            shape = CompactGlassShape,
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            Modifier.matchParentSize()
                .semantics { contentDescription = "$label. Сейчас: $value" }
                .clickable(role = Role.Button) { expanded = true }
        )
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
internal fun AddDrugDialog(
    onDismiss: () -> Unit,
    onSaved: (DrugEntity) -> Unit
) {
    val app = LocalContext.current.applicationContext as BookeeperApp
    val vm: DrugsViewmodel = viewModel(factory = DrugsViewmodelFactory(app.repository))
    var name by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("UNSPECIFIED") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        containerColor = Forest900,
        titleContentColor = Cream,
        textContentColor = Cream,
        title = { Text("Новый препарат") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Средство сохранится в Справочнике — Мои средства.", color = Mist)
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
                val methods = mapOf("UNSPECIFIED" to "Уточню позже", "TREATMENT" to "Внекорневое применение",
                    "FERTILIZER" to "Корневое применение", "BOTH" to "Оба способа по инструкции")
                SelectionMenu("Способ применения", methods.getValue(method), methods.keys.toList(), { methods.getValue(it) }, { method = it })
                error?.let { Text(it, color = Danger) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    saving = true; error = null
                    vm.addDrug(name, purpose, rate, method, onSaved = onSaved,
                        onError = { error = it; saving = false })
                },
                enabled = name.isNotBlank() && !saving
            ) {
                Text(if (saving) "Сохраняем…" else "Добавить", color = Leaf300)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text("Отмена", color = Mist)
            }
        }
    )
}
