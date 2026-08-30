package ru.samates.gardenspa.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import javax.net.ssl.SSLException
import kotlinx.coroutines.CancellationException
import ru.samates.gardenspa.BookeeperApp
import ru.samates.gardenspa.data.database.entity.GardenEntity
import ru.samates.gardenspa.data.database.entity.locationOrNull
import ru.samates.gardenspa.domain.GardenWeatherForecast
import ru.samates.gardenspa.domain.HourlyGardenWeather
import ru.samates.gardenspa.domain.ScheduledTreatment
import ru.samates.gardenspa.domain.WeatherWorkAdvice
import ru.samates.gardenspa.domain.findWeatherWindow
import ru.samates.gardenspa.domain.suggestedWeatherSafeDate
import ru.samates.gardenspa.domain.weatherLimits
import ru.samates.gardenspa.domain.weatherWorkAdvice
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Forest700
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist

internal fun selectWeatherGarden(
    gardens: List<GardenEntity>,
    preferredGardenIds: List<Int>
): GardenEntity? {
    val gardensById = gardens.associateBy { it.id }
    return preferredGardenIds.firstNotNullOfOrNull(gardensById::get)
        ?: gardens.filter { it.latitude != null && it.longitude != null }.minByOrNull { it.id }
}

@Composable
fun WeatherWindowCard(
    garden: GardenEntity?,
    treatment: ScheduledTreatment?,
    onOpenGardens: () -> Unit,
    onReschedule: (ScheduledTreatment, LocalDate) -> Unit
) {
    val location = garden?.locationOrNull()
    if (garden == null || location == null) {
        GlassCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Погодное окно", color = Cream, style = MaterialTheme.typography.titleLarge)
                Text(
                    if (garden == null) {
                        "Добавьте сад и его местоположение, чтобы получать погодные рекомендации."
                    } else {
                        "Укажите местоположение сада «${garden.name}» — именно в нём запланирована ближайшая работа."
                    },
                    color = Mist
                )
                SecondaryAction("Указать место сада", onOpenGardens, Modifier.fillMaxWidth())
            }
        }
        return
    }

    val context = LocalContext.current
    val app = context.applicationContext as BookeeperApp
    var refreshToken by remember { mutableIntStateOf(0) }
    var weather by remember(garden.id, location.latitude, location.longitude) {
        mutableStateOf<GardenWeatherForecast?>(null)
    }
    var loadingError by remember(garden.id, location.latitude, location.longitude) {
        mutableStateOf<String?>(null)
    }
    var dismissedTreatmentKey by remember { mutableStateOf<String?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refreshToken++ }
    LaunchedEffect(garden.id, location.latitude, location.longitude, refreshToken) {
        loadingError = null
        try {
            weather = app.climateService.loadGardenWeather(location)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            loadingError = weatherFailureMessage(error)
        }
    }

    val advice = weather?.let { loaded ->
        treatment?.let {
            weatherWorkAdvice(listOf(it), loaded.daily, loaded.localTime.toLocalDate()).firstOrNull()
        }
    }
    val window = weather?.let { loaded ->
        treatment?.let { findWeatherWindow(it, loaded.hourly, loaded.localTime) }
    }
    val currentRain = weather?.current?.let { current ->
        current.precipitationMm > 0.0 || "дожд" in current.conditionText.lowercase(RUSSIAN_LOCALE)
    } == true
    val suggestedDate = treatment?.let { work ->
        weather?.let { loaded ->
            suggestedWeatherSafeDate(work, loaded.daily)
                ?: window?.start?.toLocalDate()?.takeIf { it.isAfter(work.scheduledDate) }
        }
    }
    val treatmentKey = treatment?.let { "${it.plant.id}:${it.originalDate}" }
    val offerReschedule = treatment != null &&
        suggestedDate != null &&
        (advice != null || (treatment.scheduledDate == weather?.localTime?.toLocalDate() && currentRain)) &&
        dismissedTreatmentKey != treatmentKey

    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Погодное окно", color = Cream, style = MaterialTheme.typography.titleLarge)
                Text("${garden.name} · ${location.localityName}", color = Leaf300)
                treatment?.let {
                    Text(
                        "${it.plant.taskName} · ${it.scheduledDate.toRussianDate(includeYear = false)}",
                        color = Mist
                    )
                }
            }

            Text(
                weatherMessage(treatment, weather, advice, window, currentRain, loadingError),
                color = Cream,
                style = MaterialTheme.typography.bodyMedium
            )

            if (loadingError != null) {
                SecondaryAction(
                    text = "Повторить",
                    onClick = { refreshToken++ },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            weather?.let { loaded ->
                val nextHours = loaded.hourly
                    .filter { !it.time.isBefore(loaded.localTime.withMinute(0).withSecond(0).withNano(0)) }
                    .take(4)
                if (nextHours.isNotEmpty()) {
                    Text("Ближайшие четыре часа", color = Mist, style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        nextHours.forEach { hour -> HourForecast(hour, Modifier.weight(1f)) }
                    }
                }
            }

            if (offerReschedule) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryAction(
                        text = "Перенести на ${suggestedDate!!.toRussianDate(includeYear = false)}",
                        onClick = { onReschedule(requireNotNull(treatment), suggestedDate) },
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryAction(
                        text = "Оставить как есть",
                        onClick = { dismissedTreatmentKey = treatmentKey },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            weather?.let { loaded ->
                Text(
                    "Обновлено ${loaded.current.observedAt.format(HOUR_FORMAT)} · Данные о погоде: WeatherAPI.com",
                    color = Leaf300,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.weatherapi.com/")))
                    }
                )
            }
            Text(
                "Прогноз вероятностный и может отличаться для конкретного участка. Для решений, связанных с безопасностью, проверяйте официальные предупреждения.",
                color = Mist,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun HourForecast(hour: HourlyGardenWeather, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Forest700, RoundedCornerShape(14.dp))
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(hour.time.format(HOUR_FORMAT), color = Cream, fontWeight = FontWeight.SemiBold)
        Text(hour.shortCondition(), color = Mist, style = MaterialTheme.typography.labelSmall)
    }
}

private fun weatherMessage(
    treatment: ScheduledTreatment?,
    weather: GardenWeatherForecast?,
    advice: WeatherWorkAdvice?,
    window: ru.samates.gardenspa.domain.WeatherWindow?,
    currentRain: Boolean,
    loadingError: String?
): String {
    if (loadingError != null) return loadingError
    if (weather == null) return "Проверяем осадки, ветер и температуру для ближайшей работы…"
    if (treatment == null) {
        return "Ближайших работ нет. Сейчас: ${weather.current.conditionText.lowercase()}, ${weather.current.temperatureC.roundWeather()} °C."
    }
    val lastForecastDate = weather.daily.maxOfOrNull { it.date }
    if (lastForecastDate != null && treatment.scheduledDate.isAfter(lastForecastDate)) {
        return "Ближайшая работа — «${treatment.plant.taskName}». Подробная рекомендация появится за три дня до неё."
    }
    if (treatment.weatherLimits() == null) {
        return "Для этой работы погодные ограничения не заданы. Сейчас: ${weather.current.conditionText.lowercase()}, ${weather.current.temperatureC.roundWeather()} °C."
    }

    val base = when {
        treatment.scheduledDate == weather.localTime.toLocalDate() && currentRain ->
            "Сейчас проводить «${treatment.plant.taskName}» не рекомендуется: в районе сада идут осадки."
        advice != null -> advice.message
        else -> "По прогнозу условия для работы «${treatment.plant.taskName}» подходят."
    }
    return window?.let { "$base\nБлижайшее сухое окно: ${it.windowText(weather.localTime.toLocalDate())}." }
        ?: "$base\nПодходящее окно в пределах трёхдневного прогноза не найдено."
}

internal fun weatherFailureMessage(error: Throwable): String {
    val causes = generateSequence(error) { it.cause }.toList()
    val statusCode = causes.asSequence()
        .mapNotNull { cause -> Regex("WeatherAPI вернул ошибку (\\d+)").find(cause.message.orEmpty()) }
        .map { match -> match.groupValues[1] }
        .firstOrNull()
    return when {
        causes.any { "WeatherAPI не настроен" in it.message.orEmpty() } ->
            "В этой сборке нет ключа WeatherAPI. Установите новый APK поверх текущего."
        causes.any { it is UnknownHostException } ->
            "Телефон не может найти api.weatherapi.com. Проверьте Private DNS, VPN или другую сеть."
        causes.any { it is SocketTimeoutException } ->
            "WeatherAPI не ответил вовремя. Повторите в другой сети."
        causes.any { it is SSLException } ->
            "Не удалось установить защищённое соединение с WeatherAPI. Проверьте дату и время телефона."
        statusCode == "401" || statusCode == "403" ->
            "WeatherAPI отклонил ключ (код $statusCode)."
        statusCode != null -> "WeatherAPI вернул ошибку $statusCode."
        causes.any { it is DateTimeParseException || it.javaClass.simpleName == "JSONException" } ->
            "WeatherAPI вернул данные в неожиданном формате."
        else -> "Не удалось обновить прогноз (${error.javaClass.simpleName})."
    }
}

private fun HourlyGardenWeather.shortCondition(): String = when {
    precipitationMm > 0.1 || chanceOfRainPercent >= 60 -> "дождь $chanceOfRainPercent%"
    chanceOfRainPercent >= 30 -> "возможен дождь"
    else -> "сухо"
}

private fun ru.samates.gardenspa.domain.WeatherWindow.windowText(today: LocalDate): String {
    val day = when (start.toLocalDate()) {
        today -> "сегодня"
        today.plusDays(1) -> "завтра"
        else -> start.toLocalDate().toRussianDate(includeYear = false)
    }
    return "$day с ${start.format(HOUR_FORMAT)} до ${endExclusive.format(HOUR_FORMAT)}"
}

private fun Double.roundWeather(): String = String.format(RUSSIAN_LOCALE, "%.0f", this)

private val HOUR_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val RUSSIAN_LOCALE: Locale = Locale.forLanguageTag("ru")
