package ru.samates.gardenspa.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.time.LocalDate
import ru.samates.gardenspa.BookeeperApp
import ru.samates.gardenspa.data.database.entity.resolvedCardId
import ru.samates.gardenspa.domain.ScheduledTreatment
import ru.samates.gardenspa.domain.gardenWorkDate
import ru.samates.gardenspa.domain.nearestIncompleteTreatment
import ru.samates.gardenspa.domain.scheduledTreatmentsOn
import ru.samates.gardenspa.domain.toDrugDisplayName
import ru.samates.gardenspa.notifications.TreatmentReminderScheduler
import ru.samates.gardenspa.presentation.navigation.AppDestinations
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Forest950
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist
import ru.samates.gardenspa.viewmodel.GardenWorkViewModel
import ru.samates.gardenspa.viewmodel.GardenWorkViewModelFactory
import ru.samates.gardenspa.viewmodel.GardensViewmodel
import ru.samates.gardenspa.viewmodel.GardensViewmodelFactory
import ru.samates.gardenspa.viewmodel.PlantsViewmodel
import ru.samates.gardenspa.viewmodel.PlantsViewmodelFactory
import ru.samates.gardenspa.viewmodel.ProceduresViewmodel
import ru.samates.gardenspa.viewmodel.ProceduresViewmodelFactory
import ru.samates.gardenspa.viewmodel.UserViewModel

