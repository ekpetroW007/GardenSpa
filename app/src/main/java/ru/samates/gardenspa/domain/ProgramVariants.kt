package ru.samates.gardenspa.domain

import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import ru.samates.gardenspa.data.database.entity.resolvedCardId
import java.time.LocalDate
import java.util.Base64

/** A single scheduled work owns its alternative recipes; only the chosen recipe is archived. */
object ProgramVariants {
    private const val CUSTOM_PRODUCT = "__product"
    private const val CUSTOM_NOTE = "__note"
    fun baseStep(id: String?): String = id.orEmpty().substringBefore("~").substringBefore(":remaining:")

    fun isVariantWork(plant: PlantEntity): Boolean =
        EradicationCare.applies(plant.programId.orEmpty()) &&
            ((plant.programVersion ?: 0) >= EradicationCare.FIRST_VARIANT_VERSION || plant.selectedVariantId != null) &&
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
        } + selected.filterKeys { it == CUSTOM_PRODUCT || it == CUSTOM_NOTE }
    }

    fun selectedComponents(variantId: String, encoded: String): List<EradicationVariant> {
        val selected = normalizedChoices(variantId, encoded)
        return EradicationCare.componentsFor(variantId).map { group -> group.options.single { it.id == selected[group.id] } }
    }

    private fun customText(encoded: String, key: String): String? = choices(encoded)[key]?.let {
        runCatching { String(Base64.getDecoder().decode(it), Charsets.UTF_8) }.getOrNull()?.takeIf(String::isNotBlank)
    }

    fun hasCustomRecipe(encoded: String): Boolean = customText(encoded, CUSTOM_PRODUCT) != null && customText(encoded, CUSTOM_NOTE) != null

    fun editRecipe(variantId: String, encoded: String, product: String, note: String): String {
        require(product.isNotBlank() && note.isNotBlank()) { "Заполните состав и инструкцию" }
        require(product.length <= 4000 && note.length <= 20000) { "Сократите состав или инструкцию" }
        fun encodeText(text: String) = Base64.getEncoder().encodeToString(text.trim().toByteArray(Charsets.UTF_8))
        return encode(normalizedChoices(variantId, encoded) + mapOf(CUSTOM_PRODUCT to encodeText(product), CUSTOM_NOTE to encodeText(note)))
    }

    fun originalRecipe(encoded: String): String = encode(choices(encoded) - setOf(CUSTOM_PRODUCT, CUSTOM_NOTE))

    fun validateChoices(variantId: String, encoded: String) {
        selectedComponents(variantId, encoded).forEach { require(it.unavailableReason == null) { it.unavailableReason.orEmpty() } }
    }

    fun selectedNote(variant: EradicationVariant, encoded: String): String =
        customText(encoded, CUSTOM_NOTE) ?: (selectedComponents(variant.id, encoded).map { "${it.product}\n${it.note}" } + variant.note).joinToString("\n\n")

    fun selectedProduct(variant: EradicationVariant, encoded: String): String =
        customText(encoded, CUSTOM_PRODUCT) ?: selectedComponents(variant.id, encoded).takeIf { it.isNotEmpty() }?.joinToString(" + ") { it.product }
            ?.let { if (baseStep(variant.id) == "autumn_main") "$it + Эпин, 1 мл + прилипатель, 10 мл" else it }
            ?: variant.product

    fun followupFor(plant: PlantEntity): PlantEntity? {
        if (!isVariantWork(plant) || baseStep(plant.programStepId) != EradicationCare.EARLY_STEP) return null
        val modern = (plant.programVersion ?: 0) >= 21
        val source = if (modern) PlantCareCatalog.all().firstOrNull { it.id == plant.programId }?.steps
            ?.firstOrNull { baseStep(it.id) == EradicationCare.PEST_STEP } else null
        val days = if (modern) 7 else when (plant.selectedVariantId) {
            "urea_young", "copper" -> 7
            else -> return null
        }
        return plant.copy(id = 0, creationDate = LocalDate.parse(plant.creationDate).plusDays(days.toLong()).toString(),
            taskName = source?.title ?: "Обработать от зимующих вредителей до распускания почек",
            drugId = null, drugName = source?.productDescription ?: "Профилактин — Август",
            programStepId = source?.id ?: EradicationCare.PEST_STEP, programImportKey = null,
            programNote = source?.note ?: EradicationCare.PROFILAKTIN_NOTE,
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
    ProgramVariants.validateChoices(variantId, normalized)
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
    ProgramVariants.validateChoices(variantId, normalized)
    return copy(selectedVariantId = variantId, variantChoices = normalized,
        productDescription = ProgramVariants.selectedProduct(variant, normalized),
        note = ProgramVariants.selectedNote(variant, normalized), userEdited = true)
}
