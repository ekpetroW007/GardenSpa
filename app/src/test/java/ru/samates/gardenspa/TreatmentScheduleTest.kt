package ru.samates.gardenspa

import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import ru.samates.gardenspa.domain.scheduledTreatmentsOn
import ru.samates.gardenspa.domain.nearestIncompleteTreatment
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
