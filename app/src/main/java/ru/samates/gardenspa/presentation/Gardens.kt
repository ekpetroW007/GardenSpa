package ru.samates.gardenspa.presentation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import ru.samates.gardenspa.BookeeperApp
import ru.samates.gardenspa.data.climate.AndroidGardenLocationResolver
import ru.samates.gardenspa.data.database.entity.GardenEntity
import ru.samates.gardenspa.domain.GardenClimate
import ru.samates.gardenspa.domain.GardenLocation
import ru.samates.gardenspa.domain.PlantCard
import ru.samates.gardenspa.domain.decodeGardenClimate
import ru.samates.gardenspa.domain.encode
import ru.samates.gardenspa.domain.toPlantCards
import ru.samates.gardenspa.presentation.navigation.AppDestinations
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Danger
import ru.samates.gardenspa.ui.theme.Forest950
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist
import ru.samates.gardenspa.viewmodel.GardensViewmodel
import ru.samates.gardenspa.viewmodel.GardensViewmodelFactory
import ru.samates.gardenspa.viewmodel.PlantsViewmodel
import ru.samates.gardenspa.viewmodel.PlantsViewmodelFactory

@Composable
fun MyGardens(navController: NavController, innerPadding: PaddingValues) {
    val context = LocalContext.current
    val app = context.applicationContext as BookeeperApp
    val gardensVm: GardensViewmodel = viewModel(factory = GardensViewmodelFactory(app.repository))
    val plantsVm: PlantsViewmodel = viewModel(factory = PlantsViewmodelFactory(app.repository))
    val gardens by gardensVm.gardens.collectAsState()
    val plants by plantsVm.plants.collectAsState()
    val resolver = remember(context) { AndroidGardenLocationResolver(context) }
    val scope = rememberCoroutineScope()
    var gardenPendingDelete by remember { mutableStateOf<GardenEntity?>(null) }
    var climateGarden by remember { mutableStateOf<GardenEntity?>(null) }
    var climateLoading by remember { mutableStateOf(false) }
    var climateError by remember { mutableStateOf<String?>(null) }

    val saveClimate: (GardenEntity, GardenLocation) -> Unit = { garden, location ->
        climateLoading = true
        climateError = null
        scope.launch {
            runCatching { app.climateService.calculateFingerprint(location) }
                .onSuccess { fingerprint ->
                    gardensVm.updateClimate(garden.id, GardenClimate(location, fingerprint).encode()) {
                        climateLoading = false
                        climateGarden = null
                    }
                }
                .onFailure { error ->
                    climateLoading = false
                    climateError = error.message ?: "Не удалось рассчитать климат"
                }
        }
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val garden = climateGarden ?: return@rememberLauncherForActivityResult
        if (!granted) {
            climateError = "Разрешение не выдано. Введите населённый пункт."
        } else {
            scope.launch {
                runCatching { resolver.resolveApproximateDeviceLocation() }
                    .onSuccess { saveClimate(garden, it) }
                    .onFailure { climateError = it.message ?: "Не удалось определить местоположение" }
            }
        }
    }

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
        if (gardens.isEmpty()) item { EmptyGlassState("Создайте первый сад", "Объединяйте растения, процедуры и историю ухода") }
        items(gardens, key = { it.id }) { garden ->
            val gardenPlantRows = plants.filter { it.gardenId == garden.id }
            GardenGlassCard(
                garden = garden,
                plantCards = gardenPlantRows.toPlantCards(),
                onDelete = { gardenPendingDelete = garden },
                onClimate = { climateError = null; climateGarden = garden },
                onPlantOpen = { navController.navigate(AppDestinations.plantDetails(it)) },
                onAddPlant = { navController.navigate(AppDestinations.plantAdd(java.time.LocalDate.now().toString(), garden.id)) }
            )
        }
    }

    gardenPendingDelete?.let { garden ->
        DeleteConfirmationDialog(garden.name, onConfirm = { gardensVm.deleteGarden(garden.id); gardenPendingDelete = null }, onDismiss = { gardenPendingDelete = null })
    }
    climateGarden?.let { garden ->
        val currentClimate = garden.climateData.decodeGardenClimate()
        ClimateSetupDialog(
            gardenName = garden.name,
            existingLocation = currentClimate?.location,
            loading = climateLoading,
            error = climateError,
            onDismiss = { if (!climateLoading) climateGarden = null },
            onUseApproximateLocation = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    scope.launch {
                        runCatching { resolver.resolveApproximateDeviceLocation() }
                            .onSuccess { saveClimate(garden, it) }
                            .onFailure { climateError = it.message ?: "Не удалось определить местоположение" }
                    }
                } else {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            },
            onCalculate = { name ->
                scope.launch {
                    runCatching { resolver.resolvePlace(name) }
                        .onSuccess { saveClimate(garden, it) }
                        .onFailure { climateError = it.message ?: "Не удалось определить точку" }
                }
            }
        )
    }
}

