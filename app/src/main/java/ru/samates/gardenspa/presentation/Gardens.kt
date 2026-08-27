package ru.samates.gardenspa.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import ru.samates.gardenspa.BookeeperApp
import ru.samates.gardenspa.data.database.entity.GardenEntity
import ru.samates.gardenspa.data.database.entity.climateOrNull
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.domain.PlantCard
import ru.samates.gardenspa.domain.toDrugDisplayName
import ru.samates.gardenspa.domain.toPlantCards
import ru.samates.gardenspa.presentation.navigation.AppDestinations
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Danger
import ru.samates.gardenspa.ui.theme.Forest900
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist
import ru.samates.gardenspa.viewmodel.GardensViewmodel
import ru.samates.gardenspa.viewmodel.GardensViewmodelFactory
import ru.samates.gardenspa.viewmodel.PlantsViewmodel
import ru.samates.gardenspa.viewmodel.PlantsViewmodelFactory

@Composable
fun MyGardens(navController: NavController, innerPadding: PaddingValues) {
    val context = LocalContext.current
    val application = context.applicationContext as BookeeperApp
    val gardensVm: GardensViewmodel = viewModel(factory = GardensViewmodelFactory(application.repository))
    val plantsVm: PlantsViewmodel = viewModel(factory = PlantsViewmodelFactory(application.repository))
    val gardens by gardensVm.gardens.collectAsState()
    val plants by plantsVm.plants.collectAsState()
    var gardenForActions by remember { mutableStateOf<GardenEntity?>(null) }
    var gardenPendingDelete by remember { mutableStateOf<GardenEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Растения по участкам", color = Mist)
                Text("Мой сад", style = MaterialTheme.typography.headlineLarge, color = Cream)
                PrimaryAction("Создать новый сад", { navController.navigate(AppDestinations.GARDEN_ADD) }, Modifier.fillMaxWidth())
            }
        }
        if (gardens.isEmpty()) {
            item { EmptyGlassState("Создайте первый сад", "Укажите название и место, а затем добавьте растения") }
        }
        items(gardens, key = { it.id }) { garden ->
            val gardenPlantRows = plants.filter { it.gardenId == garden.id }
            GardenGlassCard(
                garden = garden,
                plantCards = gardenPlantRows.toPlantCards(),
                onActions = { gardenForActions = garden },
                onLocation = { navController.navigate(AppDestinations.gardenLocation(garden.id)) },
                onPlantOpen = { navController.navigate(AppDestinations.plantDetails(it)) },
                onAddPlant = {
                    navController.navigate(AppDestinations.plantAdd(LocalDate.now().toString(), garden.id))
                }
            )
        }
    }

    gardenForActions?.let { garden ->
        val rows = plants.filter { it.gardenId == garden.id }
        AlertDialog(
            onDismissRequest = { gardenForActions = null },
            containerColor = Forest900,
            titleContentColor = Cream,
            textContentColor = Cream,
            title = { Text("Действия с садом «${garden.name}»") },
            text = { Text("Экспорт сохранит список растений в текстовый файл. Удаление сада нельзя отменить.", color = Mist) },
            confirmButton = {
                TextButton(onClick = {
                    exportGardenToFile(context, garden.name, rows)
                    gardenForActions = null
                }) { Text("Экспортировать", color = Leaf300) }
            },
            dismissButton = {
                TextButton(onClick = {
                    gardenForActions = null
                    gardenPendingDelete = garden
                }) { Text("Удалить сад", color = Danger) }
            }
        )
    }

    gardenPendingDelete?.let { garden ->
        DeleteConfirmationDialog(
            itemName = "Сад «${garden.name}». Растения останутся без привязки к саду.",
            onConfirm = {
                gardensVm.deleteGarden(garden.id)
                gardenPendingDelete = null
            },
            onDismiss = { gardenPendingDelete = null }
        )
    }
}

@Composable
private fun GardenGlassCard(
    garden: GardenEntity,
    plantCards: List<PlantCard>,
    onActions: () -> Unit,
    onLocation: () -> Unit,
    onPlantOpen: (Int) -> Unit,
    onAddPlant: () -> Unit
) {
    val next = plantCards.flatMap { it.procedures }
        .mapNotNull { row -> runCatching { LocalDate.parse(row.creationDate) }.getOrNull()?.let { it to row } }
        .filter { (date, _) -> !date.isBefore(LocalDate.now()) }
        .minByOrNull { it.first }
    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(garden.name, style = MaterialTheme.typography.headlineMedium, color = Cream)
            if (garden.locationName.isNullOrBlank()) {
                Text("Место не указано — сроки программ могут быть неточными", color = Mist)
                SecondaryAction("Указать место сада", onLocation, Modifier.fillMaxWidth())
            } else {
                Text("Место: ${garden.locationName}", color = Leaf300)
                garden.climateOrNull()?.let { Text("Условия: ${it.displayName()}", color = Mist) }
                garden.climateUpdatedAt?.substringBefore('T')?.let {
                    Text("Расчёт обновлён ${it.toRussianDateOrSelf()}", color = Mist)
                }
                Text("Изменить место", color = Mist, modifier = Modifier.clickable(onClick = onLocation))
            }
            Text("${plantCountText(plantCards.size)} · ${plantCards.sumOf { it.procedures.size }} работ по уходу", color = Mist)
            next?.let { (date, row) ->
                GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Ближайшая работа — ${date.toRussianDate(false)}", color = Leaf300)
                        Text("${row.taskName}: ${row.plantName}", color = Cream)
                    }
                }
            }
            if (plantCards.isEmpty()) {
                Text("В этом саду пока нет растений", color = Mist)
            } else {
                plantCards.take(4).forEach { card ->
                    val plant = card.primary
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onPlantOpen(plant.id) }.padding(vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PlantPhoto(plant.photoUri, plant.plantName, Modifier.size(58.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(plant.plantName, color = Cream, style = MaterialTheme.typography.titleMedium)
                            Text("${card.procedures.size} работ по уходу", color = Mist)
                        }
                        Text("Открыть", color = Leaf300)
                    }
                }
            }
            PrimaryAction("Добавить растение", onAddPlant, Modifier.fillMaxWidth())
            SecondaryAction("Действия с садом", onActions, Modifier.fillMaxWidth())
        }
    }
}

private fun exportGardenToFile(context: Context, gardenName: String, plants: List<PlantEntity>) {
    try {
        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val content = buildString {
            appendLine("Сад: $gardenName")
            appendLine("Дата экспорта: ${formatter.format(Date())}")
            appendLine("=".repeat(40))
            plants.forEach { appendLine("${it.plantName} — ${it.taskName} — ${it.drugName.toDrugDisplayName()}") }
            appendLine("Всего растений: ${plants.toPlantCards().size}")
        }
        val fileStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(context.getExternalFilesDir(null), "сад_${gardenName}_$fileStamp.txt")
        file.writeText(content, Charsets.UTF_8)
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Экспорт сада: $gardenName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Экспортировать сад"))
    } catch (e: Exception) {
        Toast.makeText(context, "Не удалось экспортировать сад: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
