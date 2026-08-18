package ru.samates.gardenspa.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.time.LocalDate
import ru.samates.gardenspa.BookeeperApp
import ru.samates.gardenspa.domain.ScheduledTreatment
import ru.samates.gardenspa.domain.scheduledTreatmentsOn
import ru.samates.gardenspa.domain.toDrugDisplayName
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
import ru.samates.gardenspa.viewmodel.PlantsViewmodel
import ru.samates.gardenspa.viewmodel.PlantsViewmodelFactory
import ru.samates.gardenspa.viewmodel.ProceduresViewmodel
import ru.samates.gardenspa.viewmodel.ProceduresViewmodelFactory

@Composable
fun Profile(navController: NavController, onScreenSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as BookeeperApp
    val gardensVm: GardensViewmodel = viewModel(factory = GardensViewmodelFactory(app.repository))
    val plantsVm: PlantsViewmodel = viewModel(factory = PlantsViewmodelFactory(app.repository))
    val drugsVm: DrugsViewmodel = viewModel(factory = DrugsViewmodelFactory(app.repository))
    val proceduresVm: ProceduresViewmodel = viewModel(factory = ProceduresViewmodelFactory(app.repository))
    val gardens by gardensVm.gardens.collectAsState()
    val plants by plantsVm.plants.collectAsState()
    val drugs by drugsVm.drugs.collectAsState()
    val procedures by proceduresVm.procedures.collectAsState()
    val today = LocalDate.now()
    val todayTreatments = scheduledTreatmentsOn(plants, procedures, today)

    LazyColumn(modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                        proceduresVm.markCompleted(treatment.plant.id, treatment.plant.taskName, treatment.originalDate, treatment.scheduledDate) {
                            TreatmentReminderScheduler.cancelTreatmentNotification(app, treatment.plant.id, treatment.originalDate.toString())
                        }
                    }
                )
            }
        }
        item { SectionTitle("Статистика") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassCard(Modifier.weight(1f).height(82.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(6.dp), onClick = { onScreenSelected("Мои сады") }) {
                    HomeMetric(gardens.size.toString(), "садов", Modifier.fillMaxSize())
                }
                GlassCard(Modifier.weight(1f).height(82.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(6.dp), onClick = { navController.navigate(AppDestinations.ALL_PLANTS) }) {
                    HomeMetric(plants.size.toString(), "процедур", Modifier.fillMaxSize())
                }
                GlassCard(Modifier.weight(1f).height(82.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(6.dp), onClick = { onScreenSelected("Справочник") }) {
                    HomeMetric(drugs.size.toString(), "препаратов", Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun TodayTreatmentCard(treatment: ScheduledTreatment, onOpen: () -> Unit, onComplete: () -> Unit) {
    var completionRequested by remember(treatment.plant.id, treatment.originalDate) { mutableStateOf(false) }
    val completed = treatment.completed || completionRequested
    val userText = if (treatment.plant.programId == null) treatment.plant.taskName else treatment.plant.plantDetails

    GlassCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (treatment.plant.programId == null) treatment.plant.plantName else treatment.plant.taskName, style = MaterialTheme.typography.titleLarge, color = Cream)
            if (userText.isNotBlank()) Text(userText, color = Leaf300)
            Text("${treatment.plant.gardenName} · ${treatment.plant.drugName.toDrugDisplayName()}", color = Mist)
            Button(
                onClick = { completionRequested = true; onComplete() },
                enabled = !completed,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Leaf300, contentColor = Forest950, disabledContainerColor = Leaf300, disabledContentColor = Forest950)
            ) {
                CompletionButtonContent(completed)
            }
        }
    }
}

@Composable
private fun HomeMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(value, color = ru.samates.gardenspa.ui.theme.Leaf200, fontSize = if (value.length >= 4) 20.sp else 24.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, textAlign = TextAlign.Center)
        Text(label, color = Mist, fontSize = 9.sp, lineHeight = 12.sp, maxLines = 1, softWrap = false, textAlign = TextAlign.Center)
    }
}
