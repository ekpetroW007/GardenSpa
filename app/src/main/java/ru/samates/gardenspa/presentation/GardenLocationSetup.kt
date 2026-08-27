package ru.samates.gardenspa.presentation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import ru.samates.gardenspa.BookeeperApp
import ru.samates.gardenspa.data.climate.AndroidGardenLocationResolver
import ru.samates.gardenspa.data.database.entity.climateOrNull
import ru.samates.gardenspa.data.database.entity.locationOrNull
import ru.samates.gardenspa.domain.ClimateFingerprint
import ru.samates.gardenspa.domain.GardenLocation
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Danger
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist
import ru.samates.gardenspa.viewmodel.GardensViewmodel
import ru.samates.gardenspa.viewmodel.GardensViewmodelFactory

@Composable
fun GardenLocationSetup(navController: NavController, gardenId: Int) {
    val context = LocalContext.current
    val app = context.applicationContext as BookeeperApp
    val gardensVm: GardensViewmodel = viewModel(factory = GardensViewmodelFactory(app.repository))
    val gardens by gardensVm.gardens.collectAsState()
    val garden = gardens.firstOrNull { it.id == gardenId }
    val resolver = remember(context) { AndroidGardenLocationResolver(context) }
    val scope = rememberCoroutineScope()
    var locality by remember(garden?.id) { mutableStateOf(garden?.locationName.orEmpty()) }
    var resolvedLocation by remember(garden?.id) { mutableStateOf(garden?.locationOrNull()) }
    var climate by remember(garden?.id) { mutableStateOf(garden?.climateOrNull()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun calculate(resolve: suspend () -> GardenLocation) {
        loading = true
        error = null
        scope.launch {
            runCatching {
                val point = resolve()
                point to app.climateService.calculateFingerprint(point)
            }.onSuccess { (point, fingerprint) ->
                resolvedLocation = point
                climate = fingerprint
                locality = point.localityName
            }.onFailure { error = it.message ?: "Не удалось обновить условия сада" }
            loading = false
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) calculate { resolver.resolveApproximateDeviceLocation() }
        else error = "Введите город или посёлок — доступ к местоположению не предоставлен."
    }

    BotanicalBackground {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Место сада", garden?.name, onBack = { navController.popBackStack() })
            Column(
                Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (garden == null) {
                    EmptyGlassState("Сад не найден", "Вернитесь назад и выберите сад ещё раз")
                } else {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Укажите место", color = Cream, style = MaterialTheme.typography.titleLarge)
                            Text("GardenSpa использует его только для подбора сроков ухода.", color = Mist)
                            SecondaryAction(
                                "Определить автоматически",
                                onClick = {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                        calculate { resolver.resolveApproximateDeviceLocation() }
                                    } else launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !loading
                            )
                            OutlinedTextField(
                                value = locality,
                                onValueChange = { locality = it },
                                label = { Text("Город или посёлок") },
                                keyboardOptions = SentenceKeyboardOptions,
                                singleLine = true,
                                colors = glassTextFieldColors(),
                                shape = CompactGlassShape,
                                modifier = Modifier.fillMaxWidth()
                            )
                            SecondaryAction(
                                "Найти",
                                onClick = { calculate { resolver.resolvePlace(locality) } },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = locality.isNotBlank() && !loading
                            )
                            if (loading) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(color = Leaf300)
                                    Text("Обновляем условия…", color = Mist)
                                }
                            }
                            if (resolvedLocation != null && climate != null) {
                                Text("${resolvedLocation?.localityName}: ${climate?.displayName()}", color = Leaf300)
                            }
                            error?.let { Text(it, color = Danger) }
                        }
                    }
                    PrimaryAction(
                        "Сохранить место сада",
                        onClick = {
                            val point = resolvedLocation ?: return@PrimaryAction
                            val fingerprint = climate ?: return@PrimaryAction
                            gardensVm.updateClimate(garden, point, fingerprint) { navController.popBackStack() }
                        },
                        enabled = resolvedLocation != null && climate != null && !loading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
