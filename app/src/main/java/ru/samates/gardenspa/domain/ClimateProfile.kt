package ru.samates.gardenspa.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.MonthDay
import kotlin.math.roundToInt

enum class LocationSource {
    PLACE_SEARCH,
    APPROXIMATE_DEVICE,
    MANUAL_COORDINATES,
    REGION_FALLBACK
}

enum class ClimateConfidence {
    LOW,
    MEDIUM,
    HIGH
}

data class GardenLocation(
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Int? = null,
    val localityName: String,
    val source: LocationSource,
    val accuracyKm: Double
)

data class HistoricalWeatherDay(
    val date: LocalDate,
    val minimumTemperatureC: Double,
    val maximumTemperatureC: Double,
    val precipitationMm: Double
)

data class ForecastWeatherDay(
    val date: LocalDate,
    val minimumTemperatureC: Double,
    val maximumTemperatureC: Double,
    val precipitationMm: Double,
    val maximumWindMetersPerSecond: Double
)

data class CurrentGardenWeather(
    val observedAt: LocalDateTime,
    val temperatureC: Double,
    val precipitationMm: Double,
    val windMetersPerSecond: Double,
    val conditionText: String
)

data class HourlyGardenWeather(
    val time: LocalDateTime,
    val temperatureC: Double,
    val precipitationMm: Double,
    val chanceOfRainPercent: Int,
    val windMetersPerSecond: Double,
    val conditionText: String
)

data class GardenWeatherForecast(
    val localTime: LocalDateTime,
    val current: CurrentGardenWeather,
    val hourly: List<HourlyGardenWeather>,
    val daily: List<ForecastWeatherDay>
)

data class ClimateFingerprint(
    val safeSpringDay: MonthDay,
    val safeAutumnDay: MonthDay,
    val frostFreeDays: Int,
    val growingDegreeDays5: Double,
    val growingDegreeDays10: Double,
    val warmSeasonPrecipitationMm: Double,
    val winterMinimumP10: Double,
    val confidence: ClimateConfidence,
    val sourceYears: Int
) {
    fun safeSpringDate(year: Int): LocalDate = safeSpringDay.atYear(year)

    fun safeAutumnDate(year: Int): LocalDate = safeAutumnDay.atYear(year)

    fun displayName(): String {
        val temperature = when {
            frostFreeDays < 120 || growingDegreeDays10 < 1_200 -> "прохладный"
            frostFreeDays > 190 || growingDegreeDays10 > 2_600 -> "тёплый"
            else -> "умеренный"
        }
        val moisture = when {
            warmSeasonPrecipitationMm < 260 -> "влажность низкая"
            warmSeasonPrecipitationMm > 480 -> "влажность высокая"
            else -> "влажность умеренная"
        }
        return "$temperature, $moisture"
    }
}

class ClimateFingerprintCalculator {
    fun calculate(days: List<HistoricalWeatherDay>): ClimateFingerprint {
        require(days.isNotEmpty()) { "Для расчёта климата нужны исторические данные" }

        val completeYears = days
            .groupBy { it.date.year }
            .filterValues { yearDays -> yearDays.size >= 330 }
        require(completeYears.size >= 3) {
            "Недостаточно полных лет для устойчивого климатического расчёта"
        }

        val lastSpringFrostDays = mutableListOf<Int>()
        val firstAutumnFrostDays = mutableListOf<Int>()
        val annualGdd5 = mutableListOf<Double>()
        val annualGdd10 = mutableListOf<Double>()
        val annualWarmPrecipitation = mutableListOf<Double>()
        val annualWinterMinimums = mutableListOf<Double>()

        completeYears.toSortedMap().values.forEach { yearDays ->
            val springFrost = yearDays
                .filter { it.date.monthValue <= 6 && it.minimumTemperatureC <= 0.0 }
                .maxByOrNull { it.date }
                ?.date
                ?.let(::canonicalDayOfYear)
                ?: canonicalDayOfYear(LocalDate.of(2000, Month.JANUARY, 1))
            val autumnFrost = yearDays
                .filter { it.date.monthValue >= 7 && it.minimumTemperatureC <= 0.0 }
                .minByOrNull { it.date }
                ?.date
                ?.let(::canonicalDayOfYear)
                ?: canonicalDayOfYear(LocalDate.of(2000, Month.DECEMBER, 31))

            lastSpringFrostDays += springFrost
            firstAutumnFrostDays += autumnFrost
            annualGdd5 += yearDays.sumOf { day -> degreeDays(day, baseTemperature = 5.0) }
            annualGdd10 += yearDays.sumOf { day -> degreeDays(day, baseTemperature = 10.0) }
            annualWarmPrecipitation += yearDays
                .filter { it.date.monthValue in 4..10 }
                .sumOf { it.precipitationMm.coerceAtLeast(0.0) }
            annualWinterMinimums += yearDays
                .filter { it.date.monthValue in setOf(1, 2, 12) }
                .minOf { it.minimumTemperatureC }
        }

        val springDay = percentile(lastSpringFrostDays, 0.80).coerceIn(1, 366)
        val autumnDay = percentile(firstAutumnFrostDays, 0.20).coerceIn(1, 366)
        val frostFreeDays = (autumnDay - springDay).coerceAtLeast(0)
        val years = completeYears.size

        return ClimateFingerprint(
            safeSpringDay = canonicalMonthDay(springDay),
            safeAutumnDay = canonicalMonthDay(autumnDay),
            frostFreeDays = frostFreeDays,
            growingDegreeDays5 = annualGdd5.average(),
            growingDegreeDays10 = annualGdd10.average(),
            warmSeasonPrecipitationMm = annualWarmPrecipitation.average(),
            winterMinimumP10 = percentile(annualWinterMinimums, 0.10),
            confidence = when {
                years >= 15 -> ClimateConfidence.HIGH
                years >= 8 -> ClimateConfidence.MEDIUM
                else -> ClimateConfidence.LOW
            },
            sourceYears = years
        )
    }

    private fun degreeDays(day: HistoricalWeatherDay, baseTemperature: Double): Double {
        val average = (day.minimumTemperatureC + day.maximumTemperatureC) / 2.0
        return (average - baseTemperature).coerceAtLeast(0.0)
    }

    private fun canonicalDayOfYear(date: LocalDate): Int =
        LocalDate.of(2000, date.month, date.dayOfMonth).dayOfYear

    private fun canonicalMonthDay(dayOfYear: Int): MonthDay =
        MonthDay.from(LocalDate.ofYearDay(2000, dayOfYear))

    private fun percentile(values: List<Int>, percentile: Double): Int {
        val sorted = values.sorted()
        val index = ((sorted.lastIndex) * percentile).roundToInt()
        return sorted[index]
    }

    private fun percentile(values: List<Double>, percentile: Double): Double {
        val sorted = values.sorted()
        val index = ((sorted.lastIndex) * percentile).roundToInt()
        return sorted[index]
    }
}
