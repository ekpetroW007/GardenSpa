package ru.samates.gardenspa.presentation

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import ru.samates.gardenspa.BookeeperApp
import ru.samates.gardenspa.domain.ScheduledTreatment
import ru.samates.gardenspa.domain.recurrenceDescription
import ru.samates.gardenspa.domain.scheduledTreatmentsOn
import ru.samates.gardenspa.domain.toDrugDisplayName
import ru.samates.gardenspa.notifications.TreatmentReminderScheduler
import ru.samates.gardenspa.presentation.navigation.AppDestinations
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Forest700
import ru.samates.gardenspa.ui.theme.Forest950
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist
import ru.samates.gardenspa.ui.theme.Warning
import ru.samates.gardenspa.viewmodel.GardenWorkViewModel
import ru.samates.gardenspa.viewmodel.GardenWorkViewModelFactory
import ru.samates.gardenspa.viewmodel.PlantsViewmodel
import ru.samates.gardenspa.viewmodel.PlantsViewmodelFactory
import ru.samates.gardenspa.viewmodel.ProceduresViewmodel
import ru.samates.gardenspa.viewmodel.ProceduresViewmodelFactory

private enum class CalendarMode { TODAY, WEEK, MONTH }
private data class UndoAction(val message: String, val plantId: Int, val originalDate: LocalDate)

