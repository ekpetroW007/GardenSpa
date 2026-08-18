package ru.samates.gardenspa

import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.domain.occursOn
import ru.samates.gardenspa.domain.truncateProgramFrom
import java.time.LocalDate
import org.junit.Assert.assertEquals
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

    @Test
    fun importedProgramRemovesEveryOccurrenceFromSelectedDate() {
        val cutoff = LocalDate.parse("2026-08-20")
        val retained = listOf(
            plant(start = "2026-08-01", type = "CUSTOM", interval = 10, endType = "COUNT", count = 5).copy(id = 1),
            plant(start = "2026-08-10", type = "CUSTOM", interval = 10, endType = "COUNT", count = 5).copy(id = 2),
            plant(start = "2026-09-01", type = "NONE").copy(id = 3)
        ).truncateProgramFrom(cutoff)

        assertEquals(listOf(1, 2), retained.map(PlantEntity::id))
        assertEquals(listOf(2, 1), retained.map(PlantEntity::repeatCount))
        assertTrue(listOf("2026-08-20", "2026-08-21", "2026-08-30", "2026-09-01").map(LocalDate::parse).all { date -> retained.none { it.occursOn(date) } })
    }
}
