package ru.samates.gardenspa.domain

import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import java.time.LocalDate

internal fun PlantEntity.effectiveVariantDate(history: List<ProcedureEntity>): LocalDate = LocalDate.parse(
    history.firstOrNull { it.plantId == id && it.scheduledDate == creationDate }?.rescheduledDate ?: creationDate
)

internal fun PlantEntity.isUnchangedVariantFollowup(expected: PlantEntity? = null): Boolean =
    programBaseline?.let { it == programFingerprint() } ?: (expected != null &&
        creationDate == programImportKey?.substringAfterLast(':') && taskName == expected.taskName &&
        drugName == expected.drugName && programNote == expected.programNote && repeatType == "NONE")

/** Only an untouched automatic child follows its parent; its own history or edits take priority. */
internal fun variantFollowupDateUpdates(
    parent: PlantEntity,
    plants: List<PlantEntity>,
    history: List<ProcedureEntity>,
    effectiveDate: LocalDate
): List<PlantEntity> {
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