@Composable
fun Calendar(innerPadding: PaddingValues, navController: NavController) {
    val application = LocalContext.current.applicationContext as BookeeperApp
    val plantsVm: PlantsViewmodel = viewModel(factory = PlantsViewmodelFactory(application.repository))
    val proceduresVm: ProceduresViewmodel = viewModel(factory = ProceduresViewmodelFactory(application.repository))
    val gardenWorkVm: GardenWorkViewModel = viewModel(factory = GardenWorkViewModelFactory(application.repository))
    val plants by plantsVm.plants.collectAsState()
    val procedures by proceduresVm.procedures.collectAsState()
    val gardenWorkEntries by gardenWorkVm.entries.collectAsState()
    val today = LocalDate.now()
    var mode by remember { mutableStateOf(CalendarMode.TODAY) }
    var selectedDate by remember { mutableStateOf(today) }
    var visibleMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var undoAction by remember { mutableStateOf<UndoAction?>(null) }

    val datesWithGardenWork = gardenWorkEntries.mapNotNull { runCatching { LocalDate.parse(it.workDate) }.getOrNull() }.toSet()
    val markedDates = (1..visibleMonth.lengthOfMonth()).mapNotNull { day ->
        visibleMonth.atDay(day).takeIf { scheduledTreatmentsOn(plants, procedures, it).isNotEmpty() }
    }.toSet() + datesWithGardenWork
    val agendaDays = when (mode) {
        CalendarMode.TODAY -> listOf(today)
        CalendarMode.WEEK -> (0L..6L).map(today::plusDays)
        CalendarMode.MONTH -> listOf(selectedDate)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Понятный план ухода", color = Mist)
                Text("Календарь", style = MaterialTheme.typography.headlineLarge, color = Cream)
                PrimaryAction("Добавить работу", { navController.navigate(AppDestinations.plantAdd(selectedDate.toString())) }, Modifier.fillMaxWidth())
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalendarModeButton("Сегодня", mode == CalendarMode.TODAY, Modifier.weight(1f)) {
                    mode = CalendarMode.TODAY
                    selectedDate = today
                }
                CalendarModeButton("Неделя", mode == CalendarMode.WEEK, Modifier.weight(1f)) { mode = CalendarMode.WEEK }
                CalendarModeButton("Месяц", mode == CalendarMode.MONTH, Modifier.weight(1f)) { mode = CalendarMode.MONTH }
            }
        }
        undoAction?.let { action ->
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(action.message, color = Cream, modifier = Modifier.weight(1f))
                        Text(
                            "Отменить",
                            color = Leaf300,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                proceduresVm.undoChange(action.plantId, action.originalDate) {
                                    TreatmentReminderScheduler.refreshNow(application)
                                }
                                undoAction = null
                            }
                        )
                    }
                }
            }
        }
        if (mode == CalendarMode.MONTH) {
            item {
                MonthCalendar(
                    month = visibleMonth,
                    selectedDate = selectedDate,
                    datesWithTasks = markedDates,
                    onPreviousMonth = { visibleMonth = visibleMonth.minusMonths(1) },
                    onNextMonth = { visibleMonth = visibleMonth.plusMonths(1) },
                    onDateSelected = { selectedDate = it; visibleMonth = YearMonth.from(it) }
                )
            }
        }

        agendaDays.forEach { date ->
            val treatments = scheduledTreatmentsOn(plants, procedures, date)
            val gardenWork = gardenWorkEntries.filter { it.workDate == date.toString() }
            item { SectionTitle(dateTitle(date, today)) }
            if (gardenWork.isNotEmpty()) {
                item {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("Садовая активность", color = Cream, style = MaterialTheme.typography.titleLarge)
                            Text("Примерно ${gardenWork.sumOf { it.calories }.roundToInt()} ккал", color = Leaf300)
                            gardenWork.forEach { Text("${it.activityName}: ${it.minutes} мин", color = Mist) }
                        }
                    }
                }
            }
            if (treatments.isEmpty()) {
                item { EmptyGlassState("Дел нет", "На этот день работы не запланированы") }
            } else {
                items(treatments, key = { "${date}:${it.plant.id}:${it.originalDate}" }) { treatment ->
                    TreatmentCard(
                        treatment = treatment,
                        onOpen = { navController.navigate(AppDestinations.plantDetails(treatment.plant.id)) },
                        onComplete = {
                            proceduresVm.markCompleted(
                                treatment.plant.id,
                                treatment.plant.taskName,
                                treatment.originalDate,
                                treatment.scheduledDate
                            ) {
                                TreatmentReminderScheduler.cancelTreatmentNotification(application, treatment.plant.id, treatment.originalDate.toString())
                            }
                            undoAction = UndoAction("Работа отмечена как выполненная", treatment.plant.id, treatment.originalDate)
                        },
                        onReschedule = { newDate ->
                            proceduresVm.reschedule(treatment.plant.id, treatment.plant.taskName, treatment.originalDate, newDate) {
                                TreatmentReminderScheduler.cancelTreatmentNotification(application, treatment.plant.id, treatment.originalDate.toString())
                                TreatmentReminderScheduler.refreshNow(application)
                            }
                            undoAction = UndoAction("Работа перенесена на ${newDate.toRussianDate(false)}", treatment.plant.id, treatment.originalDate)
                            selectedDate = newDate
                            visibleMonth = YearMonth.from(newDate)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarModeButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Leaf300 else Forest700,
            contentColor = if (selected) Forest950 else Cream
        ),
        shape = RoundedCornerShape(16.dp)
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

private fun dateTitle(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Сегодня, ${date.toRussianDate(false)}"
    today.plusDays(1) -> "Завтра, ${date.toRussianDate(false)}"
    else -> date.toRussianDate(false).replaceFirstChar(Char::uppercase)
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    selectedDate: LocalDate,
    datesWithTasks: Set<LocalDate>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val locale = Locale.forLanguageTag("ru")
    val title = month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    val first = month.atDay(1)
    val gridStart = first.minusDays((first.dayOfWeek.value - 1).toLong())
    GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onPreviousMonth, modifier = Modifier.semantics { contentDescription = "Предыдущий месяц" }) {
                    Text("‹", color = Leaf300, fontSize = 34.sp)
                }
                Text(title, color = Cream, style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onNextMonth, modifier = Modifier.semantics { contentDescription = "Следующий месяц" }) {
                    Text("›", color = Leaf300, fontSize = 34.sp)
                }
            }
            Row(Modifier.fillMaxWidth()) {
                listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach {
                    Text(it, color = Mist, textAlign = TextAlign.Center, fontSize = 13.sp, modifier = Modifier.weight(1f))
                }
            }
            repeat(6) { week ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { weekDay ->
                        val date = gridStart.plusDays((week * 7 + weekDay).toLong())
                        val isSelected = date == selectedDate
                        val taskText = if (date in datesWithTasks) ", есть запланированные работы" else ", работ нет"
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .semantics {
                                    contentDescription = "${date.toRussianDate()}$taskText"
                                    selected = isSelected
                                }
                                .clickable(role = Role.Button) { onDateSelected(date) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                Modifier.size(40.dp).background(if (isSelected) Leaf300 else Color.Transparent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    date.dayOfMonth.toString(),
                                    color = when {
                                        isSelected -> Color(0xFF071D17)
                                        YearMonth.from(date) != month -> Color(0xFF9DAEA5)
                                        else -> Cream
                                    },
                                    fontSize = 14.sp
                                )
                            }
                            Text(if (date in datesWithTasks) "•" else "", color = Leaf300, fontSize = 16.sp, lineHeight = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TreatmentCard(
    treatment: ScheduledTreatment,
    onComplete: () -> Unit,
    onReschedule: (LocalDate) -> Unit,
    onOpen: () -> Unit
) {
    val context = LocalContext.current
    var completionRequested by remember(treatment.plant.id, treatment.originalDate) { mutableStateOf(false) }
    val completed = treatment.completed || completionRequested
    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (treatment.rescheduled) Text("Перенесено с ${treatment.originalDate.toRussianDate(false)}", color = Warning)
            Text(treatment.plant.taskName, style = MaterialTheme.typography.titleLarge, color = Cream)
            Text("${treatment.plant.plantName} · ${treatment.plant.gardenName}", color = Leaf300)
            Text(treatment.plant.drugName.toDrugDisplayName(), color = Mist)
            Text(treatment.plant.recurrenceDescription(), color = Mist)
            SecondaryAction("Подробнее", onOpen, Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryAction(
                    "Перенести",
                    onClick = {
                        val initial = treatment.scheduledDate.coerceAtLeast(LocalDate.now())
                        DatePickerDialog(context, { _, y, m, d -> onReschedule(LocalDate.of(y, m + 1, d)) }, initial.year, initial.monthValue - 1, initial.dayOfMonth).apply {
                            datePicker.minDate = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        }.show()
                    },
                    modifier = Modifier.weight(1f).height(52.dp)
                )
                Button(
                    onClick = {
                        completionRequested = true
                        onComplete()
                    },
                    enabled = !completed,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Leaf300,
                        contentColor = Forest950,
                        disabledContainerColor = Leaf300,
                        disabledContentColor = Forest950
                    )
                ) { CompletionButtonContent(completed) }
            }
        }
    }
}

@Composable
internal fun CompletionButtonContent(completed: Boolean) {
    Box(contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = !completed, enter = fadeIn(), exit = fadeOut() + scaleOut(targetScale = 0.35f)) {
            Text("Готово", fontWeight = FontWeight.SemiBold)
        }
        AnimatedVisibility(
            visible = completed,
            enter = fadeIn() + scaleIn(initialScale = 0.15f, animationSpec = spring(dampingRatio = 0.45f, stiffness = 300f)),
            exit = fadeOut()
        ) { TwigCheckMark() }
    }
}

@Composable
internal fun TwigCheckMark() {
    val twigBrown = Color(0xFF7A4B2C)
    val twigHighlight = Color(0xFFA06B43)
    Canvas(Modifier.size(width = 34.dp, height = 28.dp).semantics { contentDescription = "Выполнено" }) {
        val joint = Offset(size.width * 0.43f, size.height * 0.73f)
        val leftStart = Offset(size.width * 0.18f, size.height * 0.43f)
        val rightEnd = Offset(size.width * 0.83f, size.height * 0.18f)
        val branchWidth = 4.dp.toPx()
        val detailWidth = 1.7.dp.toPx()
        drawLine(twigBrown, leftStart, joint, branchWidth, StrokeCap.Round)
        drawLine(twigBrown, joint, rightEnd, branchWidth, StrokeCap.Round)
        drawLine(twigBrown, Offset(size.width * 0.29f, size.height * 0.56f), Offset(size.width * 0.19f, size.height * 0.67f), detailWidth, StrokeCap.Round)
        drawLine(twigBrown, Offset(size.width * 0.61f, size.height * 0.50f), Offset(size.width * 0.58f, size.height * 0.31f), detailWidth, StrokeCap.Round)
        drawLine(twigHighlight, Offset(size.width * 0.23f, size.height * 0.43f), Offset(size.width * 0.41f, size.height * 0.66f), detailWidth, StrokeCap.Round)
        drawLine(twigHighlight, Offset(size.width * 0.47f, size.height * 0.65f), Offset(size.width * 0.78f, size.height * 0.23f), detailWidth, StrokeCap.Round)
    }
}
