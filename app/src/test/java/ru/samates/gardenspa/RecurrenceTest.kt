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
    fun weeklyDaysAreWorkDatesWithinStartAndEndBounds() {
        val rule = plant(start = "2026-09-16", type = "WEEKLY", days = "2,4",
            endType = "UNTIL_DATE", endDate = "2026-10-12")
        val expected = setOf("2026-09-17", "2026-09-22", "2026-09-24", "2026-09-29",
            "2026-10-01", "2026-10-06", "2026-10-08")
        val start = LocalDate.parse("2026-09-14")
        (0L..30L).map(start::plusDays).forEach { date ->
            org.junit.Assert.assertEquals(date.toString(), date.toString() in expected, rule.occursOn(date))
        }
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
