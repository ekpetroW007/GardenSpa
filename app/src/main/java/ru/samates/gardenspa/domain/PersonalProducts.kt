package ru.samates.gardenspa.domain

import java.time.LocalDate
import java.util.UUID
import ru.samates.gardenspa.data.database.entity.DrugEntity
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.resolvedCardId

val DrugEntity.careNote: String get() = listOf(purpose, consumptionRate).filter(String::isNotBlank).joinToString("\n")

fun ProgramProduct.asPersonalProduct() = DrugEntity(name = displayName, purpose = purpose,
    consumptionRate = note, applicationMethod = if (sections.size > 1) "BOTH" else sections.singleOrNull()?.name ?: "UNSPECIFIED")

fun PlantEntity.withPersonalProduct(drug: DrugEntity, date: LocalDate, reminder: Int,
    afterInspection: Boolean = false, problemId: String? = null): PlantEntity {
    require(drug.id > 0 && reminder in setOf(0, 1, 5))
    val problem = ProgramProductCatalog.problems.firstOrNull { it.id == problemId }?.label ?: "Болезнь / вредитель не указаны пользователем"
    return copy(id = if (afterInspection) 0 else id, plantCardId = resolvedCardId,
        taskName = "Применить: ${drug.name}",
        drugId = drug.id, drugName = drug.name, creationDate = date.toString(),
        programStepId = if (afterInspection) "problem:personal:${UUID.randomUUID()}" else programStepId?.let { ProgramProductCatalog.stepWithProduct(it, "personal:${drug.id}") },
        programImportKey = if (afterInspection) null else programImportKey,
        programNote = (if (afterInspection) "После осмотра: $problem.\n" else "") + drug.careNote,
        repeatType = "NONE", repeatInterval = 1, wateringInterval = 1, repeatDaysOfWeek = "",
        repeatEndType = "NEVER", repeatEndDate = null, repeatCount = null,
        reminderDaysBefore = reminder, userLockedDate = true)
}

fun GeneratedCareProgram.withPersonalProduct(index: Int?, drug: DrugEntity, date: LocalDate,
    reminder: Int, problemId: String? = null): GeneratedCareProgram {
    require(!date.isBefore(chosenStartDate) && reminder in setOf(0, 1, 5))
    val previous = index?.let { steps[it] }
    val problem = ProgramProductCatalog.problems.firstOrNull { it.id == problemId }?.label ?: "Болезнь / вредитель не указаны пользователем"
    val added = GeneratedCareStep(
        templateStepId = previous?.templateStepId?.let { ProgramProductCatalog.stepWithProduct(it, "personal:${drug.id}") }
            ?: "problem:personal:${UUID.randomUUID()}",
        title = "Применить: ${drug.name}", scheduledDate = date, windowStart = date, windowEnd = date,
        recurrence = null, weatherAdjusted = false, needsWeatherConfirmation = true,
        explanation = "Выбрано собственное средство. Условия применения — по его инструкции.",
        productDescription = drug.name, note = (if (index == null) "После осмотра: $problem.\n" else "") + drug.careNote,
        reminderDaysBefore = reminder)
    return copy(steps = if (index == null) steps + added else steps.toMutableList().also { it[index] = added })
}
