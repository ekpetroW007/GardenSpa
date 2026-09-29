package ru.samates.gardenspa

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import ru.samates.gardenspa.data.database.AppDatabase
import ru.samates.gardenspa.data.database.entity.*
import ru.samates.gardenspa.domain.*
import java.time.LocalDate
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ProgramVariantPersistenceInstrumentedTest {
    @Test fun parentDateChangesKeepSevenDayGapAndPreserveManualOrCompletedFollowup() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            db.plantDao().insertPlant(source())
            val parent = db.plantDao().getAllPlantsOnce().single()
            db.plantDao().selectProgramVariant(parent.id, "urea_young", "")
            val child = db.plantDao().getAllPlantsOnce().single { it.id != parent.id }
            suspend fun moveParent(date: String) = db.procedureDao().saveProcedureWithSnapshot(ProcedureEntity(
                plantId = parent.id, procedureName = parent.taskName, scheduledDate = parent.creationDate,
                rescheduledDate = date, status = "PLANNED"))
            moveParent("2027-04-06")
            assertEquals("2027-04-13", db.plantDao().getPlantById(child.id.toLong())?.creationDate)
            db.procedureDao().undoChange(parent.id, parent.creationDate)
            assertEquals("2027-04-11", db.plantDao().getPlantById(child.id.toLong())?.creationDate)
            moveParent("2027-04-07")
            db.plantDao().selectProgramVariant(parent.id, "urea_young", "")
            val movedChild = requireNotNull(db.plantDao().getPlantById(child.id.toLong()))
            assertEquals("2027-04-14", movedChild.creationDate)
            assertEquals(2, db.plantDao().getAllPlantsOnce().size)
            val childMove = db.procedureDao().saveProcedureWithSnapshot(ProcedureEntity(plantId = child.id,
                procedureName = child.taskName, scheduledDate = movedChild.creationDate,
                rescheduledDate = "2027-04-19", status = "PLANNED"))
            moveParent("2027-04-10")
            assertEquals(movedChild, db.plantDao().getPlantById(child.id.toLong()))
            assertEquals(childMove, db.procedureDao().procedureById(childMove.id))
            val completedChild = db.procedureDao().saveProcedureWithSnapshot(childMove.copy(status = "COMPLETED", completedDate = "2027-04-19"))
            moveParent("2027-04-12")
            assertEquals(movedChild, db.plantDao().getPlantById(child.id.toLong()))
            assertEquals(completedChild, db.procedureDao().procedureById(completedChild.id))

            db.plantDao().insertPlant(source().copy(plantCardId = "edited-card", programImportKey = "edited-card:apple:v20:before_bud_break"))
            val editedParent = db.plantDao().getPlantsForCardOnce("edited-card").single()
            db.plantDao().selectProgramVariant(editedParent.id, "urea_young", "")
            var card = db.plantDao().getPlantsForCardOnce("edited-card")
            db.plantDao().replaceImportedProgramCard(card, card.map { if (it.id == editedParent.id) it.copy(creationDate = "2027-04-09") else it })
            card = db.plantDao().getPlantsForCardOnce("edited-card")
            assertEquals("2027-04-16", card.single { it.id != editedParent.id }.creationDate)
            db.plantDao().replaceImportedProgramCard(card, card.map {
                it.copy(creationDate = if (it.id == editedParent.id) "2027-04-10" else "2027-04-25")
            })
            val manualChild = db.plantDao().getPlantsForCardOnce("edited-card").single { it.id != editedParent.id }
            assertEquals("2027-04-25", manualChild.creationDate)
            db.procedureDao().saveProcedureWithSnapshot(ProcedureEntity(plantId = editedParent.id,
                procedureName = editedParent.taskName, scheduledDate = "2027-04-10", rescheduledDate = "2027-04-12"))
            assertEquals(manualChild, db.plantDao().getPlantById(manualChild.id.toLong()))
        } finally { db.close() }
    }

    @Test fun concurrentRepeatAndVariantChangesKeepBothSettings() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            db.plantDao().insertPlant(source())
            val id = db.plantDao().getAllPlantsOnce().single().id
            repeat(10) {
                coroutineScope { listOf(
                    async { db.plantDao().updateProgramRepeat(id, 14) },
                    async { db.plantDao().selectProgramVariant(id, "copper", "copper_source=copper_young") }
                ).awaitAll() }
                val saved = requireNotNull(db.plantDao().getPlantById(id.toLong()))
                assertEquals("copper", saved.selectedVariantId)
                assertEquals("copper_source=copper_young", saved.variantChoices)
                assertEquals("CUSTOM", saved.repeatType)
                assertEquals(14, saved.repeatInterval)
                assertEquals("NEVER", saved.repeatEndType)
                assertEquals(2, db.plantDao().getAllPlantsOnce().size)
            }
            val originalRows = db.plantDao().getAllPlantsOnce()
            val editedDraft = originalRows.map { if (it.id == id) it.withProgramRepeat(7) else it }
            db.plantDao().replaceImportedProgramCard(originalRows, editedDraft)
            assertEquals(7, db.plantDao().getPlantById(id.toLong())?.repeatInterval)
            assertEquals("copper", db.plantDao().getPlantById(id.toLong())?.selectedVariantId)
            assertTrue(runCatching { db.plantDao().replaceImportedProgramCard(originalRows,
                originalRows.map { if (it.id == id) it.withProgramRepeat(30) else it }) }.isFailure)
            assertEquals(7, db.plantDao().getPlantById(id.toLong())?.repeatInterval)
        } finally { db.close() }
    }

    @Test fun selectedRecipeFollowupCommentAndNewSeasonRemainIndependent() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "variant-archive-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        var db = open()
        try {
            db.plantDao().insertPlant(source())
            val id = db.plantDao().getAllPlantsOnce().single().id
            fun completion(plant: PlantEntity) = ProcedureEntity(plantId = plant.id, procedureName = plant.taskName,
                scheduledDate = plant.creationDate, completedDate = plant.creationDate, status = "COMPLETED")
            assertTrue(runCatching { db.procedureDao().saveProcedureWithSnapshot(completion(source().copy(id = id))) }.isFailure)
            db.plantDao().selectProgramVariant(id, "urea_young", "")
            assertEquals(2, db.plantDao().getAllPlantsOnce().size)
            assertEquals("2027-04-11", db.plantDao().getAllPlantsOnce().single { it.id != id }.creationDate)
            db.plantDao().selectProgramVariant(id, "urea_mature", "")
            assertEquals(1, db.plantDao().getAllPlantsOnce().size)
            db.plantDao().selectProgramVariant(id, "copper", "copper_source=copper_young")
            val followup = db.plantDao().getAllPlantsOnce().single { it.id != id }
            val followupArchive = db.procedureDao().saveProcedureWithSnapshot(completion(followup))
            db.plantDao().selectProgramVariant(id, "urea_mature", "")
            assertEquals(followup, db.plantDao().getPlantById(followup.id.toLong()))
            assertEquals(followupArchive, db.procedureDao().procedureById(followupArchive.id))
            db.plantDao().selectProgramVariant(id, "copper", "copper_source=copper_young")
            val selected = requireNotNull(db.plantDao().getPlantById(id.toLong()))
            val archive = db.procedureDao().saveProcedureWithSnapshot(completion(selected).copy(comment = "Листья чистые"))
            assertEquals("copper", archive.selectedVariantId)
            assertEquals("copper_source=copper_young", archive.variantChoices)
            assertTrue(archive.drugName.contains("100 г"))
            assertTrue(runCatching { db.plantDao().selectProgramVariant(id, "urea_young", "") }.isFailure)
            assertEquals(1, db.procedureDao().updateCompletionComment(archive.id, "Повторный осмотр: всё хорошо"))
            val editedArchive = archive.copy(comment = "Повторный осмотр: всё хорошо")
            assertEquals(editedArchive, db.procedureDao().saveProcedureWithSnapshot(completion(selected).copy(comment = "Не перезаписывать")))
            db.procedureDao().scheduleNextSeason(archive.id, LocalDate.of(2027, 4, 4))
            db.close(); db = open()
            val next = db.plantDao().getAllPlantsOnce().single { it.programImportKey == archive.nextSeasonImportKey }
            assertEquals("2028-04-04", next.creationDate)
            assertNull(next.selectedVariantId)
            assertEquals("", next.variantChoices)
            assertFalse(next.drugName.contains("100 г"))
            assertTrue(ProgramVariants.isVariantWork(next))
            assertEquals(editedArchive, db.procedureDao().procedureById(archive.id))
            assertEquals(listOf(2027), ProgramVariants.completedYears(next, db.procedureDao().getAllProceduresOnce())["copper"])
            db.plantDao().selectProgramVariant(next.id, "urea_young", "")
            assertEquals(editedArchive, db.procedureDao().procedureById(archive.id))
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun selectedPreviewImportsOneFollowupAndAutomaticRefreshSurvivesRestart() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "program-refresh-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        var db = open()
        try {
            val gardenId = db.gardenDao().insertGarden(garden()).toInt()
            val previous = PlantCareCatalog.previousAll().first { it.id == "apple" }
            val start = LocalDate.of(2027, 4, 4)
            val old = CareProgramGenerator().generate(previous,
                CareProgramContext(start, CultivationType.OPEN_GROUND, requireNotNull(garden().climateOrNull())), "legacy-program")
            old.installedRows(gardenId, "Сад").filterNot { it.programStepId?.startsWith("after_leaf_fall_iron") == true }
                .forEach { db.plantDao().insertPlant(it.copy(programBaseline = null, userEdited = false)) }
            val before = db.plantDao().getAllPlantsOnce()
            val protected = before.first { it.programStepId?.startsWith("green_cone_biological") == true }
            val history = db.procedureDao().saveProcedureWithSnapshot(ProcedureEntity(plantId = protected.id,
                procedureName = protected.taskName, scheduledDate = protected.creationDate,
                completedDate = protected.creationDate, status = "COMPLETED"))
            val custom = before.first().copy(id = 0, taskName = "Моя отдельная работа", programImportKey = null,
                programStepId = "problem:mine", userLockedDate = true, userEdited = true)
            db.plantDao().insertPlant(custom)
            assertTrue(db.plantDao().refreshInstalledPrograms(LocalDate.of(2026, 9, 29)) > 0)
            assertEquals(0, db.plantDao().refreshInstalledPrograms(LocalDate.of(2026, 9, 29)))
            db.close(); db = open()
            val refreshed = db.plantDao().getAllPlantsOnce()
            assertEquals(protected, refreshed.first { it.id == protected.id })
            assertEquals(history, db.procedureDao().procedureById(history.id))
            assertTrue(refreshed.any { it.taskName == custom.taskName })
            assertTrue(refreshed.any { it.programStepId == EradicationCare.AUTUMN_MIX_STEP })
            assertFalse(refreshed.any { it.programStepId == EradicationCare.AUTUMN_IRON_STEP })
            assertTrue(refreshed.any { it.programStepId == EradicationCare.EARLY_STEP && it.programVersion == EradicationCare.PROGRAM_VERSION })
            assertFalse(refreshed.any { it.programStepId?.startsWith("before_bud_break_pests") == true })

            val current = CareProgramGenerator().generate(PlantCareCatalog.all().first { it.id == "apple" },
                CareProgramContext(start, CultivationType.OPEN_GROUND, requireNotNull(garden().climateOrNull())), "new-program")
            val selected = current.copy(steps = current.steps.map { if (it.templateStepId == EradicationCare.EARLY_STEP)
                it.withProgramVariant("urea_young", "") else it })
            db.plantDao().importProgram(selected.installation(), selected.installedRows(gardenId, "Сад"))
            val imported = db.plantDao().getPlantsForCardOnce("new-program")
            assertEquals(1, imported.count { it.programStepId == "before_bud_break_followup" })
            assertEquals("urea_young", imported.first { it.programStepId == EradicationCare.EARLY_STEP }.selectedVariantId)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    private fun source() = PlantEntity(plantName = "Яблоня", taskName = "Выбрать весеннюю обработку", wateringInterval = 1,
        creationDate = "2027-04-04", drugId = null, gardenId = null, drugName = "Выберите вариант", gardenName = "Сад",
        plantCardId = "variant-card", programId = "apple", programVersion = EradicationCare.PROGRAM_VERSION,
        programStepId = EradicationCare.EARLY_STEP, programImportKey = "variant-card:apple:v20:before_bud_break")

    private fun garden() = GardenEntity(name = "Сад", climateSafeSpringDay = "--04-25", climateSafeAutumnDay = "--10-10",
        climateFrostFreeDays = 168, climateGdd5 = 2600.0, climateGdd10 = 1750.0, climateWarmPrecipitation = 420.0,
        climateWinterMinimumP10 = -18.0, climateConfidence = "HIGH", climateSourceYears = 20)
}
