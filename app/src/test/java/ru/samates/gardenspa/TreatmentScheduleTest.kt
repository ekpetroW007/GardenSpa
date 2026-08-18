package ru.samates.gardenspa

import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import ru.samates.gardenspa.domain.scheduledTreatmentsOn
import ru.samates.gardenspa.domain.pendingProgramTreatments
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
    fun cancelledOccurrenceIsHiddenWithoutChangingTheSeries() {
        val cancelled = ProcedureEntity(
            plantId = plant.id,
            procedureName = plant.taskName,
            scheduledDate = "2026-07-28",
            status = "CANCELLED"
        )

        assertTrue(scheduledTreatmentsOn(listOf(plant), listOf(cancelled), LocalDate.parse("2026-07-28")).isEmpty())
        assertTrue(scheduledTreatmentsOn(listOf(plant), listOf(cancelled), LocalDate.parse("2026-07-29")).isNotEmpty())
    }

    @Test
    fun endingSeriesAlsoHidesFutureMovedOccurrences() {
        val endedPlant = plant.copy(repeatEndType = "UNTIL_DATE", repeatEndDate = "2026-07-28")
        val moved = ProcedureEntity(
            plantId = plant.id,
            procedureName = plant.taskName,
            scheduledDate = "2026-07-29",
            rescheduledDate = "2026-07-30"
        )

        assertTrue(scheduledTreatmentsOn(listOf(endedPlant), listOf(moved), LocalDate.parse("2026-07-30")).isEmpty())
    }

    @Test
    fun readyProgramListContainsOnlyUnfinishedOccurrences() {
        val programPlant = plant.copy(programId = "tomato", repeatEndType = "COUNT", repeatCount = 3)
        val records = listOf(
            ProcedureEntity(plantId = plant.id, procedureName = plant.taskName, scheduledDate = "2026-07-27", completedDate = "2026-07-27", status = "COMPLETED"),
            ProcedureEntity(plantId = plant.id, procedureName = plant.taskName, scheduledDate = "2026-07-28", status = "CANCELLED"),
            ProcedureEntity(plantId = plant.id, procedureName = plant.taskName, scheduledDate = "2026-07-29", rescheduledDate = "2026-07-31")
        )

        val pending = pendingProgramTreatments(listOf(programPlant, plant.copy(id = 8)), records)

        assertEquals(1, pending.size)
        assertEquals(LocalDate.parse("2026-07-29"), pending.single().originalDate)
        assertEquals(LocalDate.parse("2026-07-31"), pending.single().scheduledDate)
    }
}
