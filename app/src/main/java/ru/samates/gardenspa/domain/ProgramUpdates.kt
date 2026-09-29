package ru.samates.gardenspa.domain

import ru.samates.gardenspa.data.database.entity.GardenEntity
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import ru.samates.gardenspa.data.database.entity.ProgramInstallationEntity
import ru.samates.gardenspa.data.database.entity.climateOrNull
import ru.samates.gardenspa.data.database.entity.resolvedCardId
import java.security.MessageDigest
import java.time.LocalDate

internal fun programBaseStep(id: String?): String = id.orEmpty().substringBefore("~").substringBefore(":remaining:")
    .let { if (it == "after_leaf_fall_iron") "after_leaf_fall" else it }

private fun programOccurrenceKey(id: String?): String = programBaseStep(id) +
    id.orEmpty().substringAfter(":remaining:", "").let { if (it.isBlank()) "" else ":remaining:$it" }

fun PlantEntity.programFingerprint(): String {
    val values = listOf(taskName, creationDate, drugName, programNote, repeatType, repeatInterval,
        repeatDaysOfWeek, repeatEndType, repeatEndDate, repeatCount, reminderDaysBefore, programStepId)
    val serialized = values.joinToString("") { value -> value.toString().let { "${it.length}:$it" } }
    return MessageDigest.getInstance("SHA-256").digest(serialized.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

fun GeneratedCareProgram.installedRows(
    gardenId: Int?, gardenName: String, reminderDaysBefore: Int = 1, photoUri: String? = null
): List<PlantEntity> {
    val template = PlantCareCatalog.all().firstOrNull { it.id == templateId }
    return steps.map { step ->
        val repeat = step.recurrence
        val source = template?.steps?.firstOrNull { programBaseStep(it.id) == programBaseStep(step.templateStepId) }
        val customized = source != null && (step.title != source.title || step.note != source.note ||
            step.productDescription != source.productDescription ||
            (!step.templateStepId.contains(":remaining:") && repeat != source.recurrence) ||
            step.reminderDaysBefore != null || step.scheduledDate.isBefore(step.windowStart) || step.scheduledDate.isAfter(step.windowEnd))
        PlantEntity(
            plantName = plantName, taskName = step.title, wateringInterval = repeat?.interval ?: 1,
            creationDate = step.scheduledDate.toString(), drugId = null, gardenId = gardenId,
            drugName = step.productDescription ?: NO_DRUG_REQUIRED_LABEL, gardenName = gardenName,
            repeatType = repeat?.type?.name ?: RepeatType.NONE.name, repeatInterval = repeat?.interval ?: 1,
            repeatDaysOfWeek = if (repeat?.type == RepeatType.WEEKLY) step.scheduledDate.dayOfWeek.value.toString() else "",
            repeatEndType = if (repeat?.count == null) "NEVER" else "COUNT", repeatCount = repeat?.count,
            reminderDaysBefore = step.reminderDaysBefore ?: reminderDaysBefore,
            plantCardId = instanceId, programId = templateId, programVersion = templateVersion,
            programStepId = step.templateStepId,
            programImportKey = "$instanceId:$templateId:v$templateVersion:${step.templateStepId}",
            programNote = listOfNotNull(step.productDescription?.let { "Категория средства: $it" }, step.note, step.explanation)
                .filter(String::isNotBlank).joinToString("\n"),
            photoUri = photoUri, userEdited = customized || step.userEdited,
            selectedVariantId = step.selectedVariantId, variantChoices = step.variantChoices
        ).let { it.copy(programBaseline = it.programFingerprint()) }
    }
}

fun GeneratedCareProgram.installation(): ProgramInstallationEntity = ProgramInstallationEntity(
    plantCardId = instanceId, templateId = templateId, appliedVersion = templateVersion,
    startDate = chosenStartDate.toString(), cultivationType = cultivationType.name,
    southernRegion = isSouthernRegion,
    knownStepIds = PlantCareCatalog.all().first { it.id == templateId }.steps
        .map { programBaseStep(it.id) }.distinct().sorted().joinToString("\n")
)

data class ProgramUpdatePlan(
    val installation: ProgramInstallationEntity,
    val updates: List<PlantEntity>,
    val additions: List<PlantEntity>,
    val removals: List<Int>
)

object ProgramUpdates {
    fun plan(
        rows: List<PlantEntity>, history: List<ProcedureEntity>, garden: GardenEntity?,
        installation: ProgramInstallationEntity?, today: LocalDate = LocalDate.now(),
        templates: List<PlantCareTemplate> = PlantCareCatalog.all(),
        previousTemplates: List<PlantCareTemplate> = PlantCareCatalog.previousAll()
    ): ProgramUpdatePlan? {
        val representative = rows.firstOrNull { it.programId != null } ?: return null
        val template = templates.firstOrNull { it.id == representative.programId } ?: return null
        val previous = previousTemplates.firstOrNull { it.id == template.id } ?: template
        val climate = garden?.climateOrNull() ?: return null
        if (installation != null && installation.appliedVersion >= template.version) return null
        val imported = rows.filter { it.programId == template.id && it.programImportKey?.startsWith("${it.resolvedCardId}:${template.id}:v") == true }
        if (imported.isEmpty()) return null
        val legacy = installation == null || installation.inferredContext
        val context = installation ?: legacyInstallation(imported, previous, climate, today)
        val cultivation = runCatching { CultivationType.valueOf(context.cultivationType) }.getOrNull() ?: return null
        val generated = CareProgramGenerator().generate(template, CareProgramContext(
            startDate = LocalDate.parse(context.startDate), cultivationType = cultivation, climate = climate,
            isSouthernRegion = context.southernRegion
        ), context.plantCardId)
        val candidates = generated.installedRows(representative.gardenId, representative.gardenName,
            representative.reminderDaysBefore, representative.photoUri).map { it.copy(plantName = representative.plantName) }
        val historyIds = history.map { it.plantId }.toSet()
        val updates = mutableListOf<PlantEntity>()
        val removals = mutableListOf<Int>()
        imported.forEach { current ->
            if (current.userEdited || current.userLockedDate || current.selectedVariantId != null || current.id in historyIds) return@forEach
            val date = runCatching { LocalDate.parse(current.creationDate) }.getOrNull() ?: return@forEach
            if (date.isBefore(today)) return@forEach
            val unchanged = current.programBaseline?.let { it == current.programFingerprint() }
                ?: previous.steps.firstOrNull { programBaseStep(it.id) == programBaseStep(current.programStepId) }
                    ?.let { matchesLegacyDefault(current, it) } ?: false
            if (!unchanged) return@forEach
            val replacement = candidates.firstOrNull { programOccurrenceKey(it.programStepId) == programOccurrenceKey(current.programStepId) }
            if (replacement == null) {
                if (template.steps.none { programBaseStep(it.id) == programBaseStep(current.programStepId) }) removals += current.id
                return@forEach
            }
            // Legacy dates keep their season year. Only explicit climate anchors can be
            // recalculated without recovering the original start/cultivation input.
            val targetStep = template.steps.firstOrNull { programBaseStep(it.id) == programBaseStep(current.programStepId) }
            val updatedDate = if (!legacy) replacement.creationDate else if (targetStep?.anchor != CareAnchor.START_DATE) {
                LocalDate.parse(replacement.creationDate).withYear(date.year).toString()
            } else current.creationDate
            val updated = replacement.copy(id = current.id, creationDate = updatedDate,
                gardenId = current.gardenId, gardenName = current.gardenName, plantName = current.plantName,
                photoUri = current.photoUri, reminderDaysBefore = current.reminderDaysBefore, userEdited = false)
            updates += updated.copy(programBaseline = updated.programFingerprint())
        }
        val known = context.knownStepIds.lineSequence().filter(String::isNotBlank).toSet()
        val existing = rows.map { programBaseStep(it.programStepId) }.toSet()
        val additions = candidates.filter { programBaseStep(it.programStepId) !in known && programBaseStep(it.programStepId) !in existing }
            .map { row ->
                val oldDate = LocalDate.parse(row.creationDate)
                var date = oldDate.withYear(maxOf(oldDate.year, today.year))
                if (date.isBefore(today)) date = oldDate.withYear(date.year + 1)
                row.copy(creationDate = date.toString(), userEdited = false).let { it.copy(programBaseline = it.programFingerprint()) }
            }
        val allKnown = (known + template.steps.map { programBaseStep(it.id) }).sorted().joinToString("\n")
        return ProgramUpdatePlan(context.copy(appliedVersion = template.version, knownStepIds = allKnown), updates, additions, removals)
    }

    private fun legacyInstallation(rows: List<PlantEntity>, template: PlantCareTemplate, climate: ClimateFingerprint, today: LocalDate): ProgramInstallationEntity {
        val first = rows.first()
        val cultivation = if (template.supportedCultivationTypes.size == 1) template.supportedCultivationTypes.single() else CultivationType.OPEN_GROUND
        val dates = rows.mapNotNull { row -> runCatching { LocalDate.parse(row.creationDate) }.getOrNull() }
        val year = dates.minOrNull()?.year ?: today.year
        val start = recommendedStartDate(template, cultivation, climate, year)
        return ProgramInstallationEntity(first.resolvedCardId, template.id, rows.maxOf { it.programVersion ?: 0 }, start.toString(),
            cultivation.name, template.id == "hydrangea-macrophylla" && rows.none { programBaseStep(it.programStepId) == "hydrangea_winter_cover" },
            template.steps.map { programBaseStep(it.id) }.distinct().sorted().joinToString("\n"), inferredContext = true)
    }

    private fun matchesLegacyDefault(row: PlantEntity, step: CareStepTemplate): Boolean {
        if (row.taskName != step.title || row.drugName != (step.productDescription ?: NO_DRUG_REQUIRED_LABEL)) return false
        if (row.programStepId.orEmpty().substringBefore(":remaining:") != step.id) return false
        val prefix = listOfNotNull(step.productDescription?.let { "Категория средства: $it" }, step.note).filter(String::isNotBlank).joinToString("\n")
        if (!row.programNote.startsWith(prefix)) return false
        val explanation = row.programNote.removePrefix(prefix).trim()
        val standardExplanations = setOf("",
            "Дата рассчитана по программе; прогноз будет полезно проверить ближе к сроку.",
            "Дата рассчитана относительно начала ухода.",
            "Дата рассчитана относительно безопасного начала сезона.",
            "В доступном прогнозе нет полностью подходящего дня; проверьте погоду перед выполнением.",
            "Цепочка осмотров перенесена целиком с сохранением интервалов 5 дней.")
        val date = "\\d{4}-\\d{2}-\\d{2}"
        val datedExplanations = listOf(
            "Прогноз на $date соответствует условиям выполнения\\.",
            "Дата сдвинута с $date на $date по прогнозу погоды\\.",
            "Пропущенная работа с $date перенесена на $date\\. Проверьте фазу развития растения и погоду ближе к сроку\\.",
            "Срок работы ещё не завершился; при продолжении программы она назначена на $date\\.")
        if (explanation !in standardExplanations && datedExplanations.none { Regex(it).matches(explanation) }) return false
        val recurrence = if (row.programStepId.orEmpty().contains(":remaining:")) null else step.recurrence
        return row.repeatType == (recurrence?.type?.name ?: "NONE") && row.repeatInterval == (recurrence?.interval ?: 1) &&
            row.repeatCount == recurrence?.count && row.repeatEndType == if (recurrence?.count == null) "NEVER" else "COUNT"
    }
}
