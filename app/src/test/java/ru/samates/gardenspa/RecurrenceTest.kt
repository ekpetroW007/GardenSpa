package ru.samates.gardenspa

import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.domain.occursOn
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurrenceTest {
    private fun plant(
        start: String = "2026-07-27",
        type: String,
        interval: Int = 1,
        days: String = "",
        endType: String = "NEVER",
        endDate: String? = null,
        count: Int? = null
    ) = PlantEntity(
        plantName = "Томат",
        taskName = "Полив",
        wateringInterval = interval,
        creationDate = start,
        drugId = null,
        gardenId = null,
        drugName = "Не выбран",
        gardenName = "Не выбран",
        repeatType = type,
        repeatInterval = interval,
        repeatDaysOfWeek = days,
        repeatEndType = endType,
        repeatEndDate = endDate,
        repeatCount = count
    )

    @Test
    fun dailyRuleUsesSelectedCalendarDay() {
        val rule = plant(type = "DAILY", interval = 2)
        assertTrue(rule.occursOn(LocalDate.parse("2026-07-27")))
        assertFalse(rule.occursOn(LocalDate.parse("2026-07-28")))
        assertTrue(rule.occursOn(LocalDate.parse("2026-07-29")))
    }

    @Test
    fun weeklyRuleSupportsSeveralWeekdays() {
        val rule = plant(type = "WEEKLY", days = "1,3,5")
        assertTrue(rule.occursOn(LocalDate.parse("2026-07-29")))
        assertFalse(rule.occursOn(LocalDate.parse("2026-07-30")))
        assertTrue(rule.occursOn(LocalDate.parse("2026-07-31")))
    }

    @Test
    fun endDateIsInclusive() {
        val rule = plant(type = "DAILY", endType = "UNTIL_DATE", endDate = "2026-07-29")
        assertTrue(rule.occursOn(LocalDate.parse("2026-07-29")))
        assertFalse(rule.occursOn(LocalDate.parse("2026-07-30")))
    }

    @Test
    fun countLimitsOccurrences() {
        val rule = plant(type = "DAILY", endType = "COUNT", count = 3)
        assertTrue(rule.occursOn(LocalDate.parse("2026-07-29")))
        assertFalse(rule.occursOn(LocalDate.parse("2026-07-30")))
    }
}
