package ru.samates.gardenspa

import org.junit.Assert.*
import org.junit.Test
import ru.samates.gardenspa.data.database.entity.*
import ru.samates.gardenspa.domain.*
import java.time.LocalDate

class ProgramUpdatesTest {
    private val today = LocalDate.of(2026, 4, 1)
    private val garden = GardenEntity(id = 1, name = "Сад", climateSafeSpringDay = "--04-25",
        climateSafeAutumnDay = "--10-10", climateFrostFreeDays = 168, climateGdd5 = 2600.0,
        climateGdd10 = 1750.0, climateWarmPrecipitation = 420.0, climateWinterMinimumP10 = -18.0,
        climateConfidence = "HIGH", climateSourceYears = 20)
    private val old = PlantCareTemplate("update-test", "Растение", emptySet(), 1, setOf(CultivationType.OPEN_GROUND),
        steps = listOf("normal", "changed", "deleted", "done", "obsolete").map {
            CareStepTemplate(it, "Работа $it", offsetDays = 10, productDescription = "Препарат", note = "Старая инструкция")
        })
    private val current = old.copy(version = 2, steps = old.steps.filterNot { it.id == "obsolete" }
        .map { it.copy(note = "Новая инструкция") } + CareStepTemplate("new-step", "Новая работа", offsetDays = 20, note = "Новая инструкция"))
    private fun originalRows() = CareProgramGenerator().generate(old,
        CareProgramContext(today, CultivationType.OPEN_GROUND, requireNotNull(garden.climateOrNull())), "card")
        .installedRows(1, "Сад").mapIndexed { index, plant -> plant.copy(id = index + 1) }
    private fun installation() = ProgramInstallationEntity("card", old.id, 1, today.toString(), "OPEN_GROUND", false,
        old.steps.joinToString("\n") { it.id })

    @Test fun updateAddsOnlyNewStepsAndPreservesEditsDeletedStepsHistoryAndCustomWorks() {
        val original = originalRows()
        val rows = original.filterNot { it.programStepId == "deleted" }.map {
            if (it.programStepId == "changed") it.copy(programNote = "Пользовательская инструкция") else it
        } + original.first().copy(id = 99, taskName = "Моя работа", programStepId = "problem:user", programImportKey = null, userEdited = true)
        val done = original.first { it.programStepId == "done" }
        val history = listOf(ProcedureEntity(id = 1, plantId = done.id, procedureName = done.taskName,
            scheduledDate = done.creationDate, completedDate = done.creationDate, status = "COMPLETED"))
        val update = requireNotNull(ProgramUpdates.plan(rows, history, garden, installation(), today, listOf(current), listOf(old)))
        assertEquals(listOf("normal"), update.updates.map { it.programStepId })
        assertTrue(update.updates.single().programNote.contains("Новая инструкция"))
        assertEquals(listOf("new-step"), update.additions.map { it.programStepId })
        assertEquals(listOf(original.first { it.programStepId == "obsolete" }.id), update.removals)
        assertTrue(update.installation.knownStepIds.contains("deleted"))
        assertNull(ProgramUpdates.plan(rows, history, garden, update.installation, today, listOf(current), listOf(old)))
    }

    @Test fun legacyRowsMustMatchKnownDefaultsAndMissingClimateDefersUpdate() {
        val rows = originalRows().map { it.copy(programBaseline = null) }
        val changed = rows.map { if (it.programStepId == "changed") it.copy(drugName = "Личный препарат") else it }
        val update = requireNotNull(ProgramUpdates.plan(changed, emptyList(), garden, null, today, listOf(current), listOf(old)))
        assertFalse(update.updates.any { it.programStepId == "changed" })
        assertTrue(update.updates.any { it.programStepId == "normal" })
        assertEquals(rows.first().creationDate, update.updates.first().creationDate)
        assertNull(ProgramUpdates.plan(rows, emptyList(), garden.copy(climateSafeSpringDay = null), null, today, listOf(current), listOf(old)))
    }

    @Test fun lockedRowsAndRenamedDeletedAutumnStepAreNotReintroduced() {
        val source = old.copy(steps = listOf(CareStepTemplate("after_leaf_fall_iron~autumn_iron", "Железный купорос",
            CareAnchor.SAFE_AUTUMN_DATE, 14, note = "Ранняя инструкция"), old.steps.first()))
        val updated = source.copy(version = 2, steps = listOf(source.steps.first().copy(id = "after_leaf_fall", offsetDays = 15), source.steps.last()))
        val row = originalRows().first().copy(userLockedDate = true)
        val metadata = installation().copy(knownStepIds = "after_leaf_fall\nnormal")
        val plan = requireNotNull(ProgramUpdates.plan(listOf(row), emptyList(), garden, metadata, today, listOf(updated), listOf(source)))
        assertTrue(plan.updates.isEmpty())
        assertTrue(plan.additions.isEmpty())
    }
}
