package ru.samates.gardenspa.domain

import java.time.LocalDate

enum class WeatherLimitKind {
    NIGHT_TEMPERATURE,
    PRECIPITATION,
    WIND
}

data class WeatherLimitViolation(
    val kind: WeatherLimitKind,
    val actual: Double,
    val limit: Double
)

data class WeatherWorkAdvice(
    val treatment: ScheduledTreatment,
    val forecast: ForecastWeatherDay,
    val violations: List<WeatherLimitViolation>,
    val message: String
)

/**
 * Returns advice for incomplete generated work scheduled for today or tomorrow.
 * All treatments passed to one call must belong to the location represented by [forecast].
 */
fun weatherWorkAdvice(
    treatments: List<ScheduledTreatment>,
    forecast: List<ForecastWeatherDay>,
    today: LocalDate
): List<WeatherWorkAdvice> {
    val tomorrow = today.plusDays(1)
    val forecastByDate = forecast.associateBy(ForecastWeatherDay::date)
    val templatesById = PlantCareCatalog.all().associateBy(PlantCareTemplate::id)

    return treatments.mapNotNull { treatment ->
        val date = treatment.scheduledDate
        if (treatment.completed || (date != today && date != tomorrow)) return@mapNotNull null

        val programId = treatment.plant.programId ?: return@mapNotNull null
        val stepId = treatment.plant.programStepId ?: return@mapNotNull null
        val limits = treatment.weatherLimits(templatesById, programId, stepId)
            ?: return@mapNotNull null
        val dayForecast = forecastByDate[date] ?: return@mapNotNull null
        val violations = limits.violationsFor(dayForecast)
        if (violations.isEmpty()) return@mapNotNull null

        val day = if (date == today) "Сегодня" else "Завтра"
        val reasons = violations.joinToString("; ", transform = WeatherLimitViolation::russianText)
        WeatherWorkAdvice(
            treatment = treatment,
            forecast = dayForecast,
            violations = violations,
            message = "$day возможны неподходящие условия для работы «${treatment.plant.taskName}»: $reasons. Работу лучше перенести."
        )
    }
}

fun suggestedWeatherSafeDate(
    treatment: ScheduledTreatment,
    forecast: List<ForecastWeatherDay>
): LocalDate? {
    if (treatment.completed) return null
    val programId = treatment.plant.programId ?: return null
    val stepId = treatment.plant.programStepId ?: return null
    val templatesById = PlantCareCatalog.all().associateBy(PlantCareTemplate::id)
    val limits = treatment.weatherLimits(templatesById, programId, stepId) ?: return null
    return forecast.asSequence()
        .filter { day -> day.date.isAfter(treatment.scheduledDate) }
        .sortedBy(ForecastWeatherDay::date)
        .firstOrNull { day -> limits.violationsFor(day).isEmpty() }
        ?.date
}

private fun ScheduledTreatment.weatherLimits(
    templatesById: Map<String, PlantCareTemplate>,
    programId: String,
    stepId: String
): WeatherLimits? = templatesById[programId]
    ?.steps
    ?.firstOrNull { it.id == stepId }
    ?.weatherLimits

fun WeatherLimits.violationsFor(forecast: ForecastWeatherDay): List<WeatherLimitViolation> = buildList {
    minimumNightTemperatureC?.let { limit ->
        if (forecast.minimumTemperatureC < limit) {
            add(WeatherLimitViolation(WeatherLimitKind.NIGHT_TEMPERATURE, forecast.minimumTemperatureC, limit))
        }
    }
    maximumPrecipitationMm?.let { limit ->
        if (forecast.precipitationMm > limit) {
            add(WeatherLimitViolation(WeatherLimitKind.PRECIPITATION, forecast.precipitationMm, limit))
        }
    }
    maximumWindMetersPerSecond?.let { limit ->
        if (forecast.maximumWindMetersPerSecond > limit) {
            add(WeatherLimitViolation(WeatherLimitKind.WIND, forecast.maximumWindMetersPerSecond, limit))
        }
    }
}

private fun WeatherLimitViolation.russianText(): String = when (kind) {
    WeatherLimitKind.NIGHT_TEMPERATURE ->
        "ночная температура ${actual.weatherValue()} °C ниже допустимых ${limit.weatherValue()} °C"
    WeatherLimitKind.PRECIPITATION ->
        "осадки ${actual.weatherValue()} мм выше допустимых ${limit.weatherValue()} мм"
    WeatherLimitKind.WIND ->
        "ветер ${actual.weatherValue()} м/с сильнее допустимых ${limit.weatherValue()} м/с"
}

private fun Double.weatherValue(): String =
    if (this == toLong().toDouble()) toLong().toString() else toString().replace('.', ',')
