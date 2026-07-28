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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import ru.samates.gardenspa.data.database.entity.DrugEntity
import ru.samates.gardenspa.data.database.entity.GardenEntity
import ru.samates.gardenspa.domain.RepeatEndType
import ru.samates.gardenspa.domain.RepeatType
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Forest700
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

@Composable
fun PlantAdd(navController: NavController, selectedDate: String) {
    val context = LocalContext.current
    val app = context.applicationContext as BookeeperApp
    val plantsVm: PlantsViewmodel = viewModel(factory = PlantsViewmodelFactory(app.repository))
    val drugsVm: DrugsViewmodel = viewModel(factory = DrugsViewmodelFactory(app.repository))
    val gardensVm: GardensViewmodel = viewModel(factory = GardensViewmodelFactory(app.repository))
    val tasksVm: TasksViewmodel = viewModel(factory = TasksViewmodelFactory(app.repository))
    val drugs by drugsVm.drugs.collectAsState()
    val gardens by gardensVm.gardens.collectAsState()
    val startDate = remember(selectedDate) { runCatching { LocalDate.parse(selectedDate) }.getOrDefault(LocalDate.now()) }

    var plantName by remember { mutableStateOf("") }
    var taskName by remember { mutableStateOf("") }
    var selectedDrug by remember { mutableStateOf<DrugEntity?>(null) }
    var selectedGarden by remember { mutableStateOf<GardenEntity?>(null) }
    var repeatType by remember { mutableStateOf(RepeatType.NONE) }
    var intervalText by remember { mutableStateOf("1") }
    var weekDays by remember { mutableStateOf(setOf(startDate.dayOfWeek)) }
    var endType by remember { mutableStateOf(RepeatEndType.NEVER) }
    var endDate by remember { mutableStateOf(startDate.plusMonths(1)) }
    var countText by remember { mutableStateOf("10") }

    BotanicalBackground {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Новая процедура", startDate.toString(), onBack = { navController.popBackStack() })
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Растение и уход", color = Cream, style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(plantName, { plantName = it }, label = { Text("Название растения") }, singleLine = true, colors = glassTextFieldColors(), shape = CompactGlassShape, modifier = Modifier.fillMaxWidth())
                        SelectionMenu("Сад", selectedGarden?.name ?: "Не выбран", gardens, { it.name }) { selectedGarden = it }
                        OutlinedTextField(taskName, { taskName = it }, label = { Text("Процедура / задача") }, singleLine = true, colors = glassTextFieldColors(), shape = CompactGlassShape, modifier = Modifier.fillMaxWidth())
                        SelectionMenu("Препарат", selectedDrug?.name ?: "Не выбран", drugs, { it.name }) { selectedDrug = it }
                    }
                }
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Расписание", color = Cream, style = MaterialTheme.typography.titleLarge)
                        SelectionMenu("Повтор", repeatLabels.getValue(repeatType), repeatLabels.keys.toList(), { repeatLabels.getValue(it) }) { repeatType = it }
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
                            SelectionMenu("Окончание", endLabels.getValue(endType), endLabels.keys.toList(), { endLabels.getValue(it) }) { endType = it }
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
                    "Сохранить процедуру",
                    onClick = {
                        val interval = intervalText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                        plantsVm.addPlant(
                            plantName = plantName.trim(),
                            taskName = taskName.trim(),
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
                            repeatEndDate = endDate.toString().takeIf { endType == RepeatEndType.UNTIL_DATE },
                            repeatCount = countText.toIntOrNull()?.coerceAtLeast(1).takeIf { endType == RepeatEndType.COUNT }
                        )
                        tasksVm.addTask(taskName.trim())
                        navController.popBackStack()
                    },
                    enabled = plantName.isNotBlank() && taskName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun <T> SelectionMenu(
    label: String,
    value: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit
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
        }
    }
}
