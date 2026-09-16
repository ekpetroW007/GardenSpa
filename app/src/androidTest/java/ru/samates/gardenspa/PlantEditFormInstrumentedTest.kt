package ru.samates.gardenspa

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.samates.gardenspa.data.database.entity.GardenEntity
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.presentation.PlantAdd
import ru.samates.gardenspa.presentation.PreferencesManager
import ru.samates.gardenspa.presentation.storePlantPhoto
import ru.samates.gardenspa.ui.theme.MyApplicationTheme
import ru.samates.gardenspa.viewmodel.UserViewModel
import ru.samates.gardenspa.domain.scheduledTreatmentsOn
import ru.samates.gardenspa.notifications.TreatmentNotificationPublisher

@RunWith(AndroidJUnit4::class)
class PlantEditFormInstrumentedTest {
    @get:Rule val composeRule = createComposeRule()
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as BookeeperApp

    @Test
    fun programReminderCanBeChangedWithoutReplacingProgramRecurrence() = withPlant(program = true) { original ->
        open(original.id)
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithContentDescription("Фотография растения: Тест редактирования").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Фотография растения: Тест редактирования").performScrollTo().assertIsDisplayed()
        screenshot("plant-edit-photo.png")
        choose("Напоминание", "За 5 дней")
        screenshot("plant-program-reminder.png")
        composeRule.onNodeWithText("Сохранить программу").performScrollTo().performClick()
        composeRule.waitUntil(10_000) { row(original.id).reminderDaysBefore == 5 }
        val saved = row(original.id)
        assertEquals(original.programId, saved.programId)
        assertEquals(original.repeatCount, saved.repeatCount)
        assertEquals(original.repeatInterval, saved.repeatInterval)
        assertEquals(original.id, saved.id)
        assertEquals(original.photoUri, saved.photoUri)
    }

    @Test
    fun manualRepeatCountCanBeIncreasedDecreasedAndRemoved() = withPlant(program = false) { original ->
        open(original.id)
        choose("Повтор", "Еженедельно")
        composeRule.onNodeWithText("Пн").performScrollTo().assertExists()
        // Document order matters even when the reminder is below the viewport.
        val monday = composeRule.onNodeWithText("Пн").getUnclippedBoundsInRoot()
        val reminder = composeRule.onNode(hasContentDescription("Напоминание. Сейчас:", substring = true)).getUnclippedBoundsInRoot()
        assertTrue(monday.top < reminder.top)
        composeRule.onNodeWithContentDescription("Увеличить количество повторов").performScrollTo().performClick()
        composeRule.onNodeWithText("Количество повторов").assertTextContains("4")
        composeRule.onNodeWithContentDescription("Уменьшить количество повторов").performClick()
        composeRule.onNodeWithText("Количество повторов").assertTextContains("3")
        screenshot("plant-repeat-controls.png")
        composeRule.onNodeWithText("Количество повторов").performTextReplacement("0")
        composeRule.onNodeWithText("Сохранить изменения").assertIsNotEnabled()
        composeRule.onNodeWithText("Количество повторов").performTextReplacement("4")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        composeRule.onNodeWithText("Сохранить изменения").performScrollTo().assertIsEnabled().performClick()
        composeRule.waitUntil(10_000) { row(original.id).repeatCount == 4 }
        assertEquals("WEEKLY", row(original.id).repeatType)
        composeRule.onNodeWithText("Убрать ограничение повторов").performScrollTo().performClick()
        composeRule.onNodeWithText("Сохранить изменения").performScrollTo().performClick()
        composeRule.waitUntil(10_000) { row(original.id).repeatEndType == "NEVER" }
        assertNull(row(original.id).repeatCount)
        choose("Повтор", "Не повторять")
        composeRule.onNodeWithText("Сохранить изменения").performScrollTo().performClick()
        composeRule.waitUntil(10_000) { row(original.id).repeatType == "NONE" }
    }

    private fun choose(label: String, option: String) {
        composeRule.onNode(hasContentDescription("$label. Сейчас:", substring = true)).performScrollTo().performClick()
        composeRule.onNode(hasText(option) and hasAnyAncestor(isPopup())).performClick()
    }

