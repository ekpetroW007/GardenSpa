package ru.samates.gardenspa

import android.app.NotificationManager
import android.graphics.Bitmap
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import ru.samates.gardenspa.domain.*
import ru.samates.gardenspa.notifications.TreatmentReminderScheduler
import ru.samates.gardenspa.presentation.*
import ru.samates.gardenspa.ui.theme.MyApplicationTheme

@RunWith(AndroidJUnit4::class)
class CompletionUndoInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as BookeeperApp
    private val today get() = LocalDate.now()
    private fun rows(card: String) = runBlocking { app.repository.getAllPlantsOnce().filter { it.plantCardId == card } }
    private fun history(card: String) = runBlocking { app.repository.getAllProceduresOnce().filter { it.plantCardId == card } }
    private fun scrollTo(matcher: SemanticsMatcher) = compose.onNode(hasScrollToNodeAction()).performScrollToNode(matcher)

    private fun seed(card: String) = PlantEntity(plantName = "Проверка отмены ${card.take(6)}",
        taskName = "Полив ${card.take(6)}", wateringInterval = 1, creationDate = today.toString(),
        drugId = null, gardenId = null, drugName = "Моё средство", gardenName = "Проверка",
        plantCardId = card, reminderDaysBefore = 0, programNote = "Моя сохранённая инструкция")

    private fun capture(name: String) {
        compose.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.waitForIdle(500, 5_000)
        val bitmap = automation.takeScreenshot()
        val directory = File(app.getExternalFilesDir(null), "completion-undo-qa").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun calendarDialogUndoRestoresNotificationAndOnlyTheSelectedRepeat() {
        val card = UUID.randomUUID().toString()
        var plant: PlantEntity? = null
        try {
            runBlocking {
                PreferencesManager(app).completeRegistration("Тест", true)
                app.repository.insertPlant(seed(card).copy(creationDate = today.minusDays(7).toString(),
                    repeatType = "CUSTOM", repeatInterval = 7, repeatEndType = "NEVER"))
            }
            val work = rows(card).single().also { plant = it }
            val previous = runBlocking { app.repository.insertProcedure(ProcedureEntity(plantId = work.id,
                procedureName = work.taskName, scheduledDate = work.creationDate, completedDate = work.creationDate,
                status = "COMPLETED", comment = "Прошлый полив")) }
            val notificationId = TreatmentReminderScheduler.notificationId(work.id, today.toString())
            val notifications = app.getSystemService(NotificationManager::class.java)
            fun reminderVisible() = notifications.activeNotifications.any { it.id == notificationId }
            TreatmentReminderScheduler.schedule(app)
            TreatmentReminderScheduler.refreshNow(app)
            compose.waitUntil(10_000) { reminderVisible() }
            compose.setContent { MyApplicationTheme {
                BotanicalBackground { Calendar(PaddingValues(), rememberNavController()) }
            } }
            scrollTo(hasContentDescription("Выполнить работу: ${work.taskName}"))
            compose.onNodeWithContentDescription("Выполнить работу: ${work.taskName}").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Сохранить работу на следующий год?").fetchSemanticsNodes().isNotEmpty() }
            compose.waitUntil(10_000) { !reminderVisible() }
            compose.onNodeWithText("Комментарий к выполненной работе").performTextInput("Отметил случайно")
            Espresso.closeSoftKeyboard()
            compose.onNode(hasText("Отменить выполнение") and hasAnyAncestor(isDialog())).performScrollTo().performClick()
            compose.waitUntil(10_000) { history(card).singleOrNull { it.scheduledDate == today.toString() }?.status == "PLANNED" }
            compose.waitUntil(10_000) { reminderVisible() }
            val restored = history(card).single { it.scheduledDate == today.toString() }
            compose.onNodeWithText("Работа отмечена как выполненная").assertDoesNotExist()
            assertNull(restored.completedDate)
            assertEquals("Отметил случайно", restored.comment)
            assertEquals(work.drugName, restored.drugName)
            assertEquals(previous, history(card).single { it.id == previous.id })
            assertEquals(1, rows(card).size)
            assertTrue(scheduledTreatmentsOn(rows(card), history(card), today.plusDays(7)).single().completed.not())
            scrollTo(hasContentDescription("Выполнить работу: ${work.taskName}"))
            compose.onNodeWithContentDescription("Выполнить работу: ${work.taskName}").assertIsEnabled()
            capture("calendar-repeat-undone")
        } finally {
            runBlocking { app.repository.deletePlantCard(card) }
            plant?.let { TreatmentReminderScheduler.cancelTreatmentNotification(app, it.id, today.toString()) }
        }
    }

    @Test fun plantCardCompletesAReadyProgramWorkAndArchiveUndoKeepsItsPostponedDate() {
        val card = UUID.randomUUID().toString()
        try {
            val program = PlantCareCatalog.all().single { it.id == "carrot" }
            runBlocking { app.repository.insertPlant(seed(card).copy(programId = program.id,
                programVersion = program.version, programStepId = "undo_test", creationDate = today.minusDays(2).toString())) }
            val work = rows(card).single()
            val date = today.plusDays(3).toString()
            runBlocking { app.repository.insertProcedure(ProcedureEntity(plantId = work.id, procedureName = work.taskName,
                scheduledDate = work.creationDate, rescheduledDate = date)) }
            var open by mutableStateOf(true)
            compose.setContent { MyApplicationTheme { if (open) PlantDetails(rememberNavController(), work.id) } }
            scrollTo(hasContentDescription("Выполнить работу: ${work.taskName}"))
            compose.onNodeWithText("Дата: ${LocalDate.parse(date).toRussianDate()}").assertExists()
            compose.onNodeWithContentDescription("Выполнить работу: ${work.taskName}").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Нет, только в архив").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Нет, только в архив").performClick()
            compose.waitUntil(10_000) { history(card).single().status == "COMPLETED" }
            val completed = history(card).single()
            compose.runOnIdle { open = false }
            compose.waitForIdle()
            compose.runOnIdle { open = true }
            scrollTo(hasTestTag("archive:${completed.id}"))
            compose.onNode(hasText("Отменить выполнение") and hasAnyAncestor(hasTestTag("archive:${completed.id}")))
                .performScrollTo().performClick()
            compose.waitUntil(10_000) { history(card).single().status == "PLANNED" }
            val restored = history(card).single()
            assertEquals(completed.id, restored.id)
            assertEquals(work.creationDate, restored.scheduledDate)
            assertEquals(date, restored.rescheduledDate)
            assertEquals(work.programNote, restored.note)
            compose.onNodeWithTag("archive:${completed.id}").assertDoesNotExist()
            scrollTo(hasContentDescription("Выполнить работу: ${work.taskName}"))
            compose.onNodeWithContentDescription("Выполнить работу: ${work.taskName}").assertIsEnabled()
            capture("program-archive-undone")
        } finally { runBlocking { app.repository.deletePlantCard(card) } }
    }

    @Test fun completedVariantCanBeUndoneInItsCardAndCalendarWithoutLosingTheNextSeason() {
        val card = UUID.randomUUID().toString()
        try {
            val program = PlantCareCatalog.all().single { it.id == "apple" }
            runBlocking { app.repository.insertPlant(seed(card).copy(programId = "apple", programVersion = program.version,
                programStepId = EradicationCare.EARLY_STEP).withProgramVariant("urea_young", "")) }
            val work = rows(card).single()
            var calendar by mutableStateOf(false)
            compose.setContent { MyApplicationTheme {
                if (calendar) BotanicalBackground { Calendar(PaddingValues(), rememberNavController()) }
                else PlantDetails(rememberNavController(), work.id)
            } }
            scrollTo(hasTestTag("variant:urea_young"))
            compose.onNode(hasText("Выполнено") and hasAnyAncestor(hasTestTag("variant:urea_young")))
                .performScrollTo().performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Да, сохранить").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Да, сохранить").performClick()
            compose.waitUntil(10_000) { rows(card).any { it.programImportKey?.startsWith("archive-next-season:") == true } }
            val archived = history(card).single()
            val nextRows = rows(card).filter { it.id != work.id }
            scrollTo(hasTestTag("undo-variant:urea_young"))
            compose.onNodeWithTag("undo-variant:urea_young").performClick()
            compose.waitUntil(10_000) { history(card).single().status == "PLANNED" }
            assertEquals(nextRows, rows(card).filter { it.id != work.id })
            assertEquals("urea_young", rows(card).single { it.id == work.id }.selectedVariantId)
            assertTrue(ProgramVariants.completedYears(work, history(card)).isEmpty())
            compose.runOnIdle { calendar = true }
            scrollTo(hasTestTag("variant:urea_young"))
            compose.onNode(hasText("Выполнено") and hasAnyAncestor(hasTestTag("variant:urea_young")))
                .performScrollTo().performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Нет, только в архив").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Нет, только в архив").performClick()
            compose.waitUntil(10_000) { history(card).single().status == "COMPLETED" }
            assertEquals(archived.id, history(card).single().id)
            scrollTo(hasTestTag("undo-variant:urea_young"))
            compose.onNodeWithTag("undo-variant:urea_young").performClick()
            compose.waitUntil(10_000) { history(card).single().status == "PLANNED" }
            assertEquals(nextRows, rows(card).filter { it.id != work.id })
            assertEquals(work.variantChoices, history(card).single().variantChoices)
            capture("variant-next-season-preserved")
        } finally { runBlocking { app.repository.deletePlantCard(card) } }
    }
}
