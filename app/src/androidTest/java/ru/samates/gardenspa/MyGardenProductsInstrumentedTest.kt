package ru.samates.gardenspa

import android.graphics.Bitmap
import java.io.File
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.first
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
class MyGardenProductsInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as BookeeperApp
    private fun ownProducts() = runBlocking { app.repository.allDrugs.first() }
    private fun rows(garden: Int) = runBlocking { app.repository.getAllPlantsOnce().filter { it.gardenId == garden } }
    private fun capture(name: String) {
        compose.waitForIdle()
        val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val directory = File(app.getExternalFilesDir(null), "my-garden-qa").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }

    private fun garden(count: Int = 1, program: Boolean = false, block: (Int) -> Unit) {
        val gardenId = runBlocking { app.repository.insertGarden(GardenEntity(name = "Тест полного сада",
            locationName = "Москва", latitude = 55.75, longitude = 37.62,
            climateUpdatedAt = "2026-09-19T12:00:00")).toInt() }
        val previousDrugs = ownProducts().map { it.id }.toSet()
        try {
            runBlocking {
                repeat(count) { i ->
                    app.repository.insertPlant(PlantEntity(plantName = "Культура ${i + 1}", taskName = "Полив",
                        wateringInterval = 1, creationDate = LocalDate.now().toString(), drugId = null,
                        gardenId = gardenId, drugName = "Препарат не требуется", gardenName = "Тест полного сада",
                        plantCardId = UUID.randomUUID().toString(), programId = if (program) "tomato" else null,
                        programStepId = if (program) "gumi_omi_feeding" else null))
                }
            }
            block(gardenId)
        } finally {
            runBlocking {
                rows(gardenId).forEach { app.repository.deletePlantCard(it.plantCardId) }
                app.repository.deleteGarden(gardenId)
                ownProducts().filter { it.id !in previousDrugs }.forEach { app.repository.deleteDrug(it.id) }
            }
        }
    }

    @Test fun gardenShowsEightPlantsSearchesThemAndLocationOpensDetailsThenReturns() = garden(8) { garden ->
        lateinit var nav: NavHostController
        compose.setContent { MyApplicationTheme {
            nav = rememberNavController()
            BotanicalBackground { NavHost(nav, "gardens") {
                composable("gardens") { MyGardens(nav, PaddingValues()) }
                composable(AppDestinations.GARDEN_LOCATION) { GardenLocationSetup(nav, garden) }
            } }
        } }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Культура 8").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Культура 8").performScrollTo().assertIsDisplayed()
        capture("all-eight-plants")
        val search = compose.onNodeWithText("Поиск растений в этом саду")
        search.performScrollTo().performTextInput("кУЛЬТУРА 8")
        compose.onNodeWithText("Культура 8").assertExists()
        compose.onNodeWithText("Культура 1").assertDoesNotExist()
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        capture("garden-search")
        search.performTextReplacement("нет такого растения")
        compose.onNodeWithText("Растения не найдены. Измените запрос.").assertExists()
        search.performTextClearance()
        compose.onNodeWithText("Расчёт обновлён", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Место: Москва  ›").performScrollTo().performClick()
        compose.onNodeWithText("Изменить место сада").assertExists()
        compose.onNodeWithText("Расчёт обновлён", substring = true).assertExists()
        capture("location-details")
        compose.onNodeWithText("Город или посёлок").performTextReplacement("Тула")
        compose.onNodeWithText("Сохранить место сада").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithContentDescription("Назад").performClick()
        compose.runOnIdle { assertEquals("gardens", nav.currentDestination?.route) }
        compose.onNodeWithText("Место: Москва  ›").assertExists()
    }

    @Test fun manualCareOffersBothReferenceSectionsAndSavesTheChosenProduct() = garden { garden ->
        val plant = rows(garden).single()
        val user = UserViewModel(PreferencesManager(app), app.climateService)
        compose.setContent { MyApplicationTheme { PlantAdd(rememberNavController(), "", plantId = plant.id, userViewModel = user) } }
        val selector = hasContentDescription("Средство. Сейчас:", substring = true)
        compose.waitUntil(10_000) { compose.onAllNodes(selector).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(selector).performScrollTo().performClick()
        val fertilizer = ProgramProductCatalog.products.single { it.id == "mineral_azofoska" }
        val spray = ProgramProductCatalog.products.single { it.id == "topaz_flowers" }
        compose.onNodeWithText(spray.displayName).assertExists()
        compose.onNodeWithText("+ Добавить новый препарат").assertExists()
        compose.onNodeWithText(fertilizer.displayName).performScrollTo().performClick()
        compose.waitUntil(10_000) { ownProducts().any { it.name == fertilizer.displayName } }
        compose.onNodeWithText("Сохранить изменения").performScrollTo().assertIsEnabled().performClick()
        compose.waitUntil(10_000) { rows(garden).single().drugName == fertilizer.displayName }
        val updated = rows(garden).single()
        assertNotNull(updated.drugId)
        assertTrue(updated.programNote.contains("16%"))
    }

    @Test fun programEditorAddsNewOwnProductAndMyMeansShowsIt() = garden(program = true) { garden ->
        val plant = rows(garden).single()
        val user = UserViewModel(PreferencesManager(app), app.climateService)
        lateinit var nav: NavHostController
        compose.setContent { MyApplicationTheme {
            nav = rememberNavController()
            NavHost(nav, "edit") {
                composable("edit") { PlantAdd(nav, "", plantId = plant.id, userViewModel = user) }
                composable("my") { BotanicalBackground { Drugs(nav, PaddingValues(), section = null) } }
            }
        } }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Выбрать из моих средств").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Мой препарат. Сейчас: Выбрать из моих средств").performScrollTo().performClick()
        compose.onNodeWithText("+ Добавить новый препарат").performScrollTo().performClick()
        val name = "Своё средство ${UUID.randomUUID()}"
        compose.onNodeWithText("Название").performTextInput(name)
        compose.onNodeWithText("Норма расхода (необязательно)").performTextInput("Новая норма")
        compose.onNodeWithText("Добавить").performClick()
        compose.waitUntil(10_000) { ownProducts().any { it.name == name } }
        compose.onNodeWithText("Сохранить программу").performScrollTo().performClick()
        compose.waitUntil(10_000) { rows(garden).single().drugName == name }
        compose.runOnIdle { nav.navigate("my") }
        compose.onNodeWithText(name).assertExists()
        capture("my-products")
        assertEquals("Новая норма", rows(garden).single().programNote)
        assertEquals("Применить: $name", rows(garden).single().taskName)
    }

    @Test fun ownProductAtEndOfAnalogListCanBeConfirmedWithDateAndReminder() {
        var selected: DrugEntity? = null
        val name = "Мой аналог ${UUID.randomUUID()}"
        try {
            compose.setContent { MyApplicationTheme {
                ProgramProductDialog("sweet-pepper", stepId = "preventive_disease_treatment", onDismiss = {},
                    onConfirm = { _, _, _, _, _ -> fail("Ожидался свой препарат") },
                    onCustomConfirm = { drug, _, _, date, reminder ->
                        selected = drug; assertEquals(LocalDate.now(), date); assertEquals(1, reminder)
                    })
            } }
            compose.onNodeWithText("Свой препарат").performScrollTo().performClick()
            compose.onNodeWithText("Название").performTextInput(name)
            compose.onNodeWithText("Добавить").performClick()
            compose.waitUntil(10_000) { ownProducts().any { it.name == name } }
            compose.onNodeWithText("Выбрать этот препарат").assertIsNotEnabled()
            compose.onNodeWithText("Я сверил(а) этикетку: средство подходит для моей культуры, цели и условий").performScrollTo().performClick()
            compose.onNodeWithText("Выбрать этот препарат").performClick()
            compose.runOnIdle { assertEquals(name, selected?.name); assertTrue(requireNotNull(selected).id > 0) }
        } finally { runBlocking { ownProducts().filter { it.name == name }.forEach { app.repository.deleteDrug(it.id) } } }
    }
}
