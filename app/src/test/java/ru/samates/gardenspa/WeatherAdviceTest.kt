package ru.samates.gardenspa

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.domain.ForecastWeatherDay
import ru.samates.gardenspa.domain.HourlyGardenWeather
import ru.samates.gardenspa.domain.ScheduledTreatment
import ru.samates.gardenspa.domain.WeatherLimitKind
import ru.samates.gardenspa.domain.WeatherLimits
import ru.samates.gardenspa.domain.violationsFor
import ru.samates.gardenspa.domain.findWeatherWindow
import ru.samates.gardenspa.domain.weatherWorkAdvice
import ru.samates.gardenspa.domain.suggestedWeatherSafeDate

class WeatherAdviceTest {
    private val today: LocalDate = LocalDate.of(2026, 8, 25)

    @Test
    fun generatedWorkGetsSeparateTodayAndTomorrowAdviceWhileManualWorkIsIgnored() {
        val treatments = listOf(
            treatment(id = 1, date = today),
            treatment(id = 2, date = today.plusDays(1)),
            treatment(id = 3, date = today, programId = null, stepId = null)
        )
        val forecast = listOf(
            forecast(date = today, precipitation = 7.0, wind = 3.0),
            forecast(date = today.plusDays(1), precipitation = 0.0, wind = 9.0)
        )

        val advice = weatherWorkAdvice(treatments, forecast, today)

        assertEquals(2, advice.size)
        assertEquals(listOf(WeatherLimitKind.PRECIPITATION), advice[0].violations.map { it.kind })
        assertEquals(listOf(WeatherLimitKind.WIND), advice[1].violations.map { it.kind })
        assertTrue(advice[0].message.startsWith("Сегодня "))
        assertTrue(advice[1].message.startsWith("Завтра "))
        assertTrue(advice.all { "Работу лучше перенести." in it.message })
    }

    @Test
    fun allThreeWeatherLimitsProduceStructuredViolations() {
        val limits = WeatherLimits(
            minimumNightTemperatureC = 10.0,
            maximumPrecipitationMm = 3.0,
            maximumWindMetersPerSecond = 6.0
        )

        val violations = limits.violationsFor(
            ForecastWeatherDay(
                date = today,
                minimumTemperatureC = 8.0,
                maximumTemperatureC = 18.0,
                precipitationMm = 4.0,
                maximumWindMetersPerSecond = 7.0
            )
        )

        assertEquals(
            listOf(
                WeatherLimitKind.NIGHT_TEMPERATURE,
                WeatherLimitKind.PRECIPITATION,
                WeatherLimitKind.WIND
            ),
            violations.map { it.kind }
        )
    }

    @Test
    fun completedWorkAndForecastExactlyAtLimitsDoNotProduceAdvice() {
        val treatments = listOf(
            treatment(id = 1, date = today, completed = true),
            treatment(id = 2, date = today.plusDays(1))
        )
        val forecast = listOf(
            forecast(date = today, precipitation = 20.0, wind = 20.0),
            forecast(date = today.plusDays(1), precipitation = 2.0, wind = 6.0)
        )

        assertTrue(weatherWorkAdvice(treatments, forecast, today).isEmpty())
    }

    @Test
    fun rescheduleActionUsesTheFirstLaterDayWithinWeatherLimits() {
        val work = treatment(id = 1, date = today.plusDays(1))
        val forecast = listOf(
            forecast(today.plusDays(1), precipitation = 9.0, wind = 3.0),
            forecast(today.plusDays(2), precipitation = 6.0, wind = 3.0),
            forecast(today.plusDays(3), precipitation = 0.0, wind = 3.0)
        )

        assertEquals(today.plusDays(3), suggestedWeatherSafeDate(work, forecast))
    }

    @Test
    fun `weather window keeps the two dry hours required by fitosporin`() {
        val work = treatment(id = 1, date = today)
        val hourly = listOf(
            hour(16, precipitation = 1.0, chanceOfRain = 90),
            hour(17, precipitation = 0.0, chanceOfRain = 10),
            hour(18, precipitation = 0.0, chanceOfRain = 15),
            hour(19, precipitation = 1.0, chanceOfRain = 80)
        )

        val window = findWeatherWindow(work, hourly, today.atTime(16, 30))

        assertEquals(today.atTime(17, 0), window?.start)
        assertEquals(today.atTime(19, 0), window?.endExclusive)
    }

    private fun treatment(
        id: Int,
        date: LocalDate,
        programId: String? = "tomato",
        stepId: String? = "fitosporin_spraying",
        completed: Boolean = false
    ): ScheduledTreatment = ScheduledTreatment(
        plant = PlantEntity(
            id = id,
            plantName = "Томат",
            taskName = "Опрыскать томаты для профилактики болезней",
            wateringInterval = 1,
            creationDate = date.toString(),
            drugId = null,
            gardenId = 1,
            drugName = "",
            gardenName = "Дача",
            programId = programId,
            programVersion = 3,
            programStepId = stepId
        ),
        originalDate = date,
        scheduledDate = date,
        completed = completed,
        rescheduled = false
    )

    private fun forecast(date: LocalDate, precipitation: Double, wind: Double): ForecastWeatherDay =
        ForecastWeatherDay(
            date = date,
            minimumTemperatureC = 12.0,
            maximumTemperatureC = 22.0,
            precipitationMm = precipitation,
            maximumWindMetersPerSecond = wind
        )

    private fun hour(hour: Int, precipitation: Double, chanceOfRain: Int) = HourlyGardenWeather(
        time = LocalDateTime.of(today, java.time.LocalTime.of(hour, 0)),
        temperatureC = 20.0,
        precipitationMm = precipitation,
        chanceOfRainPercent = chanceOfRain,
        windMetersPerSecond = 3.0,
        conditionText = if (precipitation > 0.0) "Дождь" else "Ясно"
    )
}
