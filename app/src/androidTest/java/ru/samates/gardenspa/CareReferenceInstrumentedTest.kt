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
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.samates.gardenspa.domain.*
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
}
