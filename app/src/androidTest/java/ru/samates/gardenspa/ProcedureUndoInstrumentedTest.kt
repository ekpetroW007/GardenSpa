package ru.samates.gardenspa

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import ru.samates.gardenspa.data.database.AppDatabase
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import ru.samates.gardenspa.domain.*
import java.time.LocalDate
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ProcedureUndoInstrumentedTest {
    @Test fun reschedulingAndEditingSpringMovesUntouchedRootWorkButPreservesItsOwnPostponement() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val year = LocalDate.now().year + 1
            val parentSource = source().copy(creationDate = "$year-04-04", programId = "apple", programVersion = 22,
                programStepId = EradicationCare.EARLY_STEP, programImportKey = "undo-card:apple:v22:before_bud_break")
            db.plantDao().insertPlant(parentSource)
            val parent = db.plantDao().getAllPlantsOnce().single()
            val root = source().copy(creationDate = "$year-04-25", programId = "apple", programVersion = RootCareSchedule.VERSION,
                programStepId = "nutrition", programImportKey = "undo-card:apple:v23:nutrition")
                .let { it.copy(programBaseline = it.programFingerprint()) }
            db.plantDao().insertPlant(root)
            val rootId = db.plantDao().getAllPlantsOnce().single { it.id != parent.id }.id
            db.procedureDao().saveProcedureWithSnapshot(ProcedureEntity(plantId = parent.id, procedureName = parent.taskName,
                scheduledDate = parent.creationDate, rescheduledDate = "$year-05-10"))
            assertEquals("$year-05-11", db.plantDao().getPlantById(rootId.toLong())?.creationDate)
            db.procedureDao().undoChange(parent.id, parent.creationDate)
            assertEquals("$year-05-11", db.plantDao().getPlantById(rootId.toLong())?.creationDate)
            val editedParent = parent.copy(creationDate = "$year-06-01", userEdited = true, userLockedDate = true)
            db.plantDao().replacePlantCard("undo-card", db.plantDao().getAllPlantsOnce().map { if (it.id == parent.id) editedParent else it })
            val movedRoot = requireNotNull(db.plantDao().getPlantById(rootId.toLong()))
            assertEquals("$year-06-02", movedRoot.creationDate)
            val rootRecord = db.procedureDao().saveProcedureWithSnapshot(ProcedureEntity(plantId = rootId, procedureName = movedRoot.taskName,
                scheduledDate = movedRoot.creationDate, rescheduledDate = "$year-06-05"))
            db.procedureDao().saveProcedureWithSnapshot(ProcedureEntity(plantId = parent.id, procedureName = parent.taskName,
                scheduledDate = editedParent.creationDate, rescheduledDate = "$year-07-01"))
            assertEquals(movedRoot, db.plantDao().getPlantById(rootId.toLong()))
            assertEquals(rootRecord, db.procedureDao().procedureById(rootRecord.id))
        } finally { db.close() }
    }

    @Test fun undoAfterReopenKeepsPostponementCommentAndNextSeasonIdentityOnRepeatedCompletion() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "undo-completion-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        var db = open()
        try {
            db.plantDao().insertPlant(source())
            val original = db.plantDao().getAllPlantsOnce().single()
            val moved = db.procedureDao().saveProcedureWithSnapshot(ProcedureEntity(plantId = original.id,
                procedureName = original.taskName, scheduledDate = original.creationDate,
                rescheduledDate = "2027-04-08", status = "PLANNED"))
            val completed = db.procedureDao().saveProcedureWithSnapshot(moved.copy(id = 0,
                status = "COMPLETED", completedDate = "2027-04-09", comment = "Сохранить состав | 10 л"))
            assertEquals(moved.id, completed.id)
            db.procedureDao().scheduleNextSeason(completed.id, LocalDate.of(2027, 4, 9))
            val next = db.plantDao().getAllPlantsOnce().single { it.programImportKey == completed.nextSeasonImportKey }
            val nextArchive = db.procedureDao().saveProcedureWithSnapshot(completion(next))
            db.close(); db = open()
            val results = coroutineScope { (1..3).map { async { db.procedureDao().undoCompletion(completed.id) } }.awaitAll() }
            val expected = completed.copy(status = "PLANNED", completedDate = null)
            assertTrue(results.all { it == expected })
            assertEquals(original, db.plantDao().getPlantById(original.id.toLong()))
            assertEquals(next, db.plantDao().getPlantById(next.id.toLong()))
            assertEquals(nextArchive, db.procedureDao().procedureById(nextArchive.id))
            db.close(); db = open()
            assertEquals(expected, db.procedureDao().procedureById(completed.id))
            val plants = db.plantDao().getAllPlantsOnce()
            val records = db.procedureDao().getAllProceduresOnce()
            assertTrue(scheduledTreatmentsOn(plants, records, LocalDate.parse(original.creationDate)).isEmpty())
            assertFalse(scheduledTreatmentsOn(plants, records, LocalDate.of(2027, 4, 8)).single().completed)
            val again = db.procedureDao().saveProcedureWithSnapshot(completion(original).copy(rescheduledDate = "2027-04-08"))
            assertEquals(completed.id, again.id)
            assertEquals(completed.comment, again.comment)
            assertEquals(again, db.procedureDao().saveProcedureWithSnapshot(completion(original)))
            db.procedureDao().scheduleNextSeason(again.id, LocalDate.of(2027, 4, 9))
            assertEquals(2, db.plantDao().getAllPlantsOnce().size)
            assertEquals(next, db.plantDao().getPlantById(next.id.toLong()))
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun undoOnlyReopensTheSelectedRepeatAndPinsAnOccurrenceRemovedByLaterScheduleEdits() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            db.plantDao().insertPlant(source().copy(repeatType = "CUSTOM", repeatInterval = 7, wateringInterval = 7))
            val original = db.plantDao().getAllPlantsOnce().single()
            val first = db.procedureDao().saveProcedureWithSnapshot(completion(original))
            val secondDate = LocalDate.parse(original.creationDate).plusDays(7)
            val second = db.procedureDao().saveProcedureWithSnapshot(completion(original).copy(scheduledDate = secondDate.toString()))
            db.procedureDao().undoCompletion(first.id)
            val records = db.procedureDao().getAllProceduresOnce()
            assertFalse(scheduledTreatmentsOn(listOf(original), records, LocalDate.parse(original.creationDate)).single().completed)
            assertTrue(scheduledTreatmentsOn(listOf(original), records, secondDate).single().completed)
            assertEquals(original, db.plantDao().getPlantById(original.id.toLong()))
            val edited = original.copy(creationDate = "2027-06-01", repeatType = "NONE", userEdited = true, userLockedDate = true)
            db.plantDao().updatePlant(edited)
            val reopened = db.procedureDao().undoCompletion(second.id)
            assertEquals(secondDate.toString(), reopened.rescheduledDate)
            assertEquals(edited, db.plantDao().getPlantById(original.id.toLong()))
            assertFalse(scheduledTreatmentsOn(listOf(edited), db.procedureDao().getAllProceduresOnce(), secondDate).single().completed)
            assertEquals(1, db.plantDao().getAllPlantsOnce().size)
            val completedAgain = db.procedureDao().saveProcedureWithSnapshot(completion(edited).copy(scheduledDate = secondDate.toString()))
            assertEquals(second.id, completedAgain.id)
            assertEquals(secondDate.toString(), completedAgain.rescheduledDate)
            assertTrue(scheduledTreatmentsOn(listOf(edited), db.procedureDao().getAllProceduresOnce(), secondDate).single().completed)
            db.procedureDao().undoCompletion(second.id)
            db.procedureDao().saveProcedureWithSnapshot(reopened.copy(rescheduledDate = secondDate.plusDays(2).toString()))
            db.procedureDao().undoChange(original.id, secondDate.toString())
            assertEquals(second.id, db.procedureDao().procedureForSchedule(original.id, secondDate.toString())?.id)
            assertFalse(scheduledTreatmentsOn(listOf(edited), db.procedureDao().getAllProceduresOnce(), secondDate).single().completed)
        } finally { db.close() }
    }

    @Test fun undoVariantKeepsChosenRecipeBothSeasonsAndTheirFollowups() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            db.plantDao().insertPlant(source().copy(programId = "apple", programVersion = EradicationCare.PROGRAM_VERSION,
                programStepId = EradicationCare.EARLY_STEP, programImportKey = "undo-card:apple:v22:before_bud_break"))
            val id = db.plantDao().getAllPlantsOnce().single().id
            val choices = ProgramVariants.editRecipe("copper", "copper_source=copper_young", "Личный состав = 10 л", "Моя инструкция\nБез изменений")
            db.plantDao().selectProgramVariant(id, "copper", choices)
            val original = requireNotNull(db.plantDao().getPlantById(id.toLong()))
            val moved = db.procedureDao().saveProcedureWithSnapshot(ProcedureEntity(plantId = id,
                procedureName = original.taskName, scheduledDate = original.creationDate, rescheduledDate = "2027-04-08"))
            val child = db.plantDao().getAllPlantsOnce().single { it.id != id }
            assertEquals("2027-04-15", child.creationDate)
            val archived = db.procedureDao().saveProcedureWithSnapshot(moved.copy(status = "COMPLETED", completedDate = "2027-04-08"))
            db.procedureDao().scheduleNextSeason(archived.id, LocalDate.of(2027, 4, 8))
            val nextId = requireNotNull(db.procedureDao().nextSeasonWork(archived.nextSeasonImportKey)).id
            db.plantDao().selectProgramVariant(nextId, "urea_young", "")
            val plantsBefore = db.plantDao().getAllPlantsOnce()
            val nextChild = plantsBefore.single { it.programImportKey?.startsWith("variant-followup:$nextId:") == true }
            val nextChildArchive = db.procedureDao().saveProcedureWithSnapshot(completion(nextChild))
            db.procedureDao().undoChange(id, original.creationDate)
            assertEquals(plantsBefore, db.plantDao().getAllPlantsOnce())
            assertEquals(archived.copy(status = "PLANNED", completedDate = null), db.procedureDao().procedureById(archived.id))
            assertEquals(nextChildArchive, db.procedureDao().procedureById(nextChildArchive.id))
            assertEquals(choices, db.plantDao().getPlantById(id.toLong())?.variantChoices)
            assertTrue(ProgramVariants.completedYears(original, db.procedureDao().getAllProceduresOnce()).isEmpty())
        } finally { db.close() }
    }

    @Test fun undoDeletedOrReplacedWorkRestoresOneSnapshotWithoutOverwritingOtherWork() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (deleted in listOf(false, true)) {
            val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                db.plantDao().insertPlant(source())
                val original = db.plantDao().getAllPlantsOnce().single()
                val archived = db.procedureDao().saveProcedureWithSnapshot(completion(original).copy(comment = "Комментарий к исходной работе"))
                db.plantDao().insertPlant(source().copy(taskName = "Независимая работа", drugName = "Другой препарат"))
                val unrelated = db.plantDao().getAllPlantsOnce().single { it.id != original.id }
                val changed = original.copy(drugName = "Новый выбранный препарат", programNote = "Другая дозировка", userEdited = true)
                if (deleted) db.plantDao().deletePlant(original.id) else db.plantDao().updatePlant(changed)
                val planned = db.procedureDao().undoCompletion(archived.id)
                val restored = requireNotNull(db.plantDao().getPlantById(planned.plantId.toLong()))
                assertEquals(archived.id, planned.id)
                assertEquals("PLANNED", planned.status)
                assertEquals(archived.comment, planned.comment)
                assertEquals(archived.drugName, restored.drugName)
                assertEquals(archived.note, restored.programNote)
                assertEquals(archived.procedureName, restored.taskName)
                assertEquals("NONE", restored.repeatType)
                assertEquals(unrelated, db.plantDao().getPlantById(unrelated.id.toLong()))
                if (deleted) assertEquals(original.id, restored.id) else {
                    assertNotEquals(original.id, restored.id)
                    assertEquals(changed, db.plantDao().getPlantById(original.id.toLong()))
                }
                val size = db.plantDao().getAllPlantsOnce().size
                assertEquals(planned, db.procedureDao().undoCompletion(archived.id))
                assertEquals(size, db.plantDao().getAllPlantsOnce().size)
            } finally { db.close() }
        }
    }

    private fun source() = PlantEntity(plantName = "Яблоня", taskName = "Обработка", wateringInterval = 1,
        creationDate = "2027-04-04", drugId = null, gardenId = null, drugName = "Сохранённое средство",
        gardenName = "Сад", plantCardId = "undo-card", programNote = "Сохранённая дозировка")

    private fun completion(plant: PlantEntity) = ProcedureEntity(plantId = plant.id, procedureName = plant.taskName,
        scheduledDate = plant.creationDate, completedDate = plant.creationDate, status = "COMPLETED")
}
