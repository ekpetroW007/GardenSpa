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

    @Test fun catalogRenamesRefreshOverdueAndPostponedWorkWithoutMovingDatesOrChangingArchives() {
        val rows = originalRows()
        val postponed = rows.first { it.programStepId == "normal" }
        val completed = rows.first { it.programStepId == "done" }
        val records = listOf(
            ProcedureEntity(id = 201, plantId = postponed.id, procedureName = postponed.taskName,
                scheduledDate = postponed.creationDate, rescheduledDate = "2026-06-15", status = "PLANNED"),
            ProcedureEntity(id = 202, plantId = completed.id, procedureName = completed.taskName,
                scheduledDate = completed.creationDate, completedDate = completed.creationDate,
                drugName = completed.drugName, note = completed.programNote, status = "COMPLETED")
        )
        val renamed = current.copy(steps = current.steps.map {
            it.copy(title = "Новое имя работы", productDescription = "Новое имя средства")
        })
        val plan = requireNotNull(ProgramUpdates.plan(rows, records, garden, installation(),
            LocalDate.of(2026, 5, 1), listOf(renamed), listOf(old)))
        val updated = plan.updates.first { it.id == postponed.id }
        assertEquals("Новое имя средства", updated.drugName)
        assertEquals("Новое имя работы", updated.taskName)
        assertEquals(postponed.creationDate, updated.creationDate)
        val scheduled = scheduledTreatmentsOn(listOf(updated), records, LocalDate.of(2026, 6, 15)).single()
        assertEquals("Новое имя средства", scheduled.plant.drugName)
        assertTrue(scheduled.rescheduled)
        assertEquals(rows.first { it.programStepId == "changed" }.creationDate,
            plan.updates.first { it.programStepId == "changed" }.creationDate)
        assertFalse(plan.updates.any { it.id == completed.id })
        assertFalse(completed.id in plan.removals)
    }

    @Test fun existingWhitewashRefreshKeepsTheActualParentDateAndRespectsManualChildChanges() {
        val source = PlantCareTemplate("apple", "Яблоня", emptySet(), 21, setOf(CultivationType.OPEN_GROUND), steps = listOf(
            CareStepTemplate(EradicationCare.AUTUMN_IRON_STEP, "Осенняя искореняющая обработка",
                CareAnchor.AUTUMN_ERADICATION_DATE, 0, productDescription = "Железный купорос — 300 г на 10 л воды", note = "Старая инструкция"),
            CareStepTemplate(EradicationCare.WHITEWASH_STEP, "Побелка деревьев", CareAnchor.AUTUMN_ERADICATION_DATE, 14,
                productDescription = "Побелка", note = "Старая инструкция")))
        val next = source.copy(version = 22, steps = source.steps.map { it.copy(note = "Новая инструкция") })
        val metadata = ProgramInstallationEntity("whitewash-card", "apple", 21, today.toString(), "OPEN_GROUND", false,
            source.steps.joinToString("\n") { it.id })
        val original = CareProgramGenerator().generate(source,
            CareProgramContext(today, CultivationType.OPEN_GROUND, requireNotNull(garden.climateOrNull())), metadata.plantCardId)
            .installedRows(garden.id, garden.name).mapIndexed { index, row -> row.copy(id = index + 1, userEdited = false) }
        val parent = original.first().copy(creationDate = "2026-12-25", userEdited = true, userLockedDate = true)
        val child = original.last()
        val plan = requireNotNull(ProgramUpdates.plan(listOf(parent, child), emptyList(), garden, metadata, today,
            listOf(next), listOf(source)))
        val updatedChild = plan.updates.single { it.id == child.id }
        assertEquals("2027-01-08", updatedChild.creationDate)
        assertTrue(updatedChild.programNote.contains("Новая инструкция"))
        val updatedParent = plan.updates.single { it.id == parent.id }
        assertEquals(EradicationCare.ironRecipe.name, updatedParent.taskName)
        assertEquals(EradicationCare.ironRecipe.name, updatedParent.drugName)
        assertEquals(parent.creationDate, updatedParent.creationDate)
        assertEquals(parent.programNote, updatedParent.programNote)
        assertTrue(updatedParent.userEdited)

        val manualChild = child.copy(creationDate = "2026-12-31", programNote = "Мой состав", userEdited = true)
        val manual = requireNotNull(ProgramUpdates.plan(listOf(parent, manualChild), emptyList(), garden, metadata, today,
            listOf(next), listOf(source)))
        assertFalse(manual.updates.any { it.id == child.id })
        assertTrue(manual.additions.isEmpty())
    }

    @Test fun knownAutumnLabelIsDeduplicatedWithoutReplacingTheChosenPersonalRecipe() {
        val source = PlantCareTemplate("apple", "Яблоня", emptySet(), 21, setOf(CultivationType.OPEN_GROUND), steps = listOf(
            CareStepTemplate(EradicationCare.AUTUMN_MIX_STEP, "Осенняя основная обработка — выбрать смесь",
                offsetDays = 0, productDescription = "Смесь", note = "Инструкция")))
        val next = source.copy(version = 22)
        val metadata = ProgramInstallationEntity("personal-card", "apple", 21, today.toString(), "OPEN_GROUND", false,
            EradicationCare.AUTUMN_MIX_STEP)
        val choices = ProgramVariants.editRecipe("autumn_main", "", "Мой состав = 7 мл | 10 л", "Моя инструкция\nВторая строка")
        val row = PlantEntity(id = 57, plantName = "Яблоня", taskName =
            "Осенняя основная обработка — выбрать смесь · Осенняя основная обработка", wateringInterval = 14,
            creationDate = "2026-09-10", drugId = null, gardenId = garden.id, drugName = "Мой состав = 7 мл | 10 л",
            gardenName = garden.name, plantCardId = metadata.plantCardId, programId = "apple", programVersion = 21,
            programStepId = EradicationCare.AUTUMN_MIX_STEP, programImportKey = "personal-card:apple:v21:${EradicationCare.AUTUMN_MIX_STEP}",
            selectedVariantId = "autumn_main", variantChoices = choices, userEdited = true, userLockedDate = true,
            programNote = "Моя инструкция\nВторая строка", repeatType = "CUSTOM", repeatInterval = 14)
        val plan = requireNotNull(ProgramUpdates.plan(listOf(row), emptyList(), garden, metadata, today, listOf(next), listOf(source)))
        assertEquals(listOf(row.copy(taskName = EradicationCare.autumnRecipe.name)), plan.updates)
        assertTrue(plan.additions.isEmpty())
        assertTrue(plan.removals.isEmpty())
        val ownTitle = row.copy(taskName = "Моя осенняя процедура")
        assertTrue(requireNotNull(ProgramUpdates.plan(listOf(ownTitle), emptyList(), garden, metadata, today,
            listOf(next), listOf(source))).updates.isEmpty())
        val archived = ProcedureEntity(id = 203, plantId = row.id, procedureName = row.taskName, scheduledDate = row.creationDate,
            completedDate = row.creationDate, drugName = row.drugName, note = row.programNote, status = "COMPLETED")
        assertTrue(requireNotNull(ProgramUpdates.plan(listOf(row), listOf(archived), garden, metadata, today,
            listOf(next), listOf(source))).updates.isEmpty())
    }

    @Test fun namesRefreshWithoutGardenAndFullUpdateWaitsForClimate() {
        val row = originalRows().first()
        val renamedTemplate = current.copy(steps = current.steps.map {
            if (it.id == row.programStepId) it.copy(title = "Новое название работы", productDescription = "Новое название средства") else it
        })
        val names = requireNotNull(ProgramUpdates.plan(listOf(row), emptyList(), null, installation(), today,
            listOf(renamedTemplate), listOf(old)))
        val renamed = names.updates.single()
        assertEquals("Новое название работы", renamed.taskName)
        assertEquals("Новое название средства", renamed.drugName)
        assertEquals(row.creationDate, renamed.creationDate)
        assertEquals(row.programNote, renamed.programNote)
        assertEquals(row.programVersion, renamed.programVersion)
        assertEquals(installation(), names.installation)
        assertTrue(names.additions.isEmpty())
        assertTrue(names.removals.isEmpty())
        assertNull(ProgramUpdates.plan(listOf(renamed), emptyList(), null, names.installation, today,
            listOf(renamedTemplate), listOf(old)))
        val complete = requireNotNull(ProgramUpdates.plan(listOf(renamed), emptyList(), garden, names.installation, today,
            listOf(renamedTemplate), listOf(old)))
        assertEquals(renamedTemplate.version, complete.installation.appliedVersion)
        assertTrue(complete.updates.single().programNote.contains("Новая инструкция"))
        assertEquals(listOf("new-step"), complete.additions.map { it.programStepId })
    }

    @Test fun personalProductMatchingAnOldCatalogNameIsNeverRenamedWithOrWithoutClimate() {
        val source = PlantCareTemplate("apple", "Яблоня", emptySet(), 21, setOf(CultivationType.OPEN_GROUND), steps = listOf(
            CareStepTemplate(EradicationCare.AUTUMN_IRON_STEP, "Осенняя искореняющая обработка", offsetDays = 0,
                productDescription = "Железный купорос — 300 г на 10 л воды", note = "Моя инструкция")))
        val next = source.copy(version = 22, steps = source.steps.map {
            it.copy(title = EradicationCare.ironRecipe.name, productDescription = EradicationCare.ironRecipe.name)
        })
        val metadata = ProgramInstallationEntity("personal-iron", "apple", 21, today.toString(), "OPEN_GROUND", false,
            EradicationCare.AUTUMN_IRON_STEP)
        val original = CareProgramGenerator().generate(source,
            CareProgramContext(today, CultivationType.OPEN_GROUND, requireNotNull(garden.climateOrNull())), metadata.plantCardId)
            .installedRows(garden.id, garden.name).single().copy(id = 58, userEdited = true, userLockedDate = true)
        val personalRows = listOf(original.copy(drugId = 77),
            original.copy(drugId = null, programStepId = "${EradicationCare.AUTUMN_IRON_STEP}~personal:77"))
        for (row in personalRows) for (availableGarden in listOf(garden, null)) {
            val plan = ProgramUpdates.plan(listOf(row), emptyList(), availableGarden, metadata, today, listOf(next), listOf(source))
            assertTrue("climate=${availableGarden != null}, drugId=${row.drugId}", plan?.updates.orEmpty().isEmpty())
            assertTrue(plan?.additions.orEmpty().isEmpty())
            assertTrue(plan?.removals.orEmpty().isEmpty())
        }
    }
}
