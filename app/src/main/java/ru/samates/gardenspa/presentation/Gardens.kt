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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ru.samates.gardenspa.BookeeperApp
import ru.samates.gardenspa.data.database.entity.GardenEntity
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.presentation.navigation.AppDestinations
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Danger
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist
import ru.samates.gardenspa.viewmodel.GardensViewmodel
import ru.samates.gardenspa.viewmodel.GardensViewmodelFactory
import ru.samates.gardenspa.viewmodel.PlantsViewmodel
import ru.samates.gardenspa.viewmodel.PlantsViewmodelFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MyGardens(navController: NavController, innerPadding: PaddingValues) {
    val context = LocalContext.current
    val application = context.applicationContext as BookeeperApp
    val gardensVm: GardensViewmodel = viewModel(factory = GardensViewmodelFactory(application.repository))
    val plantsVm: PlantsViewmodel = viewModel(factory = PlantsViewmodelFactory(application.repository))
    val gardens by gardensVm.gardens.collectAsState()
    val plants by plantsVm.plants.collectAsState()
    var gardenPendingDelete by remember { mutableStateOf<GardenEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Ваши пространства", color = Mist)
                    Text("Мои сады", style = MaterialTheme.typography.headlineLarge, color = Cream)
                }
                PrimaryAction("+ Сад", { navController.navigate(AppDestinations.GARDEN_ADD) })
            }
        }
        if (gardens.isEmpty()) {
            item { EmptyGlassState("Создайте первый сад", "Объединяйте растения, процедуры и историю ухода") }
        }
        items(gardens, key = { it.id }) { garden ->
            val gardenPlants = plants.filter { it.gardenId == garden.id }
            GardenGlassCard(
                name = garden.name,
                plants = gardenPlants,
                onDelete = { gardenPendingDelete = garden },
                onExport = { exportGardenToFile(context, garden.name, gardenPlants) },
                onPlantOpen = { navController.navigate(AppDestinations.plantDetails(it)) },
                onAddPlant = { navController.navigate(AppDestinations.plantAdd(java.time.LocalDate.now().toString())) }
            )
        }
    }

    gardenPendingDelete?.let { garden ->
        DeleteConfirmationDialog(
            itemName = garden.name,
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
    name: String,
    plants: List<PlantEntity>,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onPlantOpen: (Int) -> Unit,
    onAddPlant: () -> Unit
) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("САД", color = Leaf300, style = MaterialTheme.typography.labelMedium)
                    Text(name, style = MaterialTheme.typography.headlineMedium, color = Cream)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Экспорт", color = Leaf300, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.clickable(onClick = onExport))
                    Text("Удалить", color = Danger, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.clickable(onClick = onDelete))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Metric(plants.size.toString(), "растений")
                Metric(plants.count { it.taskName.isNotBlank() }.toString(), "процедур")
                Metric(plants.map { it.drugId }.filterNotNull().distinct().size.toString(), "препаратов")
            }
            if (plants.isEmpty()) {
                Text("В этом саду пока нет растений", color = Mist)
            } else {
                plants.take(4).forEach { plant ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlantOpen(plant.id) }
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(plant.plantName, color = Cream, style = MaterialTheme.typography.titleMedium)
                            Text(plant.taskName, color = Mist, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("›", color = Leaf300, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
            PrimaryAction("Добавить процедуру", onAddPlant, Modifier.fillMaxWidth())
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
            plants.forEach { appendLine("${it.plantName} — ${it.taskName} — ${it.drugName}") }
            appendLine("Всего растений: ${plants.size}")
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
        Toast.makeText(context, "Ошибка экспорта: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
