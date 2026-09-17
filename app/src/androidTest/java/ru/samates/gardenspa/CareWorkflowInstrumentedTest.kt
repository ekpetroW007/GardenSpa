package ru.samates.gardenspa

import android.graphics.Bitmap
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
import ru.samates.gardenspa.presentation.navigation.AppNavigation
import ru.samates.gardenspa.ui.theme.MyApplicationTheme
import ru.samates.gardenspa.viewmodel.*

@RunWith(AndroidJUnit4::class)
class CareWorkflowInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as BookeeperApp
    private fun rows(card: String) = runBlocking { app.repository.getAllPlantsOnce().filter { it.plantCardId == card } }

    private fun withPlant(crop: String = "carrot", name: String = "Морковь для проверки", block: (PlantEntity) -> Unit) {
        val card = UUID.randomUUID().toString()
        val garden = runBlocking { app.repository.insertGarden(GardenEntity(name = "Проверка функций")).toInt() }
        try {
            runBlocking {
                app.repository.insertPlant(PlantEntity(plantName = name, taskName = "Профилактика",
                    wateringInterval = 1, creationDate = LocalDate.now().toString(), drugId = null,
                    gardenId = garden, drugName = "Препарат не выбран", gardenName = "Проверка функций",
                    plantCardId = card, programId = crop, programVersion = 3, programStepId = "preventive_disease_treatment"))
            }
            block(rows(card).single())
        } finally { runBlocking { app.repository.deletePlantCard(card); app.repository.deleteGarden(garden) } }
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().uiAutomation.waitForIdle(500, 5_000)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val directory = File(app.getExternalFilesDir(null), "care-workflow-qa").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun previousPageWorksAcrossAllTabsAndEveryFullScreenForm() = withPlant { plant ->
        val main = MainScreenViewmodel()
        val user = UserViewModel(PreferencesManager(app), app.climateService)
        lateinit var nav: NavHostController
        compose.setContent {
            MyApplicationTheme {
                nav = rememberNavController()
                NavHost(nav, "main") {
                    composable("main") { MainScreen(main, nav, user) }
                    composable("plant") { PlantDetails(nav, plant.id) }
                    composable("edit") { PlantAdd(nav, "", plantId = plant.id, userViewModel = user) }
                    composable("add") { PlantAdd(nav, LocalDate.now().toString(), userViewModel = user) }
                    composable("all") { AllPlants(nav) }
                    composable("garden") { GardenAdd(nav) }
                    composable("location") { GardenLocationSetup(nav, requireNotNull(plant.gardenId)) }
                    composable("drug") { DrugAdd(nav) }
                    composable("drug-info") { DrugInfo(nav, "Средство", "Цель", "Норма") }
                    composable("settings") { SettingsScreen(nav, user) }
                }
            }
        }
        val screens = listOf("Сады", "Календарь", "Справочник", "Препараты", "Удобрения", "Рецепты", "Баковые смеси")
        screens.forEach { page ->
            compose.runOnIdle { main.changeScreen(page) }
            if (page == "Календарь") screenshot("calendar-back")
            if (page == "Справочник") screenshot("reference-sections")
        }
        screens.asReversed().forEachIndexed { index, page ->
            compose.runOnIdle { assertEquals(page, main.selectedScreen) }
            if (index % 2 == 0) compose.onNodeWithContentDescription("Назад").performClick()
            else androidx.test.espresso.Espresso.pressBack()
        }
        compose.runOnIdle { assertEquals("Сегодня", main.selectedScreen); assertFalse(main.canGoBack) }
        for (route in listOf("plant", "edit", "add", "all", "garden", "location", "drug", "drug-info", "settings")) {
            compose.runOnIdle { nav.navigate(route) }
            if (route == "add") {
                compose.onNodeWithText("Популярные растения").performScrollTo()
                listOf("Томат", "Огурец", "Яблоня", "Груша", "Малина", "Смородина", "Газон", "Роза")
                    .forEach { compose.onNodeWithText(it).assertExists() }
                screenshot("popular-plants")
            }
            compose.onNodeWithContentDescription("Назад").assertIsDisplayed().performClick()
            compose.runOnIdle { assertEquals("main", nav.currentDestination?.route) }
        }
    }

    @Test fun tankMixtureCanBeScheduledCompletedAndFoundInPlantArchive() = withPlant("apple", "Яблоня для проверки") { plant ->
        val plannedDate = LocalDate.now().plusDays(2)
        val preferences = PreferencesManager(app)
        runBlocking { preferences.completeRegistration("Тест", true) }
        val user = UserViewModel(preferences, app.climateService)
        compose.setContent { MyApplicationTheme { AppNavigation(user) } }
        compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Средства и народные рецепты").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Средства и народные рецепты").performClick()
        compose.onNodeWithText("Баковые смеси").performScrollTo().performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Зелёный конус")
        compose.onNodeWithText("Использовать").performScrollTo().performClick()
        compose.onNode(hasContentDescription("Растение. Сейчас:", substring = true)).performClick()
        compose.onNode(hasText("Яблоня для проверки · Проверка функций") and hasAnyAncestor(isPopup())).performClick()
        compose.onNodeWithText("Дата: ${LocalDate.now().toRussianDate()}").performClick()
        if (plannedDate.month != LocalDate.now().month) compose.onNodeWithContentDescription("Следующий месяц").performClick()
        compose.onNodeWithContentDescription(plannedDate.toRussianDate()).performClick()
        compose.onNodeWithText("Выбрать дату").performClick()
        screenshot("use-tank-mixture")
        compose.onNodeWithText("Добавить в календарь").performClick()
        compose.waitUntil(10_000) { rows(plant.plantCardId).size == 2 }
        val mixture = rows(plant.plantCardId).single { it.programStepId?.startsWith("tank_mix:") == true }
        assertEquals(plannedDate.toString(), mixture.creationDate)
        assertEquals("NONE", mixture.repeatType)
        assertTrue(mixture.programNote.contains("Лепидоцид, жидкий препарат — 20 мл"))
        compose.onNodeWithContentDescription("Календарь работ").performClick()
        if (plannedDate.month != LocalDate.now().month) compose.onNodeWithContentDescription("Следующий месяц").performClick()
        compose.onNode(hasContentDescription(plannedDate.toRussianDate(), substring = true)).performClick()
        val task = compose.onNode(hasScrollToNodeAction())
        task.performScrollToNode(hasContentDescription("Выполнить работу: ${mixture.taskName}"))
        compose.onNodeWithContentDescription("Выполнить работу: ${mixture.taskName}").performClick()
        compose.waitUntil(10_000) { runBlocking { app.repository.getAllProceduresOnce().any { it.plantId == mixture.id && it.status == "COMPLETED" } } }
        task.performScrollToNode(hasContentDescription("Открыть работу: ${mixture.taskName}"))
        compose.onNodeWithContentDescription("Открыть работу: ${mixture.taskName}").performClick()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Архив выполненных процедур"))
        compose.onNodeWithText("Архив выполненных процедур").assertIsDisplayed()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Выполнено: ${LocalDate.now().toRussianDate()}"))
        screenshot("completed-procedure-archive")
        val history = runBlocking { app.repository.getAllProceduresOnce().single { it.plantId == mixture.id } }
        assertEquals(mixture.drugName, history.drugName)
        assertEquals(plant.plantCardId, history.plantCardId)
    }

    @Test fun genericProgramEditorCanChooseProductAndAppendOptionalDiagnosisWork() = withPlant { plant ->
        val user = UserViewModel(PreferencesManager(app), app.climateService)
        compose.setContent { MyApplicationTheme { PlantAdd(rememberNavController(), "", plantId = plant.id, userViewModel = user) } }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Добавить или заменить препарат").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Добавить или заменить препарат").performScrollTo().performClick()
        compose.onNodeWithText("Подробнее: Биозащитин, концентрат 5 мл").performScrollTo().performClick()
        compose.onNodeWithText("Я сверил(а) этикетку: средство подходит для моей культуры, цели и условий").performScrollTo().performClick()
        compose.onNodeWithText("Выбрать этот препарат").performClick()
        compose.onNodeWithText("После осмотра обнаружена проблема").performScrollTo().performClick()
        compose.onNodeWithText("Не указывать / пропустить").assertExists()
        compose.onNodeWithText("Перейти к препаратам").performClick()
        compose.onNodeWithText("Подробнее: Биозащитин, концентрат 5 мл").performScrollTo().performClick()
        compose.onNode(hasContentDescription("Назад") and hasAnyAncestor(isDialog())).performClick()
        compose.onNode(hasContentDescription("Назад") and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText("Не указывать / пропустить").assertExists()
        compose.onNodeWithText("Перейти к препаратам").performClick()
        compose.onNodeWithText("Подробнее: Биозащитин, концентрат 5 мл").performScrollTo().performClick()
        compose.onNodeWithText("Я сверил(а) этикетку: средство подходит для моей культуры, цели и условий").performScrollTo().performClick()
        compose.onNodeWithText("Добавить обработку").performClick()
        screenshot("generic-program-editor")
        compose.onNodeWithText("Сохранить программу").performScrollTo().performClick()
        compose.waitUntil(10_000) { rows(plant.plantCardId).size == 2 }
        val saved = rows(plant.plantCardId)
        assertEquals("biozashchitin_prevention", ProgramProductCatalog.productForStep(saved.single { it.id == plant.id }.programStepId)?.id)
        assertTrue(saved.single { it.id != plant.id }.programNote.contains("не указаны пользователем"))
        assertTrue(saved.all { it.repeatType == "NONE" && it.drugName.contains("Органик Микс") })
    }
}
