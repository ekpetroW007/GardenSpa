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
import kotlinx.coroutines.flow.first
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
        val screens = listOf("Сады", "Календарь", "Справочник", "Мои средства", "Препараты", "Удобрения", "Рецепты", "Баковые смеси")
        screens.forEach { page ->
            compose.runOnIdle { main.changeScreen(page) }
            if (page == "Календарь") screenshot("calendar-back")
            if (page == "Справочник") screenshot("reference-sections")
        }
        screens.asReversed().forEachIndexed { index, page ->
            compose.runOnIdle { assertEquals(page, main.selectedScreen) }
            if (page == "Календарь") {
                compose.onNodeWithContentDescription("Назад").assertDoesNotExist()
                androidx.test.espresso.Espresso.pressBack()
            } else if (index % 2 == 0) compose.onNodeWithContentDescription("Назад").performClick()
            else androidx.test.espresso.Espresso.pressBack()
        }
        compose.runOnIdle { assertEquals("Календарь", main.selectedScreen); assertFalse(main.canGoBack) }
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

    @Test fun eradicationRecipeSearchSchedulesSelectedPlantAndDateAndSavesToMyProducts() = withPlant("apple", "Яблоня для искореняющей смеси") { plant ->
        val recipe = EradicationCare.ironRecipe
        val plannedDate = LocalDate.now().plusDays(2)
        val previousProductIds = runBlocking { app.repository.allDrugs.first().map { it.id }.toSet() }
        var addedProductId: Int? = null
        try {
            val preferences = PreferencesManager(app)
            runBlocking { preferences.completeRegistration("Тест", true) }
            val user = UserViewModel(preferences, app.climateService)
            compose.setContent { MyApplicationTheme { AppNavigation(user) } }
            compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Средства и народные рецепты").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Средства и народные рецепты").performClick()
            compose.onNodeWithText("Баковые смеси").performScrollTo().performClick()
            compose.onNode(hasSetTextAction()).performTextInput("Искореняющая")
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            compose.onNodeWithText(recipe.name).assertExists()
            compose.onNodeWithText("Показать рецепт").performScrollTo().performClick()
            compose.onNodeWithText(EradicationCare.IRON_NOTE).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Что понадобится").assertDoesNotExist()
            compose.onNodeWithText("Как использовать").assertDoesNotExist()
            compose.onNodeWithText("Меры осторожности").assertDoesNotExist()
            compose.onNodeWithText("Использовать").performScrollTo().performClick()
            compose.onNode(hasContentDescription("Растение. Сейчас:", substring = true)).performClick()
            compose.onNode(hasText("${plant.plantName} · Проверка функций") and hasAnyAncestor(isPopup())).performClick()
            compose.onNode(hasContentDescription("Дата. Сейчас:", substring = true)).performClick()
            if (plannedDate.month != LocalDate.now().month) compose.onNodeWithContentDescription("Следующий месяц").performClick()
            compose.onNodeWithContentDescription(plannedDate.toRussianDate()).performClick()
            compose.onNodeWithText("Подтвердить дату").performClick()
            compose.onNodeWithText("Сохранить").performClick()
            compose.waitUntil(10_000) { rows(plant.plantCardId).size == 2 }
            val saved = rows(plant.plantCardId).single { it.id != plant.id }
            assertEquals(plant.plantCardId, saved.plantCardId)
            assertEquals(plannedDate.toString(), saved.creationDate)
            assertEquals("NONE", saved.repeatType)
            assertEquals(recipe.name, saved.drugName)
            assertEquals(EradicationCare.IRON_NOTE, saved.programNote)
            val product = runBlocking { app.repository.allDrugs.first().single { it.id == saved.drugId } }
            addedProductId = product.id.takeIf { it !in previousProductIds }
            assertEquals(recipe.name, product.name)
            assertEquals("TREATMENT", product.applicationMethod)
            assertTrue(product.purpose.contains(EradicationCare.IRON_NOTE))
            compose.onNodeWithContentDescription("Средства и народные рецепты").performClick()
            compose.onNodeWithText("Мои средства").performScrollTo().performClick()
            compose.onNode(hasSetTextAction()).performTextInput("Искореняющая")
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            compose.onNodeWithText(recipe.name).performScrollTo().assertIsDisplayed()
            screenshot("eradication-mixture-my-products")
        } finally {
            addedProductId?.let { id -> runBlocking { app.repository.deleteDrug(id) } }
        }
    }

    @Test fun mixtureReturnsFromCreatingPlantWithItsChosenDateAndCancelCreatesNothing() =
        recipeReturnsFromCreatingPlantWithItsChosenDate(EradicationCare.ironRecipe)

    @Test fun folkRecipeUsesTheSameFlowAndKeepsAnUnspecifiedApplicationMethod() =
        recipeReturnsFromCreatingPlantWithItsChosenDate(FolkFertilizers.recipes.single { it.id == "yeast_tincture" })

    private fun recipeReturnsFromCreatingPlantWithItsChosenDate(recipe: FolkFertilizerRecipe) = withPlant { existing ->
        val date = LocalDate.now().plusDays(3)
        val newName = "Растение для рецепта ${UUID.randomUUID().toString().take(6)}"
        val previousDrugIds = runBlocking { app.repository.allDrugs.first().map { it.id }.toSet() }
        fun createdRows() = runBlocking { app.repository.getAllPlantsOnce().filter { it.plantName == newName } }
        try {
            val preferences = PreferencesManager(app)
            runBlocking { preferences.completeRegistration("Тест", true) }
            val user = UserViewModel(preferences, app.climateService)
            compose.setContent { MyApplicationTheme { AppNavigation(user) } }
            compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Средства и народные рецепты").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Средства и народные рецепты").performClick()
            val category = if (recipe.isTankMix) "Баковые смеси" else "Народные рецепты"
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(category))
            compose.onNodeWithText(category).performClick()
            compose.onNode(hasSetTextAction()).performTextInput(recipe.name)
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            compose.onNodeWithText("Использовать").performScrollTo().performClick()
            compose.onNodeWithText("Сохранить").assertIsNotEnabled()
            compose.onNodeWithContentDescription("Дата. Сейчас: ").performClick()
            compose.onNodeWithText("Подтвердить дату").assertExists()
            if (date.month != LocalDate.now().month) compose.onNodeWithContentDescription("Следующий месяц").performClick()
            compose.onNodeWithContentDescription(date.toRussianDate()).performClick()
            compose.onNodeWithText("Подтвердить дату").performClick()
            compose.onNodeWithText("Сохранить").assertIsNotEnabled()

            fun openAddPlant() {
                compose.onNode(hasContentDescription("Растение. Сейчас:", substring = true)).performClick()
                val add = compose.onNode(hasText("+ Растение") and hasAnyAncestor(isPopup()))
                val oldPlant = compose.onNode(hasText("${existing.plantName} · ${existing.gardenName}") and hasAnyAncestor(isPopup()))
                assertTrue(add.fetchSemanticsNode().boundsInRoot.top < oldPlant.fetchSemanticsNode().boundsInRoot.top)
                add.performClick()
                compose.onNodeWithText("Популярные растения").performScrollTo().assertIsDisplayed()
            }
            openAddPlant()
            compose.onNodeWithContentDescription("Назад").performClick()
            compose.onNodeWithText("Использовать средство").assertExists()
            compose.onNodeWithContentDescription("Дата. Сейчас: ${date.toRussianDate()}").assertExists()
            compose.onNodeWithText("Сохранить").assertIsNotEnabled()
            assertTrue(createdRows().isEmpty())

            openAddPlant()
            compose.onNodeWithTag("popular:Томат").performScrollTo().performClick().assertIsSelected()
            compose.onNodeWithTag("popular:Огурец").performClick().assertIsSelected()
            compose.onNodeWithTag("popular:Томат").assertIsNotSelected()
            screenshot("popular-plant-highlight")
            compose.onAllNodes(hasSetTextAction()).onFirst().performScrollTo().performTextReplacement(newName)
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            compose.onNode(hasContentDescription("Сад. Сейчас:", substring = true)).performScrollTo().performClick()
            compose.onNode(hasText(existing.gardenName) and hasAnyAncestor(isPopup())).performClick()
            compose.onNodeWithText("Настроить уход самостоятельно").performScrollTo().performClick()
            compose.onNodeWithText("Работа 1").performScrollTo().performTextInput("Полив")
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            compose.onNodeWithText("Сохранить растение").performScrollTo().performClick()
            compose.waitUntil(10_000) { createdRows().isNotEmpty() }
            compose.waitUntil(10_000) {
                compose.onAllNodesWithContentDescription("Растение. Сейчас: $newName · ${existing.gardenName}").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Использовать средство").assertExists()
            compose.onNodeWithContentDescription("Растение. Сейчас: $newName · ${existing.gardenName}").assertExists()
            compose.onNodeWithContentDescription("Дата. Сейчас: ${date.toRussianDate()}").assertExists()
            compose.onNodeWithText("Сохранить").assertIsEnabled()
            screenshot("recipe-${recipe.id}-new-plant-and-preserved-date")
            compose.onNodeWithText("Сохранить").performClick()
            compose.waitUntil(10_000) { createdRows().any { it.drugName == recipe.name } }
            val created = createdRows()
            assertEquals(2, created.size)
            assertEquals(1, created.map { it.resolvedCardId }.distinct().size)
            val procedure = created.single { it.drugName == recipe.name }
            assertEquals(existing.gardenId, procedure.gardenId)
            assertEquals(date.toString(), procedure.creationDate)
            assertEquals("NONE", procedure.repeatType)
            assertEquals("${if (recipe.isTankMix) "tank_mix" else "folk_recipe"}:${recipe.id}", procedure.programStepId)
            assertTrue(procedure.programNote.contains(recipe.preparation))
            val product = runBlocking { app.repository.allDrugs.first().single { it.id == procedure.drugId } }
            assertEquals(if (recipe.isTankMix) "TREATMENT" else "UNSPECIFIED", product.applicationMethod)
            assertEquals(recipe.purposeForDrug(), product.purpose)
            assertEquals(recipe.consumptionRate, product.consumptionRate)

            compose.onNodeWithContentDescription("Мои сады и растения").performClick()
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(newName))
            compose.onNodeWithText(newName).performClick()
            val complete = hasContentDescription("Выполнить работу: ${procedure.taskName}")
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(complete)
            compose.onNode(complete).performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Нет, только в архив").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Нет, только в архив").performClick()
            fun record() = runBlocking { app.repository.getAllProceduresOnce().singleOrNull { it.plantId == procedure.id } }
            compose.waitUntil(10_000) { record()?.status == "COMPLETED" }
            val archived = requireNotNull(record())
            assertEquals(procedure.taskName, archived.procedureName)
            assertEquals(recipe.name, archived.drugName)
            assertEquals(procedure.programNote, archived.note)
            assertEquals(date.toString(), archived.scheduledDate)
            val archive = hasTestTag("archive:${archived.id}")
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(archive)
            compose.onNode(archive).assertExists()
            screenshot("recipe-${recipe.id}-completed-archive")
            compose.onNode(hasText("Отменить выполнение") and hasAnyAncestor(archive)).performScrollTo().performClick()
            compose.waitUntil(10_000) { record()?.status == "PLANNED" }
            assertEquals(archived.copy(status = "PLANNED", completedDate = null), record())
            assertEquals(procedure, createdRows().single { it.id == procedure.id })
            assertEquals(product, runBlocking { app.repository.allDrugs.first().single { it.id == product.id } })
            assertEquals(2, createdRows().size)
            compose.onNode(archive).assertDoesNotExist()
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(complete)
            compose.onNode(complete).assertIsEnabled()
            screenshot("recipe-${recipe.id}-execution-undone")
        } finally {
            runBlocking {
                createdRows().map { it.resolvedCardId }.distinct().forEach { app.repository.deletePlantCard(it) }
                app.repository.allDrugs.first().filter { it.name == recipe.name && it.id !in previousDrugIds }
                    .forEach { app.repository.deleteDrug(it.id) }
            }
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
        compose.onNode(hasContentDescription("Дата. Сейчас:", substring = true)).performClick()
        if (plannedDate.month != LocalDate.now().month) compose.onNodeWithContentDescription("Следующий месяц").performClick()
        compose.onNodeWithContentDescription(plannedDate.toRussianDate()).performClick()
        compose.onNodeWithText("Подтвердить дату").performClick()
        screenshot("use-tank-mixture")
        compose.onNodeWithText("Сохранить").performClick()
        compose.waitUntil(10_000) { rows(plant.plantCardId).size == 2 }
        val mixture = rows(plant.plantCardId).single { it.programStepId?.startsWith("tank_mix:") == true }
        assertEquals(plannedDate.toString(), mixture.creationDate)
        assertEquals("NONE", mixture.repeatType)
        assertTrue(mixture.programNote.contains("Лепидоцид, жидкий препарат — 20 мл"))
        assertNotNull(mixture.drugId)
        compose.onNodeWithContentDescription("Календарь работ").performClick()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasContentDescription("Следующий месяц"))
        if (plannedDate.month != LocalDate.now().month) compose.onNodeWithContentDescription("Следующий месяц").performClick()
        compose.onNodeWithContentDescription("${plannedDate.toRussianDate()}, есть запланированные работы").performClick()
        val task = compose.onNode(hasScrollToNodeAction())
        task.performScrollToNode(hasContentDescription("Выполнить работу: ${mixture.taskName}"))
        compose.onNodeWithContentDescription("Выполнить работу: ${mixture.taskName}").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Сохранить работу на следующий год?").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Сохранить работу на следующий год?").assertExists()
        compose.onNodeWithText("Нет, только в архив").performClick()
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
