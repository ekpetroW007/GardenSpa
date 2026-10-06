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

    @Test fun newWhitewashUsesThePreservedIronDateIncludingARecordedPostponement() {
        val source = PlantCareTemplate("apple", "Яблоня", emptySet(), 20, setOf(CultivationType.OPEN_GROUND),
            steps = listOf(CareStepTemplate(EradicationCare.AUTUMN_IRON_STEP, "Осенняя обработка",
                CareAnchor.SAFE_AUTUMN_DATE, 15, note = "Старая инструкция")))
        val next = source.copy(version = 21, steps = source.steps + CareStepTemplate(
            "${EradicationCare.WHITEWASH_STEP}~eradication_whitewash_doctor", "Побелка деревьев",
            CareAnchor.AUTUMN_ERADICATION_DATE, 14, note = "Через две недели после обработки"))
        val metadata = ProgramInstallationEntity("iron-card", "apple", 20, today.toString(), "OPEN_GROUND", false,
            EradicationCare.AUTUMN_IRON_STEP)
        for (postponed in listOf(false, true)) {
            val actualDate = LocalDate.of(2026, 11, if (postponed) 19 else 2)
            val parent = PlantEntity(id = 55, plantName = "Яблоня", taskName = "Осенняя обработка", wateringInterval = 1,
                creationDate = if (postponed) "2026-10-25" else actualDate.toString(), drugId = null,
                gardenId = garden.id, drugName = "Железный купорос", gardenName = garden.name,
                plantCardId = metadata.plantCardId, programId = "apple", programVersion = 20,
                programStepId = EradicationCare.AUTUMN_IRON_STEP,
                programImportKey = "iron-card:apple:v20:${EradicationCare.AUTUMN_IRON_STEP}",
                userEdited = !postponed, userLockedDate = !postponed)
            val records = if (postponed) listOf(ProcedureEntity(id = 101, plantId = parent.id,
                procedureName = parent.taskName, scheduledDate = parent.creationDate,
                rescheduledDate = actualDate.toString(), status = "PLANNED")) else emptyList()
            val plan = requireNotNull(ProgramUpdates.plan(listOf(parent), records, garden, metadata, today,
                listOf(next), listOf(source)))
            val added = plan.additions.single()
            assertEquals(EradicationCare.WHITEWASH_STEP, programBaseStep(added.programStepId))
            assertEquals("postponed=$postponed", actualDate.plusDays(14).toString(), added.creationDate)
            assertEquals(added.programFingerprint(), added.programBaseline)
            assertTrue(plan.updates.isEmpty())
            assertTrue(plan.removals.isEmpty())
        }
    }

    @Test fun newStandalonePestWorkFollowsAnAlreadyChosenMatureUreaDate() {
        val source = PlantCareTemplate("apple", "Яблоня", emptySet(), 20, setOf(CultivationType.OPEN_GROUND),
            steps = listOf(CareStepTemplate(EradicationCare.EARLY_STEP, "Весенняя обработка",
                CareAnchor.DORMANT_SPRING_DATE, 0, note = "Старая инструкция")))
        val next = source.copy(version = 21, steps = source.steps + CareStepTemplate(
            "${EradicationCare.PEST_STEP}~spring_profilaktin", "От зимующих вредителей",
            CareAnchor.DORMANT_SPRING_DATE, 7, note = "Через неделю после обработки"))
        val metadata = ProgramInstallationEntity("spring-card", "apple", 20, today.toString(), "OPEN_GROUND", false,
            EradicationCare.EARLY_STEP)
        val actualDate = LocalDate.of(2026, 4, 12)
        val parent = PlantEntity(id = 56, plantName = "Яблоня", taskName = "Мочевина — взрослые растения",
            wateringInterval = 1, creationDate = actualDate.toString(), drugId = null, gardenId = garden.id,
            drugName = "Мочевина — 500–700 г", gardenName = garden.name, plantCardId = metadata.plantCardId,
            programId = "apple", programVersion = 20, programStepId = EradicationCare.EARLY_STEP,
            programImportKey = "spring-card:apple:v20:${EradicationCare.EARLY_STEP}",
            selectedVariantId = "urea_mature", userEdited = true)
        val plan = requireNotNull(ProgramUpdates.plan(listOf(parent), emptyList(), garden, metadata, today,
            listOf(next), listOf(source)))
        val added = plan.additions.single()
        assertEquals(EradicationCare.PEST_STEP, programBaseStep(added.programStepId))
        assertEquals(actualDate.plusDays(7).toString(), added.creationDate)
        assertEquals(added.programFingerprint(), added.programBaseline)
        assertTrue(plan.updates.isEmpty())
        assertTrue(plan.removals.isEmpty())
    }
}