@Composable
fun Profile(
    navController: NavController,
    onScreenSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    userViewModel: UserViewModel
) {
    val context = LocalContext.current
    val app = context.applicationContext as BookeeperApp
    val userLogin by userViewModel.userLogin.collectAsState()
    val userWeightKg by userViewModel.userWeightKg.collectAsState()
    val gardensVm: GardensViewmodel = viewModel(factory = GardensViewmodelFactory(app.repository))
    val plantsVm: PlantsViewmodel = viewModel(factory = PlantsViewmodelFactory(app.repository))
    val proceduresVm: ProceduresViewmodel = viewModel(factory = ProceduresViewmodelFactory(app.repository))
    val gardenWorkVm: GardenWorkViewModel = viewModel(factory = GardenWorkViewModelFactory(app.repository))
    val gardens by gardensVm.gardens.collectAsState()
    val plants by plantsVm.plants.collectAsState()
    val procedures by proceduresVm.procedures.collectAsState()
    val gardenWorkEntries by gardenWorkVm.entries.collectAsState()
    var today by remember { mutableStateOf(LocalDate.now()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        today = LocalDate.now()
    }
    val todayTreatments = scheduledTreatmentsOn(plants, procedures, today)
    val nextTreatments = (1L..7L).flatMap { offset ->
        val date = today.plusDays(offset)
        scheduledTreatmentsOn(plants, procedures, date).map { date to it }
    }.take(3)
    val nearestTreatment = remember(plants, procedures, today) {
        nearestIncompleteTreatment(plants, procedures, today)
    }
    val weatherGarden = selectWeatherGarden(
        gardens = gardens,
        preferredGardenIds = listOfNotNull(nearestTreatment?.plant?.gardenId)
    )
    val currentGardenWork = gardenWorkEntries.filter { it.workDate == gardenWorkDate().toString() }
    var caloriesOpen by remember { mutableStateOf(false) }
    var notificationAllowed by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationAllowed = granted
        if (granted) TreatmentReminderScheduler.refreshNow(app)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Здравствуйте, $userLogin", color = Mist)
                Text("Сегодня, ${today.toRussianDate(includeYear = false)}", color = Cream, style = MaterialTheme.typography.headlineLarge)
                Text(
                    if (todayTreatments.isEmpty()) "Запланированных дел нет" else "В саду ${todayTreatments.size} ${workEnding(todayTreatments.size)}",
                    color = Leaf300,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        item {
            WeatherWindowCard(
                garden = weatherGarden,
                treatment = nearestTreatment,
                onOpenGardens = { onScreenSelected("Сады") },
                onReschedule = { treatment, newDate ->
                    proceduresVm.reschedule(
                        plantId = treatment.plant.id,
                        procedureName = treatment.plant.taskName,
                        originalDate = treatment.originalDate,
                        newDate = newDate
                    ) {
                        TreatmentReminderScheduler.refreshNow(app)
                    }
                }
            )
        }

        if (todayTreatments.isEmpty()) {
            item {
                EmptyGlassState("На сегодня всё", "Можно отдохнуть или добавить новое растение")
            }
        } else {
            items(todayTreatments, key = { "today:${it.plant.id}:${it.originalDate}" }) { treatment ->
                TodayCareCard(
                    treatment = treatment,
                    onOpen = { navController.navigate(AppDestinations.plantDetails(treatment.plant.id)) },
                    onComplete = {
                        proceduresVm.markCompleted(
                            treatment.plant.id,
                            treatment.plant.taskName,
                            treatment.originalDate,
                            treatment.scheduledDate
                        ) {
                            TreatmentReminderScheduler.cancelTreatmentNotification(app, treatment.plant.id, treatment.originalDate.toString())
                        }
                    }
                )
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryAction(
                    "Добавить растение",
                    { navController.navigate(AppDestinations.plantAdd(today.toString())) },
                    Modifier.weight(1f)
                )
                SecondaryAction("Открыть календарь", { onScreenSelected("Календарь") }, Modifier.weight(1f))
            }
        }

        if (!notificationAllowed && plants.isNotEmpty()) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Не пропускайте уход", color = Cream, style = MaterialTheme.typography.titleLarge)
                        Text("GardenSpa может заранее напоминать о запланированных работах.", color = Mist)
                        PrimaryAction(
                            "Включить напоминания",
                            {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        item { SectionTitle("Следующие 7 дней") }
        if (nextTreatments.isEmpty()) {
            item { EmptyGlassState("Ближайших дел нет", "Новые работы появятся здесь автоматически") }
        } else {
            items(nextTreatments, key = { (date, treatment) -> "next:$date:${treatment.plant.id}" }) { (date, treatment) ->
                GlassCard(Modifier.fillMaxWidth(), onClick = { navController.navigate(AppDestinations.plantDetails(treatment.plant.id)) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(date.toRussianDate(includeYear = false), color = Leaf300, fontWeight = FontWeight.SemiBold)
                        Text(treatment.plant.taskName, color = Cream, style = MaterialTheme.typography.titleMedium)
                        Text("${treatment.plant.plantName} · ${treatment.plant.gardenName}", color = Mist)
                    }
                }
            }
        }

        item { SectionTitle("Мой сад") }
        item {
            GlassCard(Modifier.fillMaxWidth(), onClick = { onScreenSelected("Сады") }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${gardenCountText(gardens.size)} · ${plantCountText(plants.map { it.resolvedCardId }.distinct().size)}", color = Cream, style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (gardens.isEmpty()) "Создайте первый сад, чтобы объединить растения и уход" else "Открыть сады и растения",
                        color = Mist
                    )
                    Text("Перейти в мой сад  →", color = Leaf300)
                }
            }
        }

        item { SectionTitle("Дополнительно") }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryAction("Справочник средств и рецептов", { onScreenSelected("Справочник") }, Modifier.fillMaxWidth())
                    SecondaryAction("Садовая активность и калории", { caloriesOpen = !caloriesOpen }, Modifier.fillMaxWidth())
                    SecondaryAction("Настройки и помощь", { navController.navigate(AppDestinations.SETTINGS) }, Modifier.fillMaxWidth())
                }
            }
        }
        if (caloriesOpen) {
            item {
                GardenWorkCaloriesCard(
                    date = gardenWorkDate(),
                    entries = currentGardenWork,
                    savedWeightKg = userWeightKg,
                    onSave = { weightKg, work ->
                        userViewModel.updateWeightKg(weightKg)
                        gardenWorkVm.saveDay(gardenWorkDate(), weightKg, work)
                    }
                )
            }
        }
    }
}

private fun workEnding(count: Int): String = when {
    count % 100 in 11..14 -> "дел"
    count % 10 == 1 -> "дело"
    count % 10 in 2..4 -> "дела"
    else -> "дел"
}

@Composable
private fun TodayCareCard(
    treatment: ScheduledTreatment,
    onOpen: () -> Unit,
    onComplete: () -> Unit
) {
    var completedLocally by remember(treatment.plant.id, treatment.originalDate) { mutableStateOf(false) }
    val completed = treatment.completed || completedLocally
    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(treatment.plant.taskName, style = MaterialTheme.typography.titleLarge, color = Cream)
            Text("${treatment.plant.plantName} · ${treatment.plant.gardenName}", color = Leaf300)
            Text(treatment.plant.drugName.toDrugDisplayName(), color = Mist)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryAction("Подробнее", onOpen, Modifier.weight(1f).height(52.dp))
                Button(
                    onClick = {
                        completedLocally = true
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
                ) {
                    Text(if (completed) "Готово ✓" else "Готово", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
