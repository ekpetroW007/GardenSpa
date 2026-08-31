package ru.samates.gardenspa.data.climate

import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import ru.samates.gardenspa.domain.ClimateFingerprint
import ru.samates.gardenspa.domain.ClimateFingerprintCalculator
import ru.samates.gardenspa.domain.CurrentGardenWeather
import ru.samates.gardenspa.domain.ForecastWeatherDay
import ru.samates.gardenspa.domain.GardenLocation
import ru.samates.gardenspa.domain.GardenWeatherForecast
import ru.samates.gardenspa.domain.HistoricalWeatherDay
import ru.samates.gardenspa.domain.HourlyGardenWeather

class ClimateService(
    private val client: OpenMeteoClimateClient = OpenMeteoClimateClient(),
    private val calculator: ClimateFingerprintCalculator = ClimateFingerprintCalculator()
) {
    suspend fun calculateFingerprint(location: GardenLocation): ClimateFingerprint =
        calculator.calculate(client.loadHistoricalWeather(location))

    suspend fun loadForecast(location: GardenLocation): List<ForecastWeatherDay> =
        client.loadForecast(location)

    suspend fun loadGardenWeather(location: GardenLocation): GardenWeatherForecast =
        client.loadGardenWeather(location)

}

class OpenMeteoClimateClient {
    private val gardenWeatherCache = mutableMapOf<String, CachedGardenWeather>()

    suspend fun loadHistoricalWeather(
        location: GardenLocation,
        years: Int = 20,
        today: LocalDate = LocalDate.now()
    ): List<HistoricalWeatherDay> = withContext(Dispatchers.IO) {
        val endYear = today.year - 1
        val startYear = endYear - years + 1
        val url = buildString {
            append("https://archive-api.open-meteo.com/v1/archive")
            append("?latitude=${coordinate(location.latitude)}")
            append("&longitude=${coordinate(location.longitude)}")
            append("&start_date=$startYear-01-01")
            append("&end_date=$endYear-12-31")
            append("&daily=temperature_2m_min,temperature_2m_max,precipitation_sum")
            append("&timezone=auto")
            location.elevationMeters?.let { append("&elevation=$it") }
        }
        val daily = requestJson(url).getJSONObject("daily")
        val dates = daily.getJSONArray("time")
        val minimums = daily.getJSONArray("temperature_2m_min")
        val maximums = daily.getJSONArray("temperature_2m_max")
        val precipitation = daily.getJSONArray("precipitation_sum")

        buildList {
            for (index in 0 until dates.length()) {
                if (minimums.isNull(index) || maximums.isNull(index)) continue
                add(
                    HistoricalWeatherDay(
                        date = LocalDate.parse(dates.getString(index)),
                        minimumTemperatureC = minimums.getDouble(index),
                        maximumTemperatureC = maximums.getDouble(index),
                        precipitationMm = precipitation.optDouble(index, 0.0).takeUnless(Double::isNaN) ?: 0.0
                    )
                )
            }
        }
    }

    suspend fun loadForecast(location: GardenLocation): List<ForecastWeatherDay> =
        withContext(Dispatchers.IO) {
            val url = buildString {
                append("https://api.open-meteo.com/v1/forecast")
                append("?latitude=${coordinate(location.latitude)}")
                append("&longitude=${coordinate(location.longitude)}")
                append("&daily=temperature_2m_min,temperature_2m_max,precipitation_sum,wind_speed_10m_max")
                append("&forecast_days=16&timezone=auto&wind_speed_unit=ms")
                location.elevationMeters?.let { append("&elevation=$it") }
            }
            val daily = requestJson(url).getJSONObject("daily")
            val dates = daily.getJSONArray("time")
            val minimums = daily.getJSONArray("temperature_2m_min")
            val maximums = daily.getJSONArray("temperature_2m_max")
            val precipitation = daily.getJSONArray("precipitation_sum")
            val wind = daily.getJSONArray("wind_speed_10m_max")

            buildList {
                for (index in 0 until dates.length()) {
                    if (minimums.isNull(index) || maximums.isNull(index)) continue
                    add(
                        ForecastWeatherDay(
                            date = LocalDate.parse(dates.getString(index)),
                            minimumTemperatureC = minimums.getDouble(index),
                            maximumTemperatureC = maximums.getDouble(index),
                            precipitationMm = precipitation.optDouble(index, 0.0).takeUnless(Double::isNaN) ?: 0.0,
                            maximumWindMetersPerSecond = wind.optDouble(index, 0.0).takeUnless(Double::isNaN) ?: 0.0
                        )
                    )
                }
            }
        }

