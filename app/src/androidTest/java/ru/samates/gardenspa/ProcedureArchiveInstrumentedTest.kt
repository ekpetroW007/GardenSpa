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

@RunWith(AndroidJUnit4::class)
class ProcedureArchiveInstrumentedTest {
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
