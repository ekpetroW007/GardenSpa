package ru.samates.gardenspa

import android.graphics.Bitmap
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
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
import ru.samates.gardenspa.data.database.entity.*
import ru.samates.gardenspa.domain.*
import ru.samates.gardenspa.presentation.*
import ru.samates.gardenspa.presentation.navigation.AppDestinations
import ru.samates.gardenspa.ui.theme.MyApplicationTheme
import ru.samates.gardenspa.viewmodel.*

@RunWith(AndroidJUnit4::class)
class ProgramRepeatSeasonInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as BookeeperApp
    private fun rows(card: String) = runBlocking { app.repository.getAllPlantsOnce().filter { it.plantCardId == card } }
    private fun history(card: String) = runBlocking { app.repository.getAllProceduresOnce().filter { it.plantCardId == card } }

    private fun withPlant(block: (PlantEntity) -> Unit) {
        val card = UUID.randomUUID().toString()
        val garden = runBlocking { app.repository.insertGarden(GardenEntity(name = "Проверка нового сезона")).toInt() }
        try {
            runBlocking {
                app.repository.insertPlant(PlantEntity(
                    plantName = "Морковь для проверки сезона", taskName = "Полить морковь под корень",
                    wateringInterval = 1, creationDate = LocalDate.now().toString(), drugId = null,
                    gardenId = garden, drugName = "Средство на момент выполнения", gardenName = "Проверка нового сезона",
                    plantCardId = card, programId = "carrot", programVersion = 3,
                    programStepId = "season_test_nutrition", programNote = "Сохранённая инструкция: 10 г на 10 л воды"
                ))
            }
            block(rows(card).single())
        } finally {
            runBlocking { app.repository.deletePlantCard(card); app.repository.deleteGarden(garden) }
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.waitForIdle(500, 5_000)
        val bitmap = automation.takeScreenshot()
        val directory = File(app.getExternalFilesDir(null), "program-repeat-season-qa").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun scrollTo(matcher: SemanticsMatcher) = compose.onNode(hasScrollToNodeAction()).performScrollToNode(matcher)
    private fun repeatSelector() = compose.onNodeWithContentDescription("Повтор работы:", substring = true)

    @Test fun readyProgramEditorOffersEveryIntervalAndPlantCardCanDisableSavedRepeat() = withPlant { plant ->
        val user = UserViewModel(PreferencesManager(app), app.climateService)
        lateinit var navigation: NavHostController
        compose.setContent { MyApplicationTheme {
            navigation = rememberNavController()
            NavHost(navigation, "card") {
                composable("card") { PlantDetails(navigation, plant.id) }
                composable(AppDestinations.PLANT_EDIT) { PlantAdd(navigation, "", plantId = plant.id, userViewModel = user) }
            }
        } }
        compose.onNodeWithText("Редактировать карточку").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Повтор работы:", substring = true).fetchSemanticsNodes().isNotEmpty() }
        repeatSelector().performScrollTo().performClick()
        listOf("Без повтора", "7 дней", "10 дней", "14 дней", "30 дней").forEach { label ->
            compose.onNode(hasText(label) and hasAnyAncestor(isPopup())).assertExists()
        }
        capture("program-repeat-options")
        compose.onNode(hasText("14 дней") and hasAnyAncestor(isPopup())).performClick()
        compose.onNodeWithText("Сохранить программу").performScrollTo().performClick()
        compose.waitUntil(10_000) { rows(plant.plantCardId).single().repeatInterval == 14 }
        val saved = rows(plant.plantCardId).single()
        assertEquals("CUSTOM", saved.repeatType)
        assertEquals("NEVER", saved.repeatEndType)
        assertNull(saved.repeatCount)
        assertTrue(saved.occursOn(LocalDate.parse(saved.creationDate).plusDays(14 * 100L)))
        scrollTo(hasContentDescription("Повтор работы:", substring = true))
        repeatSelector().assertTextContains("Каждые 14 дн.")
        repeatSelector().performClick()
        compose.onNode(hasText("30 дней") and hasAnyAncestor(isPopup())).performClick()
        compose.waitUntil(10_000) { rows(plant.plantCardId).single().repeatInterval == 30 }
        repeatSelector().performClick()
        compose.onNode(hasText("Без повтора") and hasAnyAncestor(isPopup())).performClick()
        compose.waitUntil(10_000) { rows(plant.plantCardId).single().repeatType == "NONE" }
        assertEquals(plant.drugName, rows(plant.plantCardId).single().drugName)
        repeatSelector().assertTextContains("Без повтора")
        capture("program-repeat-disabled")
    }

    @Test fun previewRepeatIsImportedWithoutAnArtificialEndDate() {
        val date = LocalDate.now()
        val card = UUID.randomUUID().toString()
        var program by mutableStateOf(GeneratedCareProgram(
            instanceId = card, templateId = "carrot", templateVersion = 3, plantName = "Морковь — проверка предпросмотра",
            cultivationType = CultivationType.OPEN_GROUND, recommendedStartDate = date, chosenStartDate = date,
            climateSummary = "Проверка повторов", warning = null,
            steps = listOf(GeneratedCareStep("preview_repeat_test", "Полив после посадки", date, date, date,
                recurrence = null, weatherAdjusted = false, needsWeatherConfirmation = false,
                explanation = "По состоянию почвы", productDescription = "Вода", note = "Полив под корень"))
        ))
        val model = PlantsViewmodel(app.repository)
        var error: String? by mutableStateOf(null)
        try {
            compose.setContent { MyApplicationTheme {
                CareProgramPreviewDialog(program, { program = it }, error, importing = false,
                    onDismiss = {}, onImport = {
                        model.importCareProgram(program, gardenId = null, gardenName = "Проверка предпросмотра",
                            reminderDaysBefore = 1, onError = { error = it })
                    })
            } }
            repeatSelector().performScrollTo().performClick()
            compose.onNode(hasText("7 дней") and hasAnyAncestor(isPopup())).performClick()
            repeatSelector().performClick()
            compose.onNode(hasText("10 дней") and hasAnyAncestor(isPopup())).performClick()
            repeatSelector().assertTextContains("Каждые 10 дн.")
            capture("program-preview-repeat")
            compose.onNodeWithText("Добавить растение и работы").performClick()
            compose.waitUntil(10_000) { rows(card).size == 1 || error != null }
            assertNull(error)
            val saved = rows(card).single()
            assertEquals("CUSTOM", saved.repeatType)
            assertEquals(10, saved.repeatInterval)
            assertEquals("NEVER", saved.repeatEndType)
            assertNull(saved.repeatEndDate)
            assertNull(saved.repeatCount)
            assertTrue(saved.occursOn(date.plusDays(10 * 100L)))
        } finally { runBlocking { app.repository.deletePlantCard(card) } }
    }

    @Test fun calendarYesKeepsTheCompletionAndSchedulesOneMatchingWorkNextYear() = withPlant { plant ->
        compose.setContent { MyApplicationTheme {
            val navigation = rememberNavController()
            BotanicalBackground { Calendar(PaddingValues(), navigation) }
        } }
        scrollTo(hasContentDescription("Выполнить работу: ${plant.taskName}"))
        compose.onNodeWithContentDescription("Выполнить работу: ${plant.taskName}").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Сохранить работу на следующий год?").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Сохранить работу на следующий год?").assertExists()
        capture("completion-next-year-question")
        compose.onNodeWithText("Да, сохранить").performClick()
        compose.waitUntil(10_000) { rows(plant.plantCardId).size == 2 && history(plant.plantCardId).size == 1 }
        assertNextSeasonSnapshot(plant)
        compose.onNodeWithText("Сохранить работу на следующий год?").assertDoesNotExist()
    }

    @Test fun todayNoKeepsOnlyArchiveAndArchiveCanScheduleItLater() = withPlant { plant ->
        val user = UserViewModel(PreferencesManager(app), app.climateService)
        lateinit var navigation: NavHostController
        compose.setContent { MyApplicationTheme {
            navigation = rememberNavController()
            NavHost(navigation, "today") {
                composable("today") { BotanicalBackground { Profile(navigation, {}, userViewModel = user) } }
                composable("card") { PlantDetails(navigation, plant.id) }
            }
        } }
        scrollTo(hasContentDescription("Выполнить работу: ${plant.taskName}"))
        compose.onNodeWithContentDescription("Выполнить работу: ${plant.taskName}").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Сохранить работу на следующий год?").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Сохранить работу на следующий год?").assertExists()
        compose.onNodeWithText("Нет, только в архив").performClick()
        compose.waitUntil(10_000) { history(plant.plantCardId).size == 1 }
        assertEquals(1, rows(plant.plantCardId).size)
        assertEquals("COMPLETED", history(plant.plantCardId).single().status)
        compose.runOnIdle { navigation.navigate("card") }
        scrollTo(hasText("Сохранить на следующий сезон"))
        capture("archive-save-next-season")
        compose.onNodeWithText("Сохранить на следующий сезон").performClick()
        compose.onNodeWithText("Да, сохранить").performClick()
        compose.waitUntil(10_000) { rows(plant.plantCardId).size == 2 }
        assertNextSeasonSnapshot(plant)
        scrollTo(hasText("Запланировано на", substring = true))
        compose.onNodeWithText("Запланировано на", substring = true).assertExists()
        compose.onNodeWithText("Сохранить на следующий сезон").assertDoesNotExist()
        capture("archive-next-season-saved")
    }

    private fun assertNextSeasonSnapshot(plant: PlantEntity) {
        val archived = history(plant.plantCardId).single()
        assertEquals("COMPLETED", archived.status)
        assertEquals(LocalDate.now().toString(), archived.completedDate)
        assertEquals(plant.creationDate, archived.scheduledDate)
        assertEquals(plant.drugName, archived.drugName)
        assertEquals(plant.programNote, archived.note)
        val next = rows(plant.plantCardId).single { it.id != plant.id }
        assertEquals(LocalDate.parse(plant.creationDate).plusYears(1).toString(), next.creationDate)
        assertEquals(plant.taskName, next.taskName)
        assertEquals(plant.drugName, next.drugName)
        assertEquals(plant.programNote, next.programNote)
        assertEquals("NONE", next.repeatType)
    }
}
