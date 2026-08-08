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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ru.samates.gardenspa.BookeeperApp
import ru.samates.gardenspa.domain.ScheduledTreatment
import ru.samates.gardenspa.domain.recurrenceDescription
import ru.samates.gardenspa.domain.scheduledTreatmentsOn
import ru.samates.gardenspa.notifications.TreatmentReminderScheduler
import ru.samates.gardenspa.presentation.navigation.AppDestinations
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Forest950
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist
import ru.samates.gardenspa.ui.theme.Warning
import ru.samates.gardenspa.viewmodel.PlantsViewmodel
import ru.samates.gardenspa.viewmodel.PlantsViewmodelFactory
import ru.samates.gardenspa.viewmodel.ProceduresViewmodel
import ru.samates.gardenspa.viewmodel.ProceduresViewmodelFactory
import ru.samates.gardenspa.viewmodel.GardenWorkViewModel
import ru.samates.gardenspa.viewmodel.GardenWorkViewModelFactory
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun Calendar(innerPadding: PaddingValues, navController: NavController) {
    val application = LocalContext.current.applicationContext as BookeeperApp
    val plantsVm: PlantsViewmodel = viewModel(factory = PlantsViewmodelFactory(application.repository))
    val proceduresVm: ProceduresViewmodel = viewModel(factory = ProceduresViewmodelFactory(application.repository))
    val gardenWorkVm: GardenWorkViewModel = viewModel(factory = GardenWorkViewModelFactory(application.repository))
    val plants by plantsVm.plants.collectAsState()
    val procedures by proceduresVm.procedures.collectAsState()
    val gardenWorkEntries by gardenWorkVm.entries.collectAsState()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var visibleMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val treatments = scheduledTreatmentsOn(plants, procedures, selectedDate)
    val selectedGardenWork = gardenWorkEntries.filter { it.workDate == selectedDate.toString() }
    val datesWithGardenWork = gardenWorkEntries.mapNotNull { entry ->
        runCatching { LocalDate.parse(entry.workDate) }.getOrNull()
    }.toSet()
    val markedDates = (1..visibleMonth.lengthOfMonth()).mapNotNull { day ->
        visibleMonth.atDay(day).takeIf { scheduledTreatmentsOn(plants, procedures, it).isNotEmpty() }
    }.toSet() + datesWithGardenWork

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("План ухода", color = Mist)
                    Text("Календарь", style = MaterialTheme.typography.headlineLarge, color = Cream)
                }
                PrimaryAction("+ Процедура", { navController.navigate(AppDestinations.plantAdd(selectedDate.toString())) })
            }
        }
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
        item {
            SectionTitle(selectedDate.format(DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru"))))
        }
        if (selectedGardenWork.isNotEmpty()) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Садовая активность", color = Cream, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "≈ ${selectedGardenWork.sumOf { it.calories }.roundToInt()} ккал",
                            color = Leaf300,
                            style = MaterialTheme.typography.headlineLarge
                        )
                        selectedGardenWork.forEach { entry ->
                            Text("${entry.activityName}: ${entry.minutes} мин", color = Mist)
                        }
                    }
                }
            }
        }
        if (treatments.isEmpty()) {
            item { EmptyGlassState("Свободный день", "На выбранную дату процедур нет") }
        }
        items(treatments, key = { "${it.plant.id}:${it.originalDate}" }) { treatment ->
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
                },
                onReschedule = { newDate ->
                    proceduresVm.reschedule(treatment.plant.id, treatment.plant.taskName, treatment.originalDate, newDate) {
                        TreatmentReminderScheduler.cancelTreatmentNotification(application, treatment.plant.id, treatment.originalDate.toString())
                        TreatmentReminderScheduler.refreshNow(application)
                    }
                    selectedDate = newDate
                    visibleMonth = YearMonth.from(newDate)
                }
            )
        }
    }

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
    GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("‹", color = Leaf300, fontSize = 32.sp, modifier = Modifier.clickable(onClick = onPreviousMonth))
                Text(title, color = Cream, style = MaterialTheme.typography.titleLarge)
                Text("›", color = Leaf300, fontSize = 32.sp, modifier = Modifier.clickable(onClick = onNextMonth))
            }
            Row(Modifier.fillMaxWidth()) {
                listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach {
                    Text(it, color = Mist, textAlign = TextAlign.Center, fontSize = 11.sp, modifier = Modifier.weight(1f))
                }
            }
            repeat(6) { week ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { weekDay ->
                        val date = gridStart.plusDays((week * 7 + weekDay).toLong())
                        val selected = date == selectedDate
                        Column(
                            modifier = Modifier.weight(1f).clickable { onDateSelected(date) }.padding(vertical = 3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                Modifier.size(34.dp).background(if (selected) Leaf300 else Color.Transparent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    date.dayOfMonth.toString(),
                                    color = when {
                                        selected -> Color(0xFF071D17)
                                        YearMonth.from(date) != month -> Color(0x667C9185)
                                        else -> Cream
                                    },
                                    fontSize = 13.sp
                                )
                            }
                            Box(Modifier.size(4.dp).background(if (date in datesWithTasks) Leaf300 else Color.Transparent, CircleShape))
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
    var completionRequested by remember(treatment.plant.id, treatment.originalDate) {
        mutableStateOf(false)
    }
    val completed = treatment.completed || completionRequested
    GlassCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (treatment.rescheduled) Text("Перенесено с ${treatment.originalDate}", color = Warning, fontSize = 12.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(treatment.plant.plantName, style = MaterialTheme.typography.titleLarge, color = Cream)
                    Text(treatment.plant.gardenName, color = Leaf300)
                }
            }
            Text(treatment.plant.taskName, color = Cream)
            Text("${treatment.plant.drugName} · ${treatment.plant.recurrenceDescription()}", color = Mist, fontSize = 13.sp)
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
                    ),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 0.dp)
                ) {
                    CompletionButtonContent(completed)
                }
            }
        }
    }
}

