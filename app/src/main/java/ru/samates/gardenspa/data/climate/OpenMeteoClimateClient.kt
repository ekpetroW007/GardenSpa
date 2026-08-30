package ru.samates.gardenspa.data.climate

import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import ru.samates.gardenspa.domain.ClimateFingerprint
import ru.samates.gardenspa.domain.ClimateFingerprintCalculator
import ru.samates.gardenspa.domain.ForecastWeatherDay
import ru.samates.gardenspa.domain.GardenLocation
import ru.samates.gardenspa.domain.GardenWeatherForecast
import ru.samates.gardenspa.domain.HistoricalWeatherDay

class ClimateService(
    private val client: OpenMeteoClimateClient = OpenMeteoClimateClient(),
    private val weatherApiClient: WeatherApiClient,
    private val calculator: ClimateFingerprintCalculator = ClimateFingerprintCalculator()
) {
    suspend fun calculateFingerprint(location: GardenLocation): ClimateFingerprint =
        calculator.calculate(client.loadHistoricalWeather(location))

    suspend fun loadForecast(location: GardenLocation): List<ForecastWeatherDay> =
        client.loadForecast(location)

    suspend fun loadGardenWeather(location: GardenLocation): GardenWeatherForecast =
        weatherApiClient.loadForecast(location)

}

class OpenMeteoClimateClient {
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
}
