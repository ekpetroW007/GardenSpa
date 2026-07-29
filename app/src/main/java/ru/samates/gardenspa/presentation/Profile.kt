package ru.samates.gardenspa.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ru.samates.gardenspa.BookeeperApp
import ru.samates.gardenspa.domain.scheduledTreatmentsOn
import ru.samates.gardenspa.domain.toPlantCards
import ru.samates.gardenspa.presentation.navigation.AppDestinations
import ru.samates.gardenspa.ui.theme.Cream
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
import java.time.LocalDate

@Composable
fun Profile(
    navController: NavController,
    onScreenSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    userLogin: String = "Садовод"
) {
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassCard(
                    Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
                    onClick = { onScreenSelected("Мои сады") }
                ) {
                    Metric(gardens.size.toString(), "садов", Modifier.fillMaxWidth())
                }
                GlassCard(
                    Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
                    onClick = { navController.navigate(AppDestinations.ALL_PLANTS) }
                ) {
                    Metric(plants.toPlantCards().size.toString(), "растений", Modifier.fillMaxWidth())
                }
                GlassCard(
                    Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
                    onClick = { onScreenSelected("Препараты") }
                ) {
                    Metric(drugs.size.toString(), "препаратов", Modifier.fillMaxWidth())
                }
            }
        }
        item { SectionTitle("Быстрые действия") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryAction("+ Сад", { navController.navigate(AppDestinations.GARDEN_ADD) }, Modifier.weight(1f))
                    SecondaryAction("+ Растение", { navController.navigate(AppDestinations.plantAdd(today.toString())) }, Modifier.weight(1f))
                }
                SecondaryAction(
                    "В календарь",
                    { onScreenSelected("Календарь") },
                    Modifier.fillMaxWidth()
                )
            }
        }
        item { SectionTitle("Сегодня") }
        if (todayTreatments.isEmpty()) {
            item { EmptyGlassState("На сегодня всё", "Новых процедур не запланировано") }
        } else {
            items(todayTreatments.size) { index ->
                val treatment = todayTreatments[index]
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { navController.navigate(AppDestinations.plantDetails(treatment.plant.id)) }
                ) {
                    Column {
                        Text(treatment.plant.plantName, style = MaterialTheme.typography.titleLarge, color = Cream)
                        Text(treatment.plant.taskName, color = Leaf300)
                        Text("${treatment.plant.gardenName} · ${treatment.plant.drugName}", color = Mist)
                    }
                }
            }
        }
    }
}