@Composable
internal fun CompletionButtonContent(completed: Boolean) {
    Box(contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = !completed,
            enter = fadeIn(),
            exit = fadeOut() + scaleOut(targetScale = 0.35f)
        ) {
            Text("Выполнено", fontWeight = FontWeight.SemiBold)
        }
        AnimatedVisibility(
            visible = completed,
            enter = fadeIn() + scaleIn(
                initialScale = 0.15f,
                animationSpec = spring(
                    dampingRatio = 0.45f,
                    stiffness = 300f
                )
            ),
            exit = fadeOut()
        ) {
            TwigCheckMark()
        }
    }
}

@Composable
internal fun TwigCheckMark() {
    val twigBrown = Color(0xFF7A4B2C)
    val twigHighlight = Color(0xFFA06B43)

    Canvas(Modifier.size(width = 34.dp, height = 28.dp)) {
        val joint = Offset(size.width * 0.43f, size.height * 0.73f)
        val leftStart = Offset(size.width * 0.18f, size.height * 0.43f)
        val rightEnd = Offset(size.width * 0.83f, size.height * 0.18f)
        val branchWidth = 4.dp.toPx()
        val detailWidth = 1.7.dp.toPx()

        drawLine(twigBrown, leftStart, joint, branchWidth, StrokeCap.Round)
        drawLine(twigBrown, joint, rightEnd, branchWidth, StrokeCap.Round)

        drawLine(
            twigBrown,
            Offset(size.width * 0.29f, size.height * 0.56f),
            Offset(size.width * 0.19f, size.height * 0.67f),
            detailWidth,
            StrokeCap.Round
        )
        drawLine(
            twigBrown,
            Offset(size.width * 0.61f, size.height * 0.50f),
            Offset(size.width * 0.58f, size.height * 0.31f),
            detailWidth,
            StrokeCap.Round
        )
        drawLine(
            twigHighlight,
            Offset(size.width * 0.23f, size.height * 0.43f),
            Offset(size.width * 0.41f, size.height * 0.66f),
            detailWidth,
            StrokeCap.Round
        )
        drawLine(
            twigHighlight,
            Offset(size.width * 0.47f, size.height * 0.65f),
            Offset(size.width * 0.78f, size.height * 0.23f),
            detailWidth,
            StrokeCap.Round
        )
    }
}
