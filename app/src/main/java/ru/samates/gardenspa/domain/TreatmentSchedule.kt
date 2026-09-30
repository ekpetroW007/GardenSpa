package ru.samates.gardenspa.domain

import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

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

fun nearestIncompleteTreatment(
    plants: List<PlantEntity>,
    procedures: List<ProcedureEntity>,
    fromDate: LocalDate,
    searchDays: Long? = null
): ScheduledTreatment? {
    if (searchDays != null && searchDays < 0) return null
    val untilDate = searchDays?.let { runCatching { fromDate.plusDays(it) }.getOrDefault(LocalDate.MAX) }
    val records = procedures.associateBy { it.plantId to it.scheduledDate }
    val regular = plants.mapNotNull { plant ->
        var date = plant.nextOccurrenceDate(fromDate)
        while (date != null && (untilDate == null || !date.isAfter(untilDate))) {
            val record = records[plant.id to date.toString()]
            if (record?.status != "COMPLETED" && (record?.rescheduledDate == null || record.rescheduledDate == date.toString())) {
                return@mapNotNull ScheduledTreatment(plant, date, date, completed = false, rescheduled = false)
            }
            date = if (date == LocalDate.MAX) null else plant.nextOccurrenceDate(date.plusDays(1))
        }
        null
    }
    val plantsById = plants.associateBy(PlantEntity::id)
    val moved = procedures.mapNotNull { record ->
        if (record.status == "COMPLETED") return@mapNotNull null
        val date = record.rescheduledDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: return@mapNotNull null
        if (date.isBefore(fromDate) || (untilDate != null && date.isAfter(untilDate))) return@mapNotNull null
        val plant = plantsById[record.plantId] ?: return@mapNotNull null
        val originalDate = runCatching { LocalDate.parse(record.scheduledDate) }.getOrNull() ?: return@mapNotNull null
        ScheduledTreatment(plant, originalDate, date, completed = false, rescheduled = date != originalDate)
    }
    return (regular + moved).minWithOrNull(compareBy(
        { it.scheduledDate }, { it.plant.gardenName }, { it.plant.plantName }, { it.originalDate }))
}

/** Jump to the next recurrence instead of hiding work beyond an arbitrary one-year horizon. */
private fun PlantEntity.nextOccurrenceDate(fromDate: LocalDate): LocalDate? {
    val start = runCatching { LocalDate.parse(creationDate) }.getOrNull() ?: return null
    val from = maxOf(start, fromDate)
    val interval = repeatInterval.coerceAtLeast(1).toLong()
    val type = runCatching { RepeatType.valueOf(repeatType) }.getOrDefault(RepeatType.NONE)
    val candidate = runCatching {
        when (type) {
            RepeatType.NONE -> start.takeIf { it == from }
            RepeatType.DAILY, RepeatType.CUSTOM -> {
                val periods = (ChronoUnit.DAYS.between(start, from) + interval - 1) / interval
                start.plusDays(periods * interval)
            }
            RepeatType.WEEKLY -> {
                val startWeek = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val fromWeek = from.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val periods = ChronoUnit.WEEKS.between(startWeek, fromWeek) / interval
                val days = repeatDaysOfWeek.split(',').mapNotNull(String::toIntOrNull)
                    .filter { it in 1..7 }.distinct().sorted().ifEmpty { listOf(start.dayOfWeek.value) }
                (periods..periods + 1).asSequence().flatMap { period ->
                    days.asSequence().map { day -> startWeek.plusWeeks(period * interval).plusDays(day - 1L) }
                }.first { !it.isBefore(from) }
            }
            RepeatType.MONTHLY -> {
                val periods = ChronoUnit.MONTHS.between(start.withDayOfMonth(1), from.withDayOfMonth(1)) / interval
                start.plusMonths(periods * interval).takeUnless { it.isBefore(from) }
                    ?: start.plusMonths((periods + 1) * interval)
            }
            RepeatType.YEARLY -> {
                val periods = (from.year.toLong() - start.year) / interval
                start.plusYears(periods * interval).takeUnless { it.isBefore(from) }
                    ?: start.plusYears((periods + 1) * interval)
            }
        }
    }.getOrNull()
    // The shared calendar rule remains responsible for end dates and finite occurrence counts.
    return candidate?.takeIf { occursOn(it) }
}
