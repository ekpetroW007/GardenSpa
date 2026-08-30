package ru.samates.gardenspa.data.climate

import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import ru.samates.gardenspa.domain.CurrentGardenWeather
import ru.samates.gardenspa.domain.ForecastWeatherDay
import ru.samates.gardenspa.domain.GardenLocation
import ru.samates.gardenspa.domain.GardenWeatherForecast
import ru.samates.gardenspa.domain.HourlyGardenWeather

class WeatherApiClient(
    private val apiKey: String,
    private val proxyUrl: String
) {
    private val cache = mutableMapOf<String, CachedForecast>()

    suspend fun loadForecast(location: GardenLocation): GardenWeatherForecast = withContext(Dispatchers.IO) {
        val cacheKey = "${coordinate(location.latitude)},${coordinate(location.longitude)}"
        synchronized(cache) {
            cache[cacheKey]
                ?.takeIf { System.currentTimeMillis() - it.loadedAtMillis < CACHE_MILLIS }
                ?.forecast
        }?.let { return@withContext it }

        val forecast = parseForecast(requestJson(requestUrl(cacheKey)))
        synchronized(cache) {
            cache[cacheKey] = CachedForecast(System.currentTimeMillis(), forecast)
        }
        forecast
    }

    private fun requestUrl(coordinates: String): String {
        val query = "q=$coordinates&days=3&aqi=no&alerts=no&lang=ru"
        if (proxyUrl.isNotBlank()) {
            return "${proxyUrl.trim()}${if ('?' in proxyUrl) '&' else '?'}$query"
        }
        require(apiKey.isNotBlank()) {
            "WeatherAPI не настроен: добавьте WEATHERAPI_API_KEY для debug или WEATHERAPI_PROXY_URL для публикации"
        }
        val encodedKey = URLEncoder.encode(apiKey.trim(), StandardCharsets.UTF_8.name())
        return "https://api.weatherapi.com/v1/forecast.json?key=$encodedKey&$query"
    }

    private fun parseForecast(root: JSONObject): GardenWeatherForecast {
        val location = root.getJSONObject("location")
        val current = root.getJSONObject("current")
        val forecastDays = root.getJSONObject("forecast").getJSONArray("forecastday")
        val hourly = buildList {
            for (dayIndex in 0 until forecastDays.length()) {
                val hours = forecastDays.getJSONObject(dayIndex).getJSONArray("hour")
                for (hourIndex in 0 until hours.length()) {
                    val hour = hours.getJSONObject(hourIndex)
                    add(
                        HourlyGardenWeather(
                            time = LocalDateTime.parse(hour.getString("time"), DATE_TIME_FORMAT),
                            temperatureC = hour.getDouble("temp_c"),
                            precipitationMm = hour.optDouble("precip_mm", 0.0),
                            chanceOfRainPercent = hour.optInt("chance_of_rain", 0),
                            windMetersPerSecond = hour.optDouble("wind_kph", 0.0) / 3.6,
                            conditionText = hour.getJSONObject("condition").optString("text")
                        )
                    )
                }
            }
        }
        val daily = buildList {
            for (index in 0 until forecastDays.length()) {
                val forecastDay = forecastDays.getJSONObject(index)
                val day = forecastDay.getJSONObject("day")
                add(
                    ForecastWeatherDay(
                        date = LocalDate.parse(forecastDay.getString("date")),
                        minimumTemperatureC = day.getDouble("mintemp_c"),
                        maximumTemperatureC = day.getDouble("maxtemp_c"),
                        precipitationMm = day.optDouble("totalprecip_mm", 0.0),
                        maximumWindMetersPerSecond = day.optDouble("maxwind_kph", 0.0) / 3.6
                    )
                )
            }
        }
        return GardenWeatherForecast(
            localTime = LocalDateTime.parse(location.getString("localtime"), DATE_TIME_FORMAT),
            current = CurrentGardenWeather(
                observedAt = LocalDateTime.parse(current.getString("last_updated"), DATE_TIME_FORMAT),
                temperatureC = current.getDouble("temp_c"),
                precipitationMm = current.optDouble("precip_mm", 0.0),
                windMetersPerSecond = current.optDouble("wind_kph", 0.0) / 3.6,
                conditionText = current.getJSONObject("condition").optString("text")
            ),
            hourly = hourly,
            daily = daily
        )
    }

    private fun requestJson(url: String): JSONObject {
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
                error("WeatherAPI вернул ошибку $code${message.takeIf(String::isNotBlank)?.let { ": $it" }.orEmpty()}")
            }
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    private fun coordinate(value: Double): String = String.format(Locale.US, "%.5f", value)

    private data class CachedForecast(
        val loadedAtMillis: Long,
        val forecast: GardenWeatherForecast
    )

    private companion object {
        val DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm")
        const val CACHE_MILLIS: Long = 10 * 60 * 1_000L
    }
}