    @Test
    fun weekdaysScheduleAllWorkOccurrencesAndOneReminderAppliesToEveryWork() = withPlant(program = false) { original ->
        runBlocking { app.repository.insertPlant(original.copy(id = 0, taskName = "Вторая обработка")) }
        open(original.id)
        composeRule.onNodeWithText("Когда напоминать?").assertDoesNotExist()
        choose("Повтор", "Еженедельно")
        listOf("Вт", "Чт").forEach { label ->
            val chip = composeRule.onNodeWithText(label).performScrollTo()
            if (!chip.fetchSemanticsNode().config[SemanticsProperties.Selected]) chip.performClick()
        }
        listOf("Пн", "Ср", "Пт", "Сб", "Вс").forEach { label ->
            val chip = composeRule.onNodeWithText(label).performScrollTo()
            if (chip.fetchSemanticsNode().config[SemanticsProperties.Selected]) chip.performClick()
        }
        composeRule.onNodeWithText("Количество повторов").performScrollTo().performTextReplacement("4")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        choose("Напоминание", "За 5 дней")
        composeRule.onNodeWithText("Сохранить изменения").performScrollTo().performClick()
        composeRule.waitUntil(10_000) { row(original.id).repeatType == "WEEKLY" && row(original.id).reminderDaysBefore == 5 }
        val rows = runBlocking { app.repository.getAllPlantsOnce().filter { it.plantCardId == original.plantCardId } }
        assertEquals(2, rows.size)
        assertTrue(rows.all { it.repeatInterval == 1 && it.repeatDaysOfWeek == "2,4" && it.reminderDaysBefore == 5 })
        val start = LocalDate.parse(original.creationDate)
        val expectedDates = (0L..28L).map(start::plusDays).filter { it.dayOfWeek.value in setOf(2, 4) }.take(4)
        (0L..28L).map(start::plusDays).forEach { date ->
            val scheduled = scheduledTreatmentsOn(rows, emptyList(), date)
            assertEquals(if (date in expectedDates) 2 else 0, scheduled.size)
            scheduled.forEach { treatment ->
                assertTrue(TreatmentNotificationPublisher.isRegularReminder(treatment, date.minusDays(5)))
                assertFalse(TreatmentNotificationPublisher.isRegularReminder(treatment, date.minusDays(1)))
            }
        }
        screenshot("plant-weekly-procedures.png")
    }

    @Test
    fun savingOldWeeklyRuleClearsLeakedCustomInterval() = withPlant(program = false) { original ->
        runBlocking { app.repository.updatePlant(original.copy(repeatType = "WEEKLY", repeatDaysOfWeek = "2,4")) }
        open(original.id)
        choose("Напоминание", "За 5 дней")
        composeRule.onNodeWithText("Сохранить изменения").performScrollTo().performClick()
        composeRule.waitUntil(10_000) { row(original.id).repeatInterval == 1 }
        assertEquals("2,4", row(original.id).repeatDaysOfWeek)
        assertEquals(5, row(original.id).reminderDaysBefore)
    }

    private fun open(id: Int) {
        val user = UserViewModel(PreferencesManager(app), app.climateService)
        composeRule.setContent {
            MyApplicationTheme {
                PlantAdd(rememberNavController(), "", plantId = id, userViewModel = user)
            }
        }
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Тест редактирования").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun row(id: Int) = runBlocking { app.repository.getAllPlantsOnce().first { it.id == id } }

    private fun screenshot(name: String) {
        composeRule.waitForIdle()
        val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(app.getExternalFilesDir(null), name).outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }

    private fun withPlant(program: Boolean, block: (PlantEntity) -> Unit) {
        val card = "test-${UUID.randomUUID()}"
        val garden = runBlocking { app.repository.insertGarden(GardenEntity(name = "Тест формы")).toInt() }
        val source = File.createTempFile("form-photo-", ".png", app.cacheDir)
        val image = Bitmap.createBitmap(240, 180, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.GREEN) }
        source.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
        val photo = runBlocking { storePlantPhoto(app, Uri.fromFile(source)) }
        source.delete()
        try {
            runBlocking {
                app.repository.insertPlant(PlantEntity(
                    plantName = "Тест редактирования", taskName = "Обработка", wateringInterval = 14,
                    creationDate = LocalDate.now().plusDays(2).toString(), drugId = null, gardenId = garden,
                    drugName = "Без препарата", gardenName = "Тест формы", repeatType = "CUSTOM",
                    repeatInterval = 14, repeatEndType = "COUNT", repeatCount = 3,
                    photoUri = photo,
                    plantCardId = card, programId = if (program) "tomato" else null,
                    programStepId = if (program) "fitosporin_spraying" else null
                ))
            }
            block(runBlocking { app.repository.getAllPlantsOnce().first { it.plantCardId == card } })
        } finally {
            runBlocking { app.repository.deletePlantCard(card); app.repository.deleteGarden(garden) }
            File(requireNotNull(Uri.parse(photo).path)).delete()
        }
    }
}
