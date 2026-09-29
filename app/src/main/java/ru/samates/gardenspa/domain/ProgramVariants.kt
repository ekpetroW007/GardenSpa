package ru.samates.gardenspa.domain

import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import ru.samates.gardenspa.data.database.entity.resolvedCardId
import java.time.LocalDate

/** A single scheduled work owns its alternative recipes; only the chosen recipe is archived. */
object ProgramVariants {
    fun baseStep(id: String?): String = id.orEmpty().substringBefore("~").substringBefore(":remaining:")

    fun isVariantWork(plant: PlantEntity): Boolean =
        EradicationCare.applies(plant.programId.orEmpty()) &&
            ((plant.programVersion ?: 0) >= EradicationCare.PROGRAM_VERSION || plant.selectedVariantId != null) &&
            EradicationCare.variantsFor(baseStep(plant.programStepId)).isNotEmpty()

    fun choices(encoded: String): Map<String, String> = encoded.split('|').mapNotNull {
        val parts = it.split('=', limit = 2)
        if (parts.size == 2) parts[0] to parts[1] else null
    }.toMap()

    fun encode(choices: Map<String, String>): String = choices.toSortedMap().entries.joinToString("|") { "${it.key}=${it.value}" }

    fun normalizedChoices(variantId: String, encoded: String): Map<String, String> {
        val selected = choices(encoded)
        return EradicationCare.componentsFor(variantId).associate { group ->
            group.id to (group.options.firstOrNull { it.id == selected[group.id] } ?: group.options.first()).id
        }
    }

    fun selectedComponents(variantId: String, encoded: String): List<EradicationVariant> {
        val selected = normalizedChoices(variantId, encoded)
        return EradicationCare.componentsFor(variantId).map { group -> group.options.single { it.id == selected[group.id] } }
    }

    fun selectedNote(variant: EradicationVariant, encoded: String): String =
        (listOf(variant.note) + selectedComponents(variant.id, encoded).map { "${it.product}\n${it.note}" }).joinToString("\n\n")

    fun selectedProduct(variant: EradicationVariant, encoded: String): String =
        selectedComponents(variant.id, encoded).takeIf { it.isNotEmpty() }?.joinToString(" + ") { it.product }
            ?.let { if (baseStep(variant.id) == "autumn_main") "$it + Эпин, 1 мл + прилипатель, 10 мл" else it }
            ?: variant.product

    fun followupFor(plant: PlantEntity): PlantEntity? {
        if (!isVariantWork(plant)) return null
        val variant = EradicationCare.variantsFor(baseStep(plant.programStepId)).firstOrNull { it.id == plant.selectedVariantId } ?: return null
        val days = variant.followupDays ?: return null
        return plant.copy(id = 0, creationDate = LocalDate.parse(plant.creationDate).plusDays(days.toLong()).toString(),
            taskName = "Обработать от зимующих вредителей до распускания почек",
            drugId = null, drugName = "Профилактин — Август",
            programStepId = "before_bud_break_followup", programImportKey = null,
            programNote = EradicationCare.PROFILAKTIN_NOTE,
            selectedVariantId = null, variantChoices = "", repeatType = "NONE", repeatInterval = 1,
            wateringInterval = 1, repeatCount = null, repeatEndDate = null, repeatEndType = "NEVER", repeatDaysOfWeek = "",
            userLockedDate = true, userEdited = true, programBaseline = null)
    }

    fun completedYears(plant: PlantEntity, history: List<ProcedureEntity>): Map<String, List<Int>> = history
        .filter { it.status == "COMPLETED" && it.plantCardId == plant.resolvedCardId &&
            baseStep(it.programStepId) == baseStep(plant.programStepId) && it.selectedVariantId != null }
        .groupBy { requireNotNull(it.selectedVariantId) }
        .mapValues { (_, rows) -> rows.mapNotNull { row ->
            runCatching { LocalDate.parse(row.completedDate ?: row.scheduledDate).year }.getOrNull()
        }.distinct().sortedDescending() }
}

fun PlantEntity.withProgramVariant(variantId: String, choices: String): PlantEntity {
    require(ProgramVariants.isVariantWork(this)) { "Эта работа не содержит вариантов" }
    val variant = EradicationCare.variantsFor(ProgramVariants.baseStep(programStepId)).singleOrNull { it.id == variantId }
        ?: error("Вариант не найден")
    val normalized = ProgramVariants.encode(ProgramVariants.normalizedChoices(variantId, choices))
    val baseTitle = PlantCareCatalog.all().first { it.id == programId }.steps
        .first { ProgramVariants.baseStep(it.id) == ProgramVariants.baseStep(programStepId) }.title
    return copy(selectedVariantId = variantId, variantChoices = normalized, drugId = null,
        drugName = ProgramVariants.selectedProduct(variant, normalized),
        taskName = "$baseTitle · ${variant.title}", programNote = ProgramVariants.selectedNote(variant, normalized),
        userEdited = true)
}

fun PlantEntity.resetProgramVariantForNextSeason(): PlantEntity {
    if (!ProgramVariants.isVariantWork(this)) return this
    val template = PlantCareCatalog.all().firstOrNull { it.id == programId } ?: return this
    val step = template.steps.firstOrNull { ProgramVariants.baseStep(it.id) == ProgramVariants.baseStep(programStepId) } ?: return this
    return copy(taskName = step.title, drugId = null, drugName = step.productDescription.orEmpty(),
        programNote = step.note, selectedVariantId = null, variantChoices = "", programVersion = template.version,
        programStepId = step.id, programBaseline = null)
}

fun GeneratedCareStep.withProgramVariant(variantId: String, choices: String): GeneratedCareStep {
    val variant = EradicationCare.variantsFor(ProgramVariants.baseStep(templateStepId)).single { it.id == variantId }
    val normalized = ProgramVariants.encode(ProgramVariants.normalizedChoices(variantId, choices))
    return copy(selectedVariantId = variantId, variantChoices = normalized,
        productDescription = ProgramVariants.selectedProduct(variant, normalized),
        note = ProgramVariants.selectedNote(variant, normalized), userEdited = true)
}
