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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ru.samates.gardenspa.BookeeperApp
import ru.samates.gardenspa.domain.ScheduledTreatment
import ru.samates.gardenspa.domain.scheduledTreatmentsOn
import ru.samates.gardenspa.domain.gardenWorkDate
import ru.samates.gardenspa.domain.nextGardenWorkReset
import ru.samates.gardenspa.notifications.TreatmentReminderScheduler
import ru.samates.gardenspa.presentation.navigation.AppDestinations
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Forest950
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist
import ru.samates.gardenspa.viewmodel.DrugsViewmodel
import ru.samates.gardenspa.viewmodel.DrugsViewmodelFactory
import ru.samates.gardenspa.viewmodel.GardensViewmodel
import ru.samates.gardenspa.viewmodel.GardensViewmodelFactory
import ru.samates.gardenspa.viewmodel.GardenWorkViewModel
import ru.samates.gardenspa.viewmodel.GardenWorkViewModelFactory
import ru.samates.gardenspa.viewmodel.PlantsViewmodel
import ru.samates.gardenspa.viewmodel.PlantsViewmodelFactory
import ru.samates.gardenspa.viewmodel.ProceduresViewmodel
import ru.samates.gardenspa.viewmodel.ProceduresViewmodelFactory
import java.time.LocalDate
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZonedDateTime
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun Profile(
    navController: NavController,
    onScreenSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    userLogin: String = "Садовод",
    userWeightKg: Double = 70.0,
    onWeightChanged: (Double) -> Unit = {}
) {
    val app = LocalContext.current.applicationContext as BookeeperApp
    val gardensVm: GardensViewmodel = viewModel(factory = GardensViewmodelFactory(app.repository))
    val plantsVm: PlantsViewmodel = viewModel(factory = PlantsViewmodelFactory(app.repository))
    val drugsVm: DrugsViewmodel = viewModel(factory = DrugsViewmodelFactory(app.repository))
    val proceduresVm: ProceduresViewmodel = viewModel(factory = ProceduresViewmodelFactory(app.repository))
    val gardenWorkVm: GardenWorkViewModel = viewModel(factory = GardenWorkViewModelFactory(app.repository))
    val gardens by gardensVm.gardens.collectAsState()
    val plants by plantsVm.plants.collectAsState()
    val drugs by drugsVm.drugs.collectAsState()
    val procedures by proceduresVm.procedures.collectAsState()
    val gardenWorkEntries by gardenWorkVm.entries.collectAsState()
    val today = LocalDate.now()
    var calorieDate by remember { mutableStateOf(gardenWorkDate()) }
    val todayTreatments = scheduledTreatmentsOn(plants, procedures, today)
    val currentGardenWork = gardenWorkEntries.filter { it.workDate == calorieDate.toString() }
    val todayCalories = currentGardenWork.sumOf { it.calories }.roundToInt()
    var caloriesCardOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = ZonedDateTime.now()
            val waitMillis = Duration.between(now, nextGardenWorkReset(now))
                .toMillis()
                .coerceAtLeast(1_000L)
            delay(waitMillis + 250L)
            calorieDate = gardenWorkDate(LocalDateTime.now())
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column {
                    Text("Добро пожаловать,", color = Mist)
                    Text(userLogin, style = MaterialTheme.typography.headlineLarge, color = Cream)
                    Text(
                        "Сегодня в саду всё идёт по плану. Проверьте ближайшие процедуры и продолжайте в своём ритме.",
                        color = Mist,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                    PrimaryAction(
                        text = "Открыть календарь  →",
                        onClick = { onScreenSelected("Календарь") },
                        modifier = Modifier.padding(top = 18.dp)
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassCard(
                    Modifier.weight(1f).height(82.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    onClick = { onScreenSelected("Мои сады") }
                ) {
                    HomeMetric(gardens.size.toString(), "садов", Modifier.fillMaxSize())
                }
                GlassCard(
                    Modifier.weight(1f).height(82.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    onClick = { navController.navigate(AppDestinations.ALL_PLANTS) }
                ) {
                    HomeMetric(plants.size.toString(), "процедур", Modifier.fillMaxSize())
                }
                GlassCard(
                    Modifier.weight(1f).height(82.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    onClick = { onScreenSelected("Препараты") }
                ) {
                    HomeMetric(drugs.size.toString(), "препаратов", Modifier.fillMaxSize())
                }
                GlassCard(
                    Modifier.weight(1f).height(82.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    onClick = { caloriesCardOpen = !caloriesCardOpen }
                ) {
                    HomeMetric(todayCalories.toString(), "ккал", Modifier.fillMaxSize())
                }
            }
        }
        if (caloriesCardOpen) {
            item {
                GardenWorkCaloriesCard(
                    date = calorieDate,
                    entries = currentGardenWork,
                    savedWeightKg = userWeightKg,
                    onSave = { weightKg, work ->
                        onWeightChanged(weightKg)
                        gardenWorkVm.saveDay(calorieDate, weightKg, work)
                    }
                )
            }
        }
        item { SectionTitle("Быстрые действия") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryAction("+ Сад", { navController.navigate(AppDestinations.GARDEN_ADD) }, Modifier.weight(1f))
                SecondaryAction("+ Процедура", { navController.navigate(AppDestinations.plantAdd(today.toString())) }, Modifier.weight(1f))
            }
        }
        item { SectionTitle("Сегодня") }
        if (todayTreatments.isEmpty()) {
            item { EmptyGlassState("На сегодня всё", "Новых процедур не запланировано") }
        } else {
            items(todayTreatments, key = { "today:${it.plant.id}:${it.originalDate}" }) { treatment ->
                TodayTreatmentCard(
                    treatment = treatment,
                    onOpen = { navController.navigate(AppDestinations.plantDetails(treatment.plant.id)) },
                    onComplete = {
                        proceduresVm.markCompleted(
                            treatment.plant.id,
                            treatment.plant.taskName,
                            treatment.originalDate,
                            treatment.scheduledDate
                        ) {
                            TreatmentReminderScheduler.cancelTreatmentNotification(
                                app,
                                treatment.plant.id,
                                treatment.originalDate.toString()
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TodayTreatmentCard(
    treatment: ScheduledTreatment,
    onOpen: () -> Unit,
    onComplete: () -> Unit
) {
    var completionRequested by remember(treatment.plant.id, treatment.originalDate) {
        mutableStateOf(false)
    }
    val completed = treatment.completed || completionRequested

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(treatment.plant.plantName, style = MaterialTheme.typography.titleLarge, color = Cream)
            Text(treatment.plant.taskName, color = Leaf300)
            Text("${treatment.plant.gardenName} · ${treatment.plant.drugName}", color = Mist)
            Button(
                onClick = {
                    completionRequested = true
                    onComplete()
                },
                enabled = !completed,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Leaf300,
                    contentColor = Forest950,
                    disabledContainerColor = Leaf300,
                    disabledContentColor = Forest950
                )
            ) {
                CompletionButtonContent(completed)
            }
        }
    }
}

@Composable
private fun HomeMetric(value: String, label: String, modifier: Modifier = Modifier) {
    val valueFontSize = when {
        value.length >= 5 -> 17.sp
        value.length >= 4 -> 20.sp
        else -> 24.sp
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            color = ru.samates.gardenspa.ui.theme.Leaf200,
            fontSize = valueFontSize,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            color = Mist,
            fontSize = 9.sp,
            lineHeight = 12.sp,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center
        )
    }
}
