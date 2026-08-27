package ru.samates.gardenspa.presentation

import android.content.Intent
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.net.Uri
import java.time.LocalDate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.rasterFadeDuration
import org.maplibre.android.style.layers.PropertyFactory.rasterOpacity
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import org.maplibre.android.tile.TileOperation
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point
import kotlinx.coroutines.CancellationException
import ru.samates.gardenspa.BookeeperApp
import ru.samates.gardenspa.BuildConfig
import ru.samates.gardenspa.R
import ru.samates.gardenspa.data.database.entity.GardenEntity
import ru.samates.gardenspa.data.database.entity.locationOrNull
import ru.samates.gardenspa.domain.ForecastWeatherDay
import ru.samates.gardenspa.domain.ScheduledTreatment
import ru.samates.gardenspa.domain.weatherWorkAdvice
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Forest950
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist

private const val OPEN_FREE_MAP_STYLE = "https://tiles.openfreemap.org/styles/positron"
private const val OPEN_WEATHER_SOURCE = "openweather-precipitation-source"
private const val OPEN_WEATHER_LAYER = "openweather-precipitation-layer"
private const val GARDEN_POINT_SOURCE = "garden-point-source"
private const val GARDEN_POINT_LAYER = "garden-point-layer"

private enum class MapLoadState {
    LOADING,
    BASE_READY,
    PRECIPITATION_READY,
    PRECIPITATION_ERROR,
    ERROR
}

internal fun selectPrecipitationGarden(
    gardens: List<GardenEntity>,
    preferredGardenIds: List<Int>
): GardenEntity? {
    val gardensById = gardens.associateBy { it.id }
    return preferredGardenIds.firstNotNullOfOrNull(gardensById::get)
        ?: gardens.filter { it.latitude != null && it.longitude != null }.minByOrNull { it.id }
}

internal fun openWeatherPrecipitationTileUrl(apiKey: String): String? =
    apiKey.trim().takeIf { it.isNotEmpty() }?.let { key ->
        "https://tile.openweathermap.org/map/precipitation_new/{z}/{x}/{y}.png?appid=$key"
    }

