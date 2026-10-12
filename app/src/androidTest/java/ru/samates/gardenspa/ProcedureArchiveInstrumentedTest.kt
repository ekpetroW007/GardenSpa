package ru.samates.gardenspa

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import ru.samates.gardenspa.data.database.AppDatabase
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import ru.samates.gardenspa.data.database.entity.DrugEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import ru.samates.gardenspa.domain.nextSeasonImportKey
import ru.samates.gardenspa.domain.nextSeasonScheduledDate

@RunWith(AndroidJUnit4::class)
class ProcedureArchiveInstrumentedTest {
    @Test fun nextSeasonRestoresArchivedRecipeAfterEditRemovalAndReopenWithoutDuplicates() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "next-season-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        var db = open()
        try {
            val source = seasonSource().copy(programId = "apple", programVersion = 10,
                programStepId = "green_cone_biological~mix_green_cone", reminderDaysBefore = 3)
            db.plantDao().insertPlant(source)
            val original = db.plantDao().getAllPlantsOnce().single()
            val archive = db.procedureDao().saveProcedureWithSnapshot(ProcedureEntity(
                plantId = original.id, procedureName = original.taskName, scheduledDate = "2026-05-01",
                rescheduledDate = "2026-05-03", completedDate = "2026-05-04", status = "COMPLETED"))
            db.plantDao().updatePlant(original.copy(drugName = "Замена", programNote = "Изменённая норма", reminderDaysBefore = 0))
            db.plantDao().replacePlantCard("season-card", listOf(source.copy(taskName = "Другая работа", drugName = "Другой препарат")))
            db.close(); db = open()
            // No/dismiss requires no mutation; the persisted archive alone can recreate the work later.
            assertEquals(1, db.plantDao().getAllPlantsOnce().size)
            val dates = coroutineScope { (1..5).map { async {
                db.procedureDao().scheduleNextSeason(archive.id, LocalDate.of(2026, 9, 27))
            } }.awaitAll() }
            assertEquals(listOf(LocalDate.of(2027, 5, 3)), dates.distinct())
            db.close(); db = open()
            val restored = db.plantDao().getAllPlantsOnce().single { it.programImportKey == archive.nextSeasonImportKey }
            assertEquals(2, db.plantDao().getAllPlantsOnce().size)
            assertEquals(archive, db.procedureDao().getAllProceduresOnce().single())
            assertEquals(original.taskName, restored.taskName)
            assertEquals(original.drugName, restored.drugName)
            assertEquals(original.programNote, restored.programNote)
            assertEquals(original.programStepId, restored.programStepId)
            assertEquals(original.reminderDaysBefore, restored.reminderDaysBefore)
            assertEquals("NONE", restored.repeatType)
            assertEquals("2027-05-03", restored.creationDate)
            assertEquals("season-card", restored.plantCardId)
            assertTrue(restored.userLockedDate)
            assertEquals(LocalDate.of(2027, 5, 3), archive.nextSeasonScheduledDate(
                db.plantDao().getAllPlantsOnce(), db.procedureDao().getAllProceduresOnce(), LocalDate.of(2026, 9, 27)))
            db.procedureDao().undoChange(original.id, archive.scheduledDate)
            val undone = db.procedureDao().getAllProceduresOnce().single()
            assertEquals(archive.copy(status = "PLANNED", completedDate = null), undone)
            assertEquals(3, db.plantDao().getAllPlantsOnce().size)
            assertEquals(restored, db.plantDao().getPlantById(restored.id.toLong()))
            assertEquals(archive.drugName, db.plantDao().getPlantById(original.id.toLong())?.drugName)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun nextSeasonReusesMatchingUnlimitedOccurrenceButKeepsChangedProductSnapshot() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            db.plantDao().insertPlant(seasonSource().copy(repeatType = "YEARLY", repeatInterval = 1))
            val original = db.plantDao().getAllPlantsOnce().single()
            val archive = db.procedureDao().saveProcedureWithSnapshot(ProcedureEntity(
                plantId = original.id, procedureName = original.taskName, scheduledDate = original.creationDate,
                completedDate = original.creationDate, status = "COMPLETED"))
            val today = LocalDate.of(2026, 9, 27)
            assertEquals(LocalDate.of(2027, 5, 1), db.procedureDao().scheduleNextSeason(archive.id, today))
            assertEquals(1, db.plantDao().getAllPlantsOnce().size)
            db.plantDao().updatePlant(original.copy(drugName = "Новое средство"))
            assertNull(archive.nextSeasonScheduledDate(db.plantDao().getAllPlantsOnce(), listOf(archive), today))
            db.procedureDao().scheduleNextSeason(archive.id, today)
            val next = db.plantDao().getAllPlantsOnce().single { it.programImportKey == archive.nextSeasonImportKey }
            assertEquals(original.drugName, next.drugName)
            db.procedureDao().saveProcedureWithSnapshot(ProcedureEntity(plantId = next.id,
                procedureName = next.taskName, scheduledDate = next.creationDate,
                completedDate = next.creationDate, status = "COMPLETED"))
            db.procedureDao().undoChange(original.id, archive.scheduledDate)
            assertEquals(2, db.procedureDao().getAllProceduresOnce().size)
            assertEquals("PLANNED", db.procedureDao().procedureById(archive.id)?.status)
            assertEquals(3, db.plantDao().getAllPlantsOnce().size)
            assertEquals("COMPLETED", db.procedureDao().procedureForSchedule(next.id, next.creationDate)?.status)
        } finally { db.close() }
    }

    @Test fun deletedPlantCannotBeResurrectedFromAnOrphanArchive() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            db.plantDao().insertPlant(seasonSource())
            val original = db.plantDao().getAllPlantsOnce().single()
            val archive = db.procedureDao().saveProcedureWithSnapshot(ProcedureEntity(plantId = original.id,
                procedureName = original.taskName, scheduledDate = original.creationDate,
                completedDate = original.creationDate, status = "COMPLETED"))
            db.plantDao().deletePlant(original.id)
            assertTrue(runCatching { db.procedureDao().scheduleNextSeason(archive.id, LocalDate.of(2026, 9, 27)) }.isFailure)
            assertEquals(archive, db.procedureDao().getAllProceduresOnce().single())
            assertTrue(db.plantDao().getAllPlantsOnce().isEmpty())
        } finally { db.close() }
    }

    private fun seasonSource() = PlantEntity(plantName = "Яблоня", taskName = "Обработать по зелёному конусу",
        wateringInterval = 1, creationDate = "2026-05-01", drugId = null, gardenId = null,
        drugName = "Средство при выполнении", gardenName = "Сад", plantCardId = "season-card",
        programNote = "Норма при выполнении")

    @Test fun repeatedConfirmationsSaveOneOwnProductAndDifferentInstructionsRemainDistinct() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "my-products-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        var db = open()
        try {
            val product = DrugEntity(name = "  Моё средство  ", purpose = "Питание", consumptionRate = "10 г", applicationMethod = "FERTILIZER")
            val copies = coroutineScope { (1..5).map { async { db.drugDao().saveToMyProducts(product) } }.awaitAll() }
            assertEquals(1, copies.map { it.id }.distinct().size)
            val other = db.drugDao().saveToMyProducts(product.copy(consumptionRate = "20 г"))
            assertNotEquals(copies.first().id, other.id)
            db.close(); db = open()
            val saved = db.drugDao().getAllDrugs().first()
            assertEquals(2, saved.size)
            assertTrue(saved.all { it.name == "Моё средство" })
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun completionSnapshotSurvivesEditsProcedureRemovalAndDatabaseReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "archive-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        var db = open()
        try {
            val date = LocalDate.now().toString()
            val source = PlantEntity(plantName = "Томат", taskName = "Опрыскивание", wateringInterval = 1,
                creationDate = date, drugId = null, gardenId = null, drugName = "Средство при выполнении",
                gardenName = "Дача", plantCardId = "archive-card", programNote = "Норма при выполнении")
            db.plantDao().insertPlant(source)
            val first = db.plantDao().getAllPlantsOnce().single()
            val completed = ProcedureEntity(plantId = first.id, procedureName = first.taskName,
                scheduledDate = date, completedDate = date, status = "COMPLETED")
            db.procedureDao().saveProcedureWithSnapshot(completed)
            db.plantDao().updatePlant(first.copy(drugName = "Новое средство", programNote = "Новая норма"))
            db.procedureDao().saveProcedureWithSnapshot(completed.copy(completedDate = "2099-01-01"))
            db.plantDao().replacePlantCard("archive-card", listOf(source.copy(taskName = "Следующая работа")))
            db.close()
            db = open()
            val history = db.procedureDao().getAllProceduresOnce().single()
            assertEquals("archive-card", history.plantCardId)
            assertEquals("Средство при выполнении", history.drugName)
            assertEquals("Норма при выполнении", history.note)
            assertEquals(date, history.completedDate)
            assertEquals(first.id, history.plantId)
            assertFalse(db.plantDao().getAllPlantsOnce().any { it.id == history.plantId })
            db.plantDao().deletePlantCard("archive-card")
            assertTrue(db.procedureDao().getAllProceduresOnce().isEmpty())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
