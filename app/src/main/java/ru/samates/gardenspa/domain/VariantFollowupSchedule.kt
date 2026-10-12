package ru.samates.gardenspa.domain

import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import ru.samates.gardenspa.data.database.entity.resolvedCardId
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal fun PlantEntity.effectiveVariantDate(history: List<ProcedureEntity>): LocalDate = LocalDate.parse(
    history.firstOrNull { it.plantId == id && it.scheduledDate == creationDate }?.rescheduledDate ?: creationDate
)

internal fun PlantEntity.isUnchangedVariantFollowup(expected: PlantEntity? = null): Boolean =
    programBaseline?.let { it == programFingerprint() } ?: (expected != null &&
        creationDate == programImportKey?.substringAfterLast(':') && taskName == expected.taskName &&
        drugName == expected.drugName && programNote == expected.programNote && repeatType == "NONE")

internal fun PlantEntity.variantFollowupRule(): Pair<String, Long>? {
    if (!EradicationCare.applies(programId)) return null
    return when (programBaseStep(programStepId)) {
        EradicationCare.EARLY_STEP -> EradicationCare.PEST_STEP to 7L
        EradicationCare.AUTUMN_IRON_STEP -> EradicationCare.WHITEWASH_STEP to 14L
        else -> null
    }
}

private fun PlantEntity.isImportedProgramWork(): Boolean = programId != null &&
    programImportKey?.startsWith("$resolvedCardId:$programId:v") == true

/** Import provenance identifies the original pair even after either date moves to another year. */
internal fun PlantEntity.isVariantFollowupOf(parent: PlantEntity): Boolean =
    resolvedCardId == parent.resolvedCardId && programId == parent.programId &&
        programBaseStep(programStepId) == parent.variantFollowupRule()?.first &&
        (programImportKey?.startsWith("variant-followup:${parent.id}:") == true ||
            isImportedProgramWork() && parent.isImportedProgramWork())

/** Only an untouched automatic child follows its parent; its own history or edits take priority. */
internal fun variantFollowupDateUpdates(
    parent: PlantEntity,
    plants: List<PlantEntity>,
    history: List<ProcedureEntity>,
    effectiveDate: LocalDate,
    today: LocalDate = LocalDate.now()
): List<PlantEntity> = variantStageDateUpdates(parent, plants, history, effectiveDate) +
    rootFollowupDateUpdates(parent, plants, history, effectiveDate, today)

private fun rootFollowupDateUpdates(
    parent: PlantEntity,
    plants: List<PlantEntity>,
    history: List<ProcedureEntity>,
    effectiveDate: LocalDate,
    today: LocalDate
): List<PlantEntity> {
    if (!RootCareSchedule.isSpringTreatment(parent.programStepId) || !parent.isImportedProgramWork()) return emptyList()
    val season = LocalDate.parse(parent.creationDate).year
    // A different year can already contain a separate saved season; its works have no parent-occurrence link.
    if (effectiveDate.year != season) return emptyList()
    val protectedIds = history.map { it.plantId }.toSet()
    val eligible = plants.filter { row ->
        row.resolvedCardId == parent.resolvedCardId && row.programId == parent.programId && row.isImportedProgramWork() &&
            (row.programVersion ?: 0) >= RootCareSchedule.VERSION && !row.userEdited && !row.userLockedDate && row.drugId == null &&
            !row.programStepId.orEmpty().substringAfter("~", "").startsWith("personal:") && row.id !in protectedIds &&
            row.programBaseline == row.programFingerprint() && LocalDate.parse(row.creationDate).let { it.year == season && !it.isBefore(today) }
    }
    val earliest = effectiveDate.plusDays(if (parent.programId in EradicationCare.fruitTreeIds) 1 else 0)
    val updated = linkedMapOf<Int, PlantEntity>()
    fun moveForward(row: PlantEntity, date: LocalDate) {
        if (!LocalDate.parse(row.creationDate).isBefore(date)) return
        val changed = row.copy(creationDate = date.toString())
        updated[row.id] = changed.copy(programBaseline = changed.programFingerprint())
    }
    // These dates already contain the climate floor. Never move them backwards or move planting before its own phase.
    eligible.filter { RootCareSchedule.isRootStep(it.programStepId) && !RootCareSchedule.isPlantingStep(it.programStepId) }
        .groupBy { programBaseStep(it.programStepId) }.values.forEach { series ->
            val first = series.minOf { LocalDate.parse(it.creationDate) }
            val days = ChronoUnit.DAYS.between(first, earliest)
            if (days > 0) series.forEach { moveForward(it, LocalDate.parse(it.creationDate).plusDays(days)) }
        }
    updated.values.firstOrNull { programBaseStep(it.programStepId) == "hydrangea_pale_acid" }?.let { acid ->
        eligible.forEach { row ->
            val days = when (programBaseStep(row.programStepId)) {
                "hydrangea_pale_iron" -> 5L
                "hydrangea_yellow_magnesium" -> 10L
                else -> return@forEach
            }
            moveForward(updated[row.id] ?: row, LocalDate.parse(acid.creationDate).plusDays(days))
        }
    }
    return updated.values.toList()
}

private fun variantStageDateUpdates(
    parent: PlantEntity,
    plants: List<PlantEntity>,
    history: List<ProcedureEntity>,
    effectiveDate: LocalDate
): List<PlantEntity> {
    val children = plants.filter { it.isVariantFollowupOf(parent) }
    if ((parent.programVersion ?: 0) >= 21 || children.any { (it.programVersion ?: 0) >= 21 }) {
        val (_, days) = parent.variantFollowupRule() ?: return emptyList()
        val protectedIds = history.map { it.plantId }.toSet()
        return children.filter { it.id !in protectedIds }.mapNotNull { child ->
            if (!child.isUnchangedVariantFollowup()) return@mapNotNull null
            val date = effectiveDate.plusDays(days).toString()
            if (date == child.creationDate) return@mapNotNull null
            child.copy(creationDate = date).let { it.copy(programBaseline = it.programFingerprint()) }
        }
    }
    val variantId = parent.selectedVariantId ?: return emptyList()
    val desired = ProgramVariants.followupFor(parent.copy(creationDate = effectiveDate.toString())) ?: return emptyList()
    val prefix = "variant-followup:${parent.id}:$variantId:"
    val protectedIds = history.map { it.plantId }.toSet()
    return plants.filter { it.programImportKey?.startsWith(prefix) == true && it.id !in protectedIds }.mapNotNull { child ->
        val untouched = child.isUnchangedVariantFollowup(desired)
        if (!untouched || child.creationDate == desired.creationDate) return@mapNotNull null
        val updated = child.copy(creationDate = desired.creationDate)
        updated.copy(programBaseline = updated.programFingerprint())
    }
}