@Composable
fun PrecipitationMapCard(
    garden: GardenEntity?,
    scheduledTreatments: List<ScheduledTreatment> = emptyList(),
    onOpenGardens: () -> Unit
) {
    val location = garden?.locationOrNull()
    if (garden == null || location == null) {
        GlassCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Погода вокруг сада", color = Cream, style = MaterialTheme.typography.titleLarge)
                Text(
                    "Укажите место сада — карта автоматически откроется на нужном участке.",
                    color = Mist
                )
                SecondaryAction("Указать место сада", onOpenGardens, Modifier.fillMaxWidth())
            }
        }
        return
    }

    val context = LocalContext.current
    val app = context.applicationContext as BookeeperApp
    val tileUrl = remember(BuildConfig.OPENWEATHER_API_KEY) {
        openWeatherPrecipitationTileUrl(BuildConfig.OPENWEATHER_API_KEY)
    }
    val relevantTreatments = remember(garden.id, scheduledTreatments) {
        scheduledTreatments.filter { it.plant.gardenId == garden.id }
    }
    var forecast by remember(garden.id, location.latitude, location.longitude) {
        mutableStateOf<List<ForecastWeatherDay>?>(null)
    }
    var forecastFailed by remember(garden.id, location.latitude, location.longitude) {
        mutableStateOf(false)
    }
    var mapLoadState by remember(garden.id, tileUrl) { mutableStateOf(MapLoadState.LOADING) }
    val mapView = remember(context, garden.id) {
        MapLibre.getInstance(context)
        MapView(
            context,
            MapLibreMapOptions.createFromAttributes(context).textureMode(true)
        )
    }
    var foregroundRefresh by remember(mapView) { mutableStateOf(0) }
    MapViewLifecycle(mapView) { foregroundRefresh += 1 }

    DisposableEffect(mapView) {
        val failureListener = MapView.OnDidFailLoadingMapListener {
            mapLoadState = MapLoadState.ERROR
        }
        val tileActionListener = MapView.OnTileActionListener {
                operation, _, _, _, _, _, sourceId ->
            if (sourceId == OPEN_WEATHER_SOURCE) {
                when (operation) {
                    TileOperation.LoadFromNetwork,
                    TileOperation.LoadFromCache,
                    TileOperation.EndParse -> mapLoadState = MapLoadState.PRECIPITATION_READY

                    TileOperation.Error -> if (mapLoadState != MapLoadState.PRECIPITATION_READY) {
                        mapLoadState = MapLoadState.PRECIPITATION_ERROR
                    }

                    else -> Unit
                }
            }
        }
        mapView.addOnDidFailLoadingMapListener(failureListener)
        mapView.addOnTileActionListener(tileActionListener)
        onDispose {
            mapView.removeOnDidFailLoadingMapListener(failureListener)
            mapView.removeOnTileActionListener(tileActionListener)
        }
    }

    LaunchedEffect(
        garden.id,
        location.latitude,
        location.longitude,
        foregroundRefresh
    ) {
        forecast = null
        forecastFailed = false
        try {
            val loadedForecast = app.climateService.loadForecast(location)
            if (loadedForecast.isEmpty()) forecastFailed = true else forecast = loadedForecast
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            forecastFailed = true
        }
    }

    LaunchedEffect(mapView, location.latitude, location.longitude, tileUrl, foregroundRefresh) {
        mapLoadState = MapLoadState.LOADING
        mapView.getMapAsync { map ->
            map.uiSettings.isCompassEnabled = false
            map.uiSettings.isRotateGesturesEnabled = false
            map.uiSettings.isTiltGesturesEnabled = false
            map.uiSettings.isScrollGesturesEnabled = false
            map.uiSettings.isZoomGesturesEnabled = false
            map.uiSettings.isDoubleTapGesturesEnabled = false
            map.uiSettings.isQuickZoomGesturesEnabled = false
            map.cameraPosition = CameraPosition.Builder()
                .target(LatLng(location.latitude, location.longitude))
                .zoom(7.5)
                .build()
            map.setStyle(Style.Builder().fromUri(OPEN_FREE_MAP_STYLE)) { style ->
                runCatching {
                    tileUrl?.let { url ->
                        val tileSet = TileSet("2.2.0", url).apply {
                            attribution = "Weather data provided by OpenWeather"
                            maxZoom = 10f
                        }
                        style.addSource(
                            RasterSource(
                                OPEN_WEATHER_SOURCE,
                                tileSet,
                                256
                            )
                        )
                        val precipitationLayer = RasterLayer(OPEN_WEATHER_LAYER, OPEN_WEATHER_SOURCE)
                            .withProperties(
                                rasterOpacity(0.90f),
                                rasterFadeDuration(0f)
                            )
                        style.addLayer(precipitationLayer)
                    }
                    style.addSource(
                        GeoJsonSource(
                            GARDEN_POINT_SOURCE,
                            Feature.fromGeometry(Point.fromLngLat(location.longitude, location.latitude))
                        )
                    )
                    style.addLayer(
                        CircleLayer(GARDEN_POINT_LAYER, GARDEN_POINT_SOURCE).withProperties(
                            circleRadius(8f),
                            circleColor("#4F9B68"),
                            circleStrokeColor("#FFFFFF"),
                            circleStrokeWidth(2f)
                        )
                    )
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(location.latitude, location.longitude))
                        .zoom(7.5)
                        .build()
                }.onSuccess {
                    if (mapLoadState != MapLoadState.ERROR) {
                        mapLoadState = if (tileUrl == null) {
                            MapLoadState.BASE_READY
                        } else {
                            MapLoadState.LOADING
                        }
                    }
                }.onFailure {
                    mapLoadState = MapLoadState.ERROR
                }
            }
        }
    }

    val forecastText = when {
        forecastFailed -> "Не удалось обновить прогноз. Перед работами проверьте погоду вручную."
        forecast == null -> "Проверяем прогноз для работ на сегодня и завтра…"
        else -> {
            val adviceMessages = weatherWorkAdvice(
                treatments = relevantTreatments,
                forecast = forecast.orEmpty(),
                today = LocalDate.now()
            )
                .map { it.message }
                .distinct()
            when {
                adviceMessages.isNotEmpty() -> buildString {
                    append(adviceMessages.take(2).joinToString("\n"))
                    if (adviceMessages.size > 2) {
                        append("\nЕщё предупреждений: ${adviceMessages.size - 2}.")
                    }
                }
                relevantTreatments.isEmpty() ->
                    "Прогноз загружен. На сегодня и завтра нет работ для погодной проверки."
                else ->
                    "По известным правилам программы превышений нет. Ручные работы проверьте отдельно."
            }
        }
    }

    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Погода вокруг сада", color = Cream, style = MaterialTheme.typography.titleLarge)
                Text("${garden.name} · ${location.localityName}", color = Leaf300)
                Text("Карта выбрана по ближайшей запланированной работе", color = Mist)
            }
            Text(
                forecastText,
                color = if (forecastFailed) Mist else Cream,
                style = MaterialTheme.typography.bodyMedium
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(CompactGlassShape)
                    .background(Forest950)
            ) {
                AndroidView(
                    factory = { mapView },
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics {
                            contentDescription = "Карта для сада ${garden.name}"
                        }
                )
                if (mapLoadState == MapLoadState.LOADING || mapLoadState == MapLoadState.ERROR) {
                    Box(
                        Modifier.fillMaxSize().background(Forest950.copy(alpha = 0.72f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (mapLoadState == MapLoadState.ERROR) {
                                "Карта или слой осадков временно недоступны."
                            } else {
                                "Загружаем карту…"
                            },
                            color = Mist,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            if (mapLoadState == MapLoadState.ERROR) {
                Text(
                    "Карта временно недоступна. Попробуйте открыть экран позже.",
                    color = Mist
                )
            } else if (mapLoadState == MapLoadState.PRECIPITATION_ERROR) {
                Text(
                    "OpenWeather не отдал слой осадков; обычная карта остаётся доступной.",
                    color = Mist
                )
            } else if (tileUrl == null) {
                Text(
                    if (BuildConfig.DEBUG) {
                        "Карта готова. Добавьте OPENWEATHER_API_KEY в local.properties, чтобы включить слой осадков."
                    } else {
                        "Слой осадков временно недоступен."
                    },
                    color = Mist
                )
            } else if (mapLoadState == MapLoadState.PRECIPITATION_READY) {
                Text(
                    "Тёмно-синие области с тучами показывают осадки; вне них осадков сейчас нет.",
                    color = Mist
                )
            } else {
                Text("Загружаем слой осадков OpenWeather…", color = Mist)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AttributionLink(
                    context = context,
                    text = "Weather data provided by OpenWeather",
                    url = "https://openweathermap.org/",
                    modifier = Modifier.weight(1f)
                )
                Image(
                    painter = painterResource(R.drawable.openweather_attribution),
                    contentDescription = "OpenWeather",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clickable {
                            openExternalUrl(context, "https://openweathermap.org/")
                        }
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AttributionLink(context, "OpenFreeMap", "https://openfreemap.org/", Modifier.weight(1f))
                AttributionLink(context, "© OpenMapTiles", "https://openmaptiles.org/", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AttributionLink(
                    context,
                    "© OpenStreetMap",
                    "https://www.openstreetmap.org/copyright",
                    Modifier.weight(1f)
                )
                AttributionLink(context, "Прогноз: Open-Meteo", "https://open-meteo.com/", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MapViewLifecycle(
    mapView: MapView,
    onForegroundReturn: () -> Unit
) {
    val owner = LocalLifecycleOwner.current
    val appContext = LocalContext.current.applicationContext
    val latestOnForegroundReturn by rememberUpdatedState(onForegroundReturn)
    DisposableEffect(owner, mapView, appContext) {
        var destroyed = false
        var resumedOnce = false
        val memoryCallbacks = object : ComponentCallbacks2 {
            override fun onConfigurationChanged(newConfig: Configuration) = Unit

            override fun onLowMemory() {
                if (!destroyed) mapView.onLowMemory()
            }

            override fun onTrimMemory(level: Int) {
                if (!destroyed) mapView.onLowMemory()
            }
        }
        val observer = object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) = mapView.onCreate(null)
            override fun onStart(owner: LifecycleOwner) = mapView.onStart()
            override fun onResume(owner: LifecycleOwner) {
                mapView.onResume()
                if (resumedOnce) latestOnForegroundReturn() else resumedOnce = true
            }
            override fun onPause(owner: LifecycleOwner) = mapView.onPause()
            override fun onStop(owner: LifecycleOwner) = mapView.onStop()
            override fun onDestroy(owner: LifecycleOwner) {
                mapView.onDestroy()
                destroyed = true
            }
        }
        appContext.registerComponentCallbacks(memoryCallbacks)
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            appContext.unregisterComponentCallbacks(memoryCallbacks)
            if (!destroyed) {
                when {
                    owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) -> {
                        mapView.onPause()
                        mapView.onStop()
                    }
                    owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) -> mapView.onStop()
                }
                mapView.onDestroy()
            }
        }
    }
}

private fun openExternalUrl(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

@Composable
private fun AttributionLink(
    context: android.content.Context,
    text: String,
    url: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = Leaf300,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier.clickable { openExternalUrl(context, url) }
    )
}
