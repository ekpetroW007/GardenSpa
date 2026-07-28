package ru.samates.gardenspa.presentation

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
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
import ru.samates.gardenspa.ui.theme.Danger
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist
import ru.samates.gardenspa.ui.theme.Warning
import ru.samates.gardenspa.viewmodel.PlantsViewmodel
import ru.samates.gardenspa.viewmodel.PlantsViewmodelFactory
import ru.samates.gardenspa.viewmodel.ProceduresViewmodel
import ru.samates.gardenspa.viewmodel.ProceduresViewmodelFactory
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun Calendar(innerPadding: PaddingValues, navController: NavController) {
    val application = LocalContext.current.applicationContext as BookeeperApp
    val plantsVm: PlantsViewmodel = viewModel(factory = PlantsViewmodelFactory(application.repository))
    val proceduresVm: ProceduresViewmodel = viewModel(factory = ProceduresViewmodelFactory(application.repository))
    val plants by plantsVm.plants.collectAsState()
    val procedures by proceduresVm.procedures.collectAsState()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var visibleMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val treatments = scheduledTreatmentsOn(plants, procedures, selectedDate)
    val markedDates = (1..visibleMonth.lengthOfMonth()).mapNotNull { day ->
        visibleMonth.atDay(day).takeIf { scheduledTreatmentsOn(plants, procedures, it).isNotEmpty() }
    }.toSet()

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
        if (treatments.isEmpty()) {
            item { EmptyGlassState("Свободный день", "На выбранную дату процедур нет") }
        }
        items(treatments, key = { "${it.plant.id}:${it.originalDate}" }) { treatment ->
            TreatmentCard(
                treatment = treatment,
                onOpen = { navController.navigate(AppDestinations.plantDetails(treatment.plant.id)) },
                onDelete = { plantsVm.deletePlant(treatment.plant.id) },
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
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    GlassCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (treatment.rescheduled) Text("Перенесено с ${treatment.originalDate}", color = Warning, fontSize = 12.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(treatment.plant.plantName, style = MaterialTheme.typography.titleLarge, color = Cream)
                    Text(treatment.plant.gardenName, color = Leaf300)
                }
                Text("×", color = Danger, modifier = Modifier.clickable(onClick = onDelete))
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
                    enabled = !treatment.completed,
                    modifier = Modifier.weight(1f)
                )
                PrimaryAction(
                    if (treatment.completed) "Выполнено" else "Выполнить",
                    onComplete,
                    Modifier.weight(1f),
                    enabled = !treatment.completed
                )
            }
        }
    }
}
