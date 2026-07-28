package ru.samates.gardenspa.domain

import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import java.time.LocalDate

data class ScheduledTreatment(
    val plant: PlantEntity,
    val originalDate: LocalDate,
    val scheduledDate: LocalDate,
    val completed: Boolean,
    val rescheduled: Boolean
)

fun scheduledTreatmentsOn(
    plants: List<PlantEntity>,
    procedures: List<ProcedureEntity>,
    date: LocalDate
): List<ScheduledTreatment> {
    val plantsById = plants.associateBy(PlantEntity::id)
    val procedureByOccurrence = procedures.associateBy { it.plantId to it.scheduledDate }

    val regular = plants.filter { plant ->
        if (!plant.occursOn(date)) return@filter false
        val record = procedureByOccurrence[plant.id to date.toString()]
        record?.rescheduledDate == null || record.rescheduledDate == date.toString()
    }.map { plant ->
        val record = procedureByOccurrence[plant.id to date.toString()]
        ScheduledTreatment(
            plant = plant,
            originalDate = date,
            scheduledDate = date,
            completed = record?.status == "COMPLETED",
            rescheduled = false
        )
    }

    val moved = procedures.mapNotNull { record ->
        if (record.rescheduledDate != date.toString()) return@mapNotNull null
        val plant = plantsById[record.plantId] ?: return@mapNotNull null
        val originalDate = runCatching { LocalDate.parse(record.scheduledDate) }.getOrNull()
            ?: return@mapNotNull null
        ScheduledTreatment(
            plant = plant,
            originalDate = originalDate,
            scheduledDate = date,
            completed = record.status == "COMPLETED",
            rescheduled = originalDate != date
        )
    }

    return (regular + moved)
        .distinctBy { it.plant.id to it.originalDate }
        .sortedWith(compareBy({ it.plant.gardenName }, { it.plant.plantName }, { it.originalDate }))
}