@Composable
private fun GardenGlassCard(garden: GardenEntity, plantCards: List<PlantCard>, onDelete: () -> Unit, onClimate: () -> Unit, onPlantOpen: (Int) -> Unit, onAddPlant: () -> Unit) {
    val climate = garden.climateData.decodeGardenClimate()
    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("САД", color = Leaf300, style = MaterialTheme.typography.labelMedium)
                    Text(garden.name, style = MaterialTheme.typography.headlineMedium, color = Cream)
                    Text(climate?.location?.localityName ?: "Климат не настроен", color = Mist, style = MaterialTheme.typography.bodyLarge)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (climate == null) Text("Климат", color = Leaf300, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.clickable(onClick = onClimate))
                    Text("Удалить", color = Danger, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.clickable(onClick = onDelete))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Metric(plantCards.size.toString(), "растений")
                Metric(plantCards.sumOf { it.procedures.size }.toString(), "процедур")
                Metric(plantCards.flatMap { it.procedures }.mapNotNull { it.drugId }.distinct().size.toString(), "препаратов")
            }
            if (plantCards.isEmpty()) {
                Text("В этом саду пока нет растений", color = Mist)
            } else {
                plantCards.take(4).forEach { card ->
                    val plant = card.primary
                    Row(Modifier.fillMaxWidth().clickable { onPlantOpen(plant.id) }.padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(plant.plantName, color = Cream, style = MaterialTheme.typography.titleMedium)
                            if (plant.plantDetails.isNotBlank()) Text(plant.plantDetails, color = Leaf300, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            Text(card.procedures.joinToString(" · ") { it.taskName }, color = Mist, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                        }
                        Text("›", color = Leaf300, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
            PrimaryAction("Добавить растение", onAddPlant, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ClimateSetupDialog(gardenName: String, existingLocation: GardenLocation?, loading: Boolean, error: String?, onDismiss: () -> Unit, onUseApproximateLocation: () -> Unit, onCalculate: (String) -> Unit) {
    var locality by remember(existingLocation) { mutableStateOf(existingLocation?.localityName.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Forest950,
        titleContentColor = Cream,
        textContentColor = Cream,
        title = { Text("Климат сада «$gardenName»") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Определите местоположение приблизительно или найдите населённый пункт.", color = Mist)
                SecondaryAction("Определить приблизительно", onUseApproximateLocation, Modifier.fillMaxWidth(), enabled = !loading)
                OutlinedTextField(locality, { locality = it }, label = { Text("Населённый пункт") }, singleLine = true, colors = glassTextFieldColors(), shape = CompactGlassShape, modifier = Modifier.fillMaxWidth())
                if (loading) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(color = Leaf300)
                    Text("Анализируем многолетнюю погоду…", color = Mist)
                }
                error?.let { Text(it, color = Danger) }
            }
        },
        confirmButton = { TextButton(onClick = { onCalculate(locality) }, enabled = !loading && locality.isNotBlank()) { Text("Рассчитать", color = Leaf300) } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !loading) { Text("Отмена", color = Mist) } }
    )
}
