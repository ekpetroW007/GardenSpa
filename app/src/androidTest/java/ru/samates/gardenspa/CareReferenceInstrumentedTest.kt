package ru.samates.gardenspa

import android.graphics.Bitmap
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.samates.gardenspa.domain.*
import ru.samates.gardenspa.data.database.entity.GardenEntity
import ru.samates.gardenspa.viewmodel.UserViewModel
import ru.samates.gardenspa.presentation.*
import ru.samates.gardenspa.ui.theme.MyApplicationTheme

@RunWith(AndroidJUnit4::class)
class CareReferenceInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val directory = File(context.getExternalFilesDir(null), "care-reference-qa").apply { mkdirs() }
        val roots = compose.onAllNodes(isRoot())
        val bitmap = if (roots.fetchSemanticsNodes().size == 1) {
            roots[0].captureToImage().asAndroidBitmap()
        } else {
            instrumentation.uiAutomation.takeScreenshot()
        }
        File(directory, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun referenceNavigationOpensFertilizersAndMovesTheRecipeToTankMixtures() {
        var section by mutableStateOf("Справочник")
        compose.setContent {
            MyApplicationTheme {
                val navigation = rememberNavController()
                BotanicalBackground {
                    when (section) {
                        "Справочник" -> ReferenceHub(PaddingValues()) { section = it }
                        "Удобрения" -> Drugs(navigation, PaddingValues(), ProductSection.FERTILIZER)
                        "Баковые смеси" -> FolkRecipes(PaddingValues(), tankMixes = true)
                        "Рецепты" -> FolkRecipes(PaddingValues())
                    }
                }
            }
        }
        capture("reference-hub")
        compose.onNodeWithText(ProductSection.FERTILIZER.title).performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Агрикола Аква")
        compose.onNodeWithText("Агрикола Аква для гортензий").assertExists()
        compose.onNodeWithText("Показать инструкцию").performClick()
        compose.onNodeWithText("15 мл на 1 л воды", substring = true).assertExists()
        capture("fertilizer-search")
        compose.runOnIdle { section = "Справочник" }
        compose.onNodeWithText("Баковые смеси").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Волшебный")
        compose.onNodeWithText("Баковая смесь «Волшебный напиток для растений»").assertExists()
        compose.onNodeWithText("Показать рецепт").performClick()
        compose.onNodeWithText("Алирин-Б — 4 таблетки", substring = true).assertExists()
        capture("tank-mixture")
        compose.runOnIdle { section = "Рецепты" }
        compose.onNode(hasSetTextAction()).performTextInput("Волшебный")
        compose.onNodeWithText("Ничего не найдено").assertExists()
    }

    @Test fun peonyHasOptionalDiagnosisAndManufacturerSearchAndCanSaveAChoice() {
        var chosen: String? = null
        var diagnosis: String? = "unset"
        compose.setContent {
            MyApplicationTheme {
                ProgramProductDialog("peony", onDismiss = {}, onConfirm = { product, problem, cultivation, _, _ ->
                    chosen = product
                    diagnosis = problem
                    assertEquals(CultivationType.OPEN_GROUND, cultivation)
                })
            }
        }
        compose.onNodeWithText("Теплица").assertDoesNotExist()
        compose.onNodeWithText("Перейти к препаратам").assertIsEnabled().performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Август")
        compose.onNodeWithText("Подробнее: Чистоцвет, КЭ — цветочные культуры").performScrollTo().performClick()
        compose.onNodeWithText("Добавить обработку").assertIsNotEnabled()
        compose.onNodeWithText("Я сверил(а) этикетку: средство подходит для моей культуры, цели и условий")
            .performScrollTo().performClick()
        capture("peony-after-inspection")
        compose.onNodeWithText("Добавить обработку").performClick()
        compose.runOnIdle {
            assertEquals("chistotsvet_flowers", chosen)
            assertNull(diagnosis)
        }
    }

    @Test fun hydrangeaAllowsSelectingAnotherManufacturersFertilizer() {
        var chosen: String? = null
        compose.setContent {
            MyApplicationTheme {
                ProgramProductDialog("hydrangea", stepId = "nutrition~organic_hydrangea",
                    onDismiss = {}, onConfirm = { product, _, _, _, _ -> chosen = product })
            }
        }
        compose.onNode(hasSetTextAction()).performTextInput("Агрикола")
        compose.onNodeWithText("Подробнее: Агрикола Аква для гортензий").performScrollTo().performClick()
        compose.onNodeWithText("Я сверил(а) этикетку: средство подходит для моей культуры, цели и условий")
            .performScrollTo().performClick()
        compose.onNodeWithText("Выбрать этот препарат").performClick()
        compose.runOnIdle { assertEquals("agricola_hydrangea", chosen) }
    }

    @Test fun greenConeRecipeIsSearchableAndShowsTheConfirmedLiquidDose() {
        compose.setContent { MyApplicationTheme { BotanicalBackground { FolkRecipes(PaddingValues(), tankMixes = true) } } }
        compose.onNode(hasSetTextAction()).performTextInput("Зелёный конус")
        compose.onNodeWithText("Баковая смесь «Зелёный конус. Биологические препараты»").assertExists()
        compose.onNodeWithText("Показать рецепт").performClick()
        compose.onNodeWithText("Лепидоцид, жидкий препарат — 20 мл", substring = true).assertExists()
        capture("green-cone-recipe")
        compose.onNodeWithText("Рецепт пользователя; совместимость полного состава отдельно не подтверждена", substring = true)
            .performScrollTo().assertIsDisplayed()
    }

    @Test fun globalSearchFindsRecipesFertilizersAndFoliarProductsWithoutOpeningCategories() {
        compose.setContent { MyApplicationTheme { BotanicalBackground { ReferenceHub(PaddingValues()) {} } } }
        val search = compose.onNodeWithText("Поиск по всем категориям")
        search.performTextInput("коктейль аптеки")
        compose.onNodeWithText("Коктейль из аптеки").assertExists()
        compose.onNodeWithText("Народные рецепты").assertExists()
        search.performTextReplacement("газонное Fertika")
        compose.onNodeWithText("Газонное весна–лето, бесхлорное — Fertika").assertExists()
        compose.onNodeWithText(ProductSection.FERTILIZER.title).assertExists()
        search.performTextReplacement("Феровит")
        compose.onAllNodesWithText(ProductSection.TREATMENT.title).onFirst().assertExists()
        search.performTextReplacement("зеленый конус")
        compose.onNodeWithText("Баковые смеси").assertExists()
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        capture("global-reference-search")
    }

    @Test fun greenConeAllowsChoosingNewTankMixtureAndShowsTenLiterRecipe() {
        var chosen: String? = null
        compose.setContent { MyApplicationTheme {
            ProgramProductDialog("apple", stepId = SpringCarePrograms.GREEN_STEP,
                onDismiss = {}, onConfirm = { product, _, _, _, _ -> chosen = product })
        } }
        compose.onNode(hasSetTextAction()).performTextInput("Фитолавин")
        compose.onNodeWithText("Подробнее: Баковая смесь «Фитолавин + стимулятор»").performScrollTo().performClick()
        compose.onNodeWithText("На 10 л воды: Фитолавин — 20 мл", substring = true).assertExists()
        compose.onNodeWithText("Я сверил(а) этикетку: средство подходит для моей культуры, цели и условий")
            .performScrollTo().performClick()
        capture("green-cone-choice")
        compose.onNodeWithText("Выбрать этот препарат").performClick()
        compose.runOnIdle { assertEquals("mix_fitolavin_tank_mix", chosen) }
    }

    @Test fun largeLeafHydrangeaCanSelectSouthernRegionInRealPlantSetup() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as BookeeperApp
        val garden = runBlocking { app.repository.insertGarden(GardenEntity(name = "Сад гортензий для проверки",
            locationName = "Москва", latitude = 55.75, longitude = 37.62,
            climateSafeSpringDay = "--04-25", climateSafeAutumnDay = "--10-10", climateFrostFreeDays = 168,
            climateGdd5 = 2600.0, climateGdd10 = 1750.0, climateWarmPrecipitation = 420.0,
            climateWinterMinimumP10 = -18.0, climateConfidence = "HIGH", climateSourceYears = 20)).toInt() }
        try {
            val user = UserViewModel(PreferencesManager(app), app.climateService)
            compose.setContent { MyApplicationTheme { PlantAdd(rememberNavController(), "", userViewModel = user) } }
            compose.onNodeWithText("Например, томат или яблоня").performTextInput("Гортензия кр")
            compose.onNodeWithText("Гортензия крупнолистная").performClick()
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            compose.onNodeWithContentDescription("Сад. Сейчас:", substring = true).performScrollTo().performClick()
            compose.onAllNodesWithText("Сад гортензий для проверки").onLast().performClick()
            compose.onNodeWithText("Другой регион — добавить укрытие").performScrollTo().performClick()
            compose.onNodeWithText("Южный регион — без укрытия").assertExists()
            capture("hydrangea-southern-region")
        } finally {
            runBlocking { app.repository.deleteGarden(garden) }
        }
    }
}
