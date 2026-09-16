package ru.samates.gardenspa

import android.graphics.Bitmap
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.presentation.loadPlantPhoto
import ru.samates.gardenspa.presentation.storePlantPhoto
import ru.samates.gardenspa.viewmodel.PlantsViewmodel

@RunWith(AndroidJUnit4::class)
class PlantPhotoPersistenceInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app get() = instrumentation.targetContext.applicationContext as BookeeperApp

    @Test
    fun ownedPhotoSurvivesSourceRemovalAndProgramEditPreservesSchedules() = runBlocking {
        val source = File.createTempFile("test-photo-", ".png", app.cacheDir)
        val bitmap = Bitmap.createBitmap(1600, 800, Bitmap.Config.ARGB_8888)
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val cardId = "test-${UUID.randomUUID()}"
        var owned: File? = null
        try {
            val photo = storePlantPhoto(app, Uri.fromFile(source))
            owned = File(requireNotNull(Uri.parse(photo).path))
            assertTrue(source.delete())
            val loaded = requireNotNull(loadPlantPhoto(app, Uri.parse(photo)))
            assertTrue(loaded.width <= 1280)
            assertTrue(loaded.height > 0)
            repeat(2) { index ->
                app.repository.insertPlant(PlantEntity(
                    plantName = "Тест фото", taskName = "Работа $index", wateringInterval = 14,
                    creationDate = LocalDate.now().plusDays(index.toLong()).toString(),
                    drugId = null, gardenId = null, drugName = "Средство", gardenName = "",
                    repeatType = "CUSTOM", repeatInterval = 14 + index, repeatEndType = "COUNT",
                    repeatCount = 3 + index, reminderDaysBefore = 1, plantCardId = cardId,
                    programId = "tomato", programStepId = "step-$index", programNote = "Заметка $index"
                ))
            }
            val original = app.repository.getAllPlantsOnce().filter { it.plantCardId == cardId }.sortedBy { it.id }
            val saved = CountDownLatch(1)
            instrumentation.runOnMainSync {
                PlantsViewmodel(app.repository).updateImportedProgramCard(
                    plantName = "Томат с фото", existingRows = original,
                    taskNames = original.map { it.taskName }, taskDates = original.map { LocalDate.parse(it.creationDate) },
                    gardenId = null, gardenName = "", reminderDaysBefore = 5, photoUri = photo,
                    onSaved = { saved.countDown() }
                )
            }
            assertTrue("Program save callback", saved.await(10, TimeUnit.SECONDS))
            val actual = app.repository.getAllPlantsOnce().filter { it.plantCardId == cardId }.sortedBy { it.id }
            assertEquals(original.map { it.id }, actual.map { it.id })
            original.zip(actual).forEach { (before, after) ->
                assertEquals(photo, after.photoUri)
                assertEquals(5, after.reminderDaysBefore)
                assertEquals(before.repeatCount, after.repeatCount)
                assertEquals(before.repeatInterval, after.repeatInterval)
                assertEquals(before.programNote, after.programNote)
                assertNotNull(loadPlantPhoto(app, Uri.parse(after.photoUri)))
            }
        } finally {
            app.repository.deletePlantCard(cardId)
            source.delete()
            owned?.delete()
        }
    }

    @Test
    fun invalidPhotoIsRejectedWithoutKeepingAnUnreadableCopy() = runBlocking {
        val source = File.createTempFile("test-invalid-photo-", ".txt", app.cacheDir)
        val directory = File(app.filesDir, "plant-photos")
        val before = directory.list()?.toSet().orEmpty()
        try {
            source.writeText("not an image")
            assertTrue(runCatching { storePlantPhoto(app, Uri.fromFile(source)) }.isFailure)
            assertEquals(before, directory.list()?.toSet().orEmpty())
        } finally {
            source.delete()
        }
    }
}