    suspend fun loadGardenWeather(location: GardenLocation): GardenWeatherForecast =
        withContext(Dispatchers.IO) {
            val cacheKey = "${coordinate(location.latitude)},${coordinate(location.longitude)}"
            synchronized(gardenWeatherCache) {
                gardenWeatherCache[cacheKey]
                    ?.takeIf { System.currentTimeMillis() - it.loadedAtMillis < CACHE_MILLIS }
                    ?.forecast
            }?.let { return@withContext it }

            val url = buildString {
                append("https://api.open-meteo.com/v1/forecast")
                append("?latitude=${coordinate(location.latitude)}")
                append("&longitude=${coordinate(location.longitude)}")
                append("&current=temperature_2m,precipitation,weather_code,wind_speed_10m")
                append("&hourly=temperature_2m,precipitation,precipitation_probability,weather_code,wind_speed_10m")
                append("&daily=temperature_2m_min,temperature_2m_max,precipitation_sum,wind_speed_10m_max")
                append("&forecast_days=3&timezone=auto&wind_speed_unit=ms")
                location.elevationMeters?.let { append("&elevation=$it") }
            }
            val forecast = parseGardenWeather(requestJson(url))
            synchronized(gardenWeatherCache) {
                gardenWeatherCache[cacheKey] = CachedGardenWeather(System.currentTimeMillis(), forecast)
            }
            forecast
        }

    internal fun parseGardenWeather(root: JSONObject): GardenWeatherForecast {
        val current = root.getJSONObject("current")
        val hourly = root.getJSONObject("hourly")
        val daily = root.getJSONObject("daily")

        val hourlyTimes = hourly.getJSONArray("time")
        val hourlyTemperatures = hourly.getJSONArray("temperature_2m")
        val hourlyPrecipitation = hourly.getJSONArray("precipitation")
        val hourlyRainChance = hourly.getJSONArray("precipitation_probability")
        val hourlyWeatherCodes = hourly.getJSONArray("weather_code")
        val hourlyWind = hourly.getJSONArray("wind_speed_10m")
        val hours = buildList {
            for (index in 0 until hourlyTimes.length()) {
                add(
                    HourlyGardenWeather(
                        time = LocalDateTime.parse(hourlyTimes.getString(index)),
                        temperatureC = hourlyTemperatures.getDouble(index),
                        precipitationMm = hourlyPrecipitation.safeDouble(index),
                        chanceOfRainPercent = hourlyRainChance.optInt(index, 0),
                        windMetersPerSecond = hourlyWind.safeDouble(index),
                        conditionText = weatherCodeText(hourlyWeatherCodes.optInt(index, -1))
                    )
                )
            }
        }

        val dailyTimes = daily.getJSONArray("time")
        val dailyMinimums = daily.getJSONArray("temperature_2m_min")
        val dailyMaximums = daily.getJSONArray("temperature_2m_max")
        val dailyPrecipitation = daily.getJSONArray("precipitation_sum")
        val dailyWind = daily.getJSONArray("wind_speed_10m_max")
        val days = buildList {
            for (index in 0 until dailyTimes.length()) {
                if (dailyMinimums.isNull(index) || dailyMaximums.isNull(index)) continue
                add(
                    ForecastWeatherDay(
                        date = LocalDate.parse(dailyTimes.getString(index)),
                        minimumTemperatureC = dailyMinimums.getDouble(index),
                        maximumTemperatureC = dailyMaximums.getDouble(index),
                        precipitationMm = dailyPrecipitation.safeDouble(index),
                        maximumWindMetersPerSecond = dailyWind.safeDouble(index)
                    )
                )
            }
        }

        val observedAt = LocalDateTime.parse(current.getString("time"))
        return GardenWeatherForecast(
            localTime = observedAt,
            current = CurrentGardenWeather(
                observedAt = observedAt,
                temperatureC = current.getDouble("temperature_2m"),
                precipitationMm = current.optDouble("precipitation", 0.0).finiteOrZero(),
                windMetersPerSecond = current.optDouble("wind_speed_10m", 0.0).finiteOrZero(),
                conditionText = weatherCodeText(current.optInt("weather_code", -1))
            ),
            hourly = hours,
            daily = days
        )
    }

    private fun requestJson(url: String): JSONObject = JSONObject(requestText(url))

    private fun requestText(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "GardenSpa-Android/1.0")
            val code = connection.responseCode
            if (code !in 200..299) {
                val message = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                error("Погодный сервис вернул ошибку $code${message.takeIf(String::isNotBlank)?.let { ": $it" }.orEmpty()}")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun coordinate(value: Double): String = String.format(Locale.US, "%.5f", value)

    private fun org.json.JSONArray.safeDouble(index: Int): Double =
        optDouble(index, 0.0).finiteOrZero()

    private fun Double.finiteOrZero(): Double = takeUnless(Double::isNaN) ?: 0.0

    private fun weatherCodeText(code: Int): String = when (code) {
        0 -> "Ясно"
        1 -> "Преимущественно ясно"
        2 -> "Переменная облачность"
        3 -> "Пасмурно"
        45, 48 -> "Туман"
        51, 53, 55 -> "Морось"
        56, 57 -> "Переохлаждённая морось"
        61, 63, 65 -> "Дождь"
        66, 67 -> "Ледяной дождь"
        71, 73, 75, 77 -> "Снег"
        80, 81, 82 -> "Ливень"
        85, 86 -> "Снегопад"
        95 -> "Гроза"
        96, 99 -> "Гроза с градом"
        else -> "Погодные условия неизвестны"
    }

    private data class CachedGardenWeather(
        val loadedAtMillis: Long,
        val forecast: GardenWeatherForecast
    )

    private companion object {
        const val CACHE_MILLIS = 10 * 60 * 1_000L
    }
}
