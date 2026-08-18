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
        record?.status != "CANCELLED" && (record?.rescheduledDate == null || record.rescheduledDate == date.toString())
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
        if (!plant.occursOn(originalDate)) return@mapNotNull null
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

fun pendingProgramTreatments(
    plants: List<PlantEntity>,
    procedures: List<ProcedureEntity>
): List<ScheduledTreatment> {
    val procedureByOccurrence = procedures.associateBy { it.plantId to it.scheduledDate }
    return plants.asSequence()
        .filter { it.programId != null }
        .flatMap { plant -> plant.programOccurrenceDates().map { plant to it } }
        .mapNotNull { (plant, originalDate) ->
            val record = procedureByOccurrence[plant.id to originalDate.toString()]
            if (record?.status == "COMPLETED" || record?.status == "CANCELLED") return@mapNotNull null
            val scheduledDate = record?.rescheduledDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: originalDate
            ScheduledTreatment(plant, originalDate, scheduledDate, completed = false, rescheduled = scheduledDate != originalDate)
        }
        .sortedWith(compareBy({ it.scheduledDate }, { it.plant.gardenName }, { it.plant.plantName }))
        .toList()
}

private fun PlantEntity.programOccurrenceDates(): Sequence<LocalDate> {
    val start = runCatching { LocalDate.parse(creationDate) }.getOrNull() ?: return emptySequence()
    if (repeatType == RepeatType.NONE.name) return sequenceOf(start)
    if (repeatEndType != RepeatEndType.COUNT.name) return sequenceOf(start)
    // ponytail: daily scan is bounded by the small occurrence count in a ready seasonal program.
    return generateSequence(start) { it.plusDays(1) }.filter(::occursOn).take(repeatCount?.coerceAtLeast(1) ?: 1)
}
