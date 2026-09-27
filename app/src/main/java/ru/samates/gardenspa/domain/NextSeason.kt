package ru.samates.gardenspa.domain

import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import ru.samates.gardenspa.data.database.entity.PlantEntity
import java.time.LocalDate

val ProcedureEntity.nextSeasonImportKey: String
    get() = "archive-next-season:$id"

/** Keep the planned season date, including an explicit postponement, rather than the tap date. */
fun ProcedureEntity.nextSeasonDate(today: LocalDate = LocalDate.now()): LocalDate {
    val original = LocalDate.parse(rescheduledDate ?: scheduledDate)
    val year = maxOf(original.year + 1, today.year)
    val anniversary = original.withYear(year)
    return if (anniversary.isAfter(today)) anniversary else original.withYear(year + 1)
}

fun ProcedureEntity.nextSeasonScheduledDate(
    plants: List<PlantEntity>,
    procedures: List<ProcedureEntity>,
    today: LocalDate = LocalDate.now()
): LocalDate? {
    plants.firstOrNull { it.programImportKey == nextSeasonImportKey }?.let { copy ->
        val moved = procedures.firstOrNull { it.plantId == copy.id && it.scheduledDate == copy.creationDate }
        return LocalDate.parse(moved?.rescheduledDate ?: copy.creationDate)
    }
    val target = nextSeasonDate(today)
    // An unlimited repeat may already contain this occurrence. Do not duplicate it, but
    // changing its product or instructions must not silently replace the archived recipe.
    return target.takeIf {
        scheduledTreatmentsOn(plants, procedures, target).any { treatment ->
            with(treatment.plant) {
                id == plantId && taskName == procedureName && drugName == this@nextSeasonScheduledDate.drugName &&
                    programNote == note
            }
        }
    }
}
