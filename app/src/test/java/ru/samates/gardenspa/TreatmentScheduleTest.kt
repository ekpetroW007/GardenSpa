package ru.samates.gardenspa

import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import ru.samates.gardenspa.domain.scheduledTreatmentsOn
import ru.samates.gardenspa.domain.nearestIncompleteTreatment
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TreatmentScheduleTest {
    private val plant = PlantEntity(
        id = 7,
        plantName = "Томат",
        taskName = "Опрыскивание",
        wateringInterval = 1,
        creationDate = "2026-07-27",
        drugId = null,
        gardenId = null,
        drugName = "Средство",
        gardenName = "Теплица",
        repeatType = "DAILY"
    )

    @Test
    fun rescheduledOccurrenceMovesWithoutChangingTheSeries() {
        val moved = ProcedureEntity(
            plantId = plant.id,
            procedureName = plant.taskName,
            scheduledDate = "2026-07-28",
            rescheduledDate = "2026-07-30"
        )

        assertTrue(scheduledTreatmentsOn(listOf(plant), listOf(moved), LocalDate.parse("2026-07-28")).isEmpty())
        val destination = scheduledTreatmentsOn(
            listOf(plant),
            listOf(moved),
            LocalDate.parse("2026-07-30")
        )
        assertEquals(2, destination.size)
        assertTrue(destination.any { it.originalDate == LocalDate.parse("2026-07-28") && it.rescheduled })
        assertTrue(destination.any { it.originalDate == LocalDate.parse("2026-07-30") && !it.rescheduled })
    }

    @Test
    fun completedMovedOccurrenceKeepsItsEffectiveDate() {
        val completed = ProcedureEntity(
            plantId = plant.id,
            procedureName = plant.taskName,
            scheduledDate = "2026-07-28",
            rescheduledDate = "2026-07-29",
            completedDate = "2026-07-29",
            status = "COMPLETED"
        )

        val treatment = scheduledTreatmentsOn(
            listOf(plant),
            listOf(completed),
            LocalDate.parse("2026-07-29")
        ).first { it.originalDate == LocalDate.parse("2026-07-28") }
        assertTrue(treatment.completed)
        assertFalse(treatment.scheduledDate == treatment.originalDate)
    }

    @Test
    fun plannedRecordOnTodayRestoresACompletedOccurrence() {
        val restored = ProcedureEntity(
            plantId = plant.id,
            procedureName = plant.taskName,
            scheduledDate = "2026-07-30",
            rescheduledDate = null,
            completedDate = null,
            status = "PLANNED"
        )

        val treatment = scheduledTreatmentsOn(
            listOf(plant),
            listOf(restored),
            LocalDate.parse("2026-07-30")
        ).single()

        assertFalse(treatment.completed)
        assertEquals(LocalDate.parse("2026-07-30"), treatment.scheduledDate)
    }

    @Test
    fun nearestIncompleteTreatmentSelectsTheFirstFutureWork() {
        val fromDate = LocalDate.parse("2026-08-26")
        val later = plant.copy(
            id = 8,
            creationDate = fromDate.plusDays(5).toString(),
            repeatType = "NONE",
            gardenId = 8
        )
        val sooner = plant.copy(
            id = 9,
            creationDate = fromDate.plusDays(1).toString(),
            repeatType = "NONE",
            gardenId = 9
        )

        val nearest = nearestIncompleteTreatment(listOf(later, sooner), emptyList(), fromDate)

        assertEquals(9, nearest?.plant?.gardenId)
        assertEquals(fromDate.plusDays(1), nearest?.scheduledDate)
    }

    @Test
    fun nearestWorkIsNotLimitedToOneYearAndStillHonorsAnExplicitSearchWindow() {
        val today = LocalDate.of(2026, 9, 30)
        val future = plant.copy(repeatType = "NONE", creationDate = "2028-04-15", gardenId = 9)
        assertEquals(LocalDate.of(2028, 4, 15), nearestIncompleteTreatment(listOf(future), emptyList(), today)?.scheduledDate)
        assertNull(nearestIncompleteTreatment(listOf(future), emptyList(), today, searchDays = 366))
        assertNull(nearestIncompleteTreatment(listOf(future), emptyList(), today, searchDays = -1))

        val moved = ProcedureEntity(plantId = future.id, procedureName = future.taskName,
            scheduledDate = "2028-04-15", rescheduledDate = "2029-05-10")
        assertEquals(LocalDate.of(2029, 5, 10), nearestIncompleteTreatment(listOf(future), listOf(moved), today)?.scheduledDate)
        assertNull(nearestIncompleteTreatment(listOf(future), listOf(moved.copy(status = "COMPLETED")), today))
    }

    @Test
    fun aRareRepeatFindsItsNextOccurrenceAndStopsAfterTheLastCompletion() {
        val biennial = plant.copy(creationDate = "2024-02-29", repeatType = "YEARLY", repeatInterval = 2,
            repeatEndType = "COUNT", repeatCount = 3)
        val today = LocalDate.of(2026, 3, 1)
        val last = LocalDate.of(2028, 2, 29)
        assertEquals(last, nearestIncompleteTreatment(listOf(biennial), emptyList(), today)?.scheduledDate)
        val completed = ProcedureEntity(plantId = biennial.id, procedureName = biennial.taskName,
            scheduledDate = last.toString(), completedDate = last.toString(), status = "COMPLETED")
        assertNull(nearestIncompleteTreatment(listOf(biennial), listOf(completed), today))
        assertEquals(last, nearestIncompleteTreatment(listOf(biennial), listOf(completed.copy(status = "PLANNED")), today)?.scheduledDate)
    }

    @Test
    fun nearestUsesTheSameDatesAsTheCalendarForAllRepeatKindsAndHistory() {
        val from = LocalDate.of(2026, 8, 26)
        val horizon = 800L
        val rules = listOf(
            plant.copy(repeatType = "DAILY", repeatInterval = 3),
            plant.copy(repeatType = "CUSTOM", repeatInterval = 30),
            plant.copy(repeatType = "WEEKLY", repeatInterval = 2, repeatDaysOfWeek = "1,3,5"),
            plant.copy(repeatType = "WEEKLY", repeatInterval = 3, repeatDaysOfWeek = ""),
            plant.copy(creationDate = "2026-01-31", repeatType = "MONTHLY", repeatInterval = 1),
            plant.copy(creationDate = "2026-07-31", repeatType = "MONTHLY", repeatInterval = 1, repeatEndType = "COUNT", repeatCount = 3),
            plant.copy(creationDate = "2024-02-29", repeatType = "YEARLY", repeatInterval = 2),
            plant.copy(repeatType = "CUSTOM", repeatInterval = 7, repeatEndType = "COUNT", repeatCount = 2),
            plant.copy(repeatType = "DAILY", repeatEndType = "UNTIL_DATE", repeatEndDate = from.toString())
        )
        rules.forEach { rule ->
            val first = (0L..horizon).firstNotNullOfOrNull { offset ->
                scheduledTreatmentsOn(listOf(rule), emptyList(), from.plusDays(offset)).firstOrNull()
            }
            val records = first?.let { listOf(ProcedureEntity(plantId = rule.id, procedureName = rule.taskName,
                scheduledDate = it.originalDate.toString(), rescheduledDate = it.scheduledDate.plusDays(4).toString())) }.orEmpty()
            val expected = (0L..horizon).firstNotNullOfOrNull { offset ->
                scheduledTreatmentsOn(listOf(rule), records, from.plusDays(offset)).firstOrNull { !it.completed }
            }
            assertEquals("${rule.repeatType}/${rule.repeatInterval}/${rule.repeatEndType}", expected,
                nearestIncompleteTreatment(listOf(rule), records, from, horizon))
        }
    }
}
