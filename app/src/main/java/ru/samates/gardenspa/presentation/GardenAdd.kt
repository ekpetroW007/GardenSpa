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
import ru.samates.gardenspa.domain.ClimateFingerprint
import ru.samates.gardenspa.domain.GardenLocation
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Danger
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist
import ru.samates.gardenspa.viewmodel.GardensViewmodel
import ru.samates.gardenspa.viewmodel.GardensViewmodelFactory

@Composable
fun GardenAdd(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as BookeeperApp
    val viewModel: GardensViewmodel = viewModel(factory = GardensViewmodelFactory(app.repository))
    val resolver = remember(context) { AndroidGardenLocationResolver(context) }
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var locality by remember { mutableStateOf("") }
    var location by remember { mutableStateOf<GardenLocation?>(null) }
    var climate by remember { mutableStateOf<ClimateFingerprint?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun calculate(resolvedLocation: suspend () -> GardenLocation) {
        loading = true
        error = null
        scope.launch {
            runCatching {
                val point = resolvedLocation()
                point to app.climateService.calculateFingerprint(point)
            }.onSuccess { (point, fingerprint) ->
                location = point
                climate = fingerprint
                locality = point.localityName
            }.onFailure {
                error = it.message ?: "Не удалось определить условия сада. Можно указать место позже."
            }
            loading = false
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) calculate { resolver.resolveApproximateDeviceLocation() }
        else error = "Доступ к местоположению не предоставлен. Введите город или посёлок."
    }

    BotanicalBackground {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Новый сад", "Шаг 1 из 2: название и место", onBack = { navController.popBackStack() })
            Column(
                modifier = Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Как назвать сад?", style = MaterialTheme.typography.titleLarge, color = Cream)
                        Text("Например: Дача, Теплица или Сад у дома", color = Mist)
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Название сада") },
                            keyboardOptions = SentenceKeyboardOptions,
                            singleLine = true,
                            colors = glassTextFieldColors(),
                            shape = CompactGlassShape,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                GlassCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Где находится сад?", style = MaterialTheme.typography.titleLarge, color = Cream)
                        Text("Это поможет подобрать подходящие сроки ухода. Точные координаты вводить не нужно.", color = Mist)
                        SecondaryAction(
                            "Определить автоматически",
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                    calculate { resolver.resolveApproximateDeviceLocation() }
                                } else {
                                    locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !loading
                        )
                        OutlinedTextField(
                            value = locality,
                            onValueChange = {
                                locality = it
                                if (it != location?.localityName) {
                                    location = null
                                    climate = null
                                }
                            },
                            label = { Text("Город или посёлок") },
                            keyboardOptions = SentenceKeyboardOptions,
                            singleLine = true,
                            colors = glassTextFieldColors(),
                            shape = CompactGlassShape,
                            modifier = Modifier.fillMaxWidth()
                        )
                        SecondaryAction(
                            "Найти условия сада",
                            onClick = { calculate { resolver.resolvePlace(locality) } },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = locality.isNotBlank() && !loading
                        )
                        if (loading) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = Leaf300)
                                Text("Смотрим погоду прошлых лет…", color = Mist)
                            }
                        }
                        if (location != null && climate != null) {
                            Text("Место найдено: ${location?.localityName}", color = Leaf300)
                            Text("Условия: ${climate?.displayName()}", color = Cream)
                        }
                        error?.let { Text(it, color = Danger) }
                        Text("Можно создать сад сейчас и указать место позже.", color = Mist)
                    }
                }

                PrimaryAction(
                    text = if (climate == null) "Создать сад без настройки места" else "Создать сад",
                    enabled = name.isNotBlank() && !loading,
                    onClick = {
                        viewModel.gardenAdd(name.trim(), location, climate) { navController.popBackStack() }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                )
            }
        }
    }
}
