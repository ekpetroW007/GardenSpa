package ru.samates.gardenspa

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import ru.samates.gardenspa.domain.GardenWorkDraft
import ru.samates.gardenspa.domain.estimateGardenCalories
import ru.samates.gardenspa.domain.gardenWorkDate

class GardenWorkCaloriesTest {
    @Test
    fun wateringForHalfHourAtSeventyKgIsAbout140Calories() {
        assertEquals(140.0, estimateGardenCalories(4.0, 70.0, 30), 0.001)
    }

    @Test
    fun severalActivitiesAreSummed() {
        val work = listOf(
            GardenWorkDraft("watering", 30),
            GardenWorkDraft("digging", 30)
        )

        assertEquals(315.0, estimateGardenCalories(work, 70.0), 0.001)
    }

    @Test
    fun calorieDayChangesAtThreeInLocalTime() {
        assertEquals(
            LocalDate.of(2026, 8, 1),
            gardenWorkDate(LocalDateTime.of(2026, 8, 2, 2, 59))
        )
        assertEquals(
            LocalDate.of(2026, 8, 2),
            gardenWorkDate(LocalDateTime.of(2026, 8, 2, 3, 0))
        )
    }
}
