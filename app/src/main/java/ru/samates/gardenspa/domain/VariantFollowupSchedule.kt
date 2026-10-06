package ru.samates.gardenspa.domain

import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import ru.samates.gardenspa.data.database.entity.resolvedCardId
import java.time.LocalDate

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
