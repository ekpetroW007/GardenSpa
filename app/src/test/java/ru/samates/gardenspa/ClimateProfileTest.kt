package ru.samates.gardenspa

import java.time.LocalDate
import java.time.MonthDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.samates.gardenspa.domain.ClimateConfidence
import ru.samates.gardenspa.domain.ClimateFingerprintCalculator
import ru.samates.gardenspa.domain.HistoricalWeatherDay

class ClimateProfileTest {
    @Test
    fun displayNameUsesNaturalMoistureWording() {
        val fingerprint = ru.samates.gardenspa.domain.ClimateFingerprint(
            safeSpringDay = MonthDay.of(5, 5),
            safeAutumnDay = MonthDay.of(10, 1),
            frostFreeDays = 149,
            growingDegreeDays5 = 2_000.0,
            growingDegreeDays10 = 1_100.0,
            warmSeasonPrecipitationMm = 400.0,
            winterMinimumP10 = -25.0,
            confidence = ClimateConfidence.HIGH,
            sourceYears = 20
        )

        assertEquals("прохладный, влажность умеренная", fingerprint.displayName())
    }

    @Test
    fun calculatorUsesConservativeFrostPercentiles() {
        val days = (2011..2025).flatMap { year ->
            val lastFrostDay = 10 + (year - 2011)
            val firstFrostDay = 25 - ((year - 2011) / 3)
            generateSequence(LocalDate.of(year, 1, 1)) { date ->
                date.plusDays(1).takeIf { it.year == year }
            }.map { date ->
                val springFrost = date.monthValue < 4 ||
                    (date.monthValue == 4 && date.dayOfMonth <= lastFrostDay)
                val autumnFrost = date.monthValue > 10 ||
                    (date.monthValue == 10 && date.dayOfMonth >= firstFrostDay)
                HistoricalWeatherDay(
                    date = date,
                    minimumTemperatureC = if (springFrost || autumnFrost) -1.0 else 8.0,
                    maximumTemperatureC = if (springFrost || autumnFrost) 4.0 else 22.0,
                    precipitationMm = 1.0
                )
            }.toList()
        }

        val fingerprint = ClimateFingerprintCalculator().calculate(days)

        assertEquals(MonthDay.of(4, 21), fingerprint.safeSpringDay)
        assertTrue(fingerprint.safeAutumnDay.monthValue >= 10)
        assertEquals(ClimateConfidence.HIGH, fingerprint.confidence)
        assertEquals(15, fingerprint.sourceYears)
    }
}
