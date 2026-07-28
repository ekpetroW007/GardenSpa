package ru.samates.gardenspa

import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import ru.samates.gardenspa.domain.scheduledTreatmentsOn
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
}
