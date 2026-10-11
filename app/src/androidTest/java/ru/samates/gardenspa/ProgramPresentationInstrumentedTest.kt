package ru.samates.gardenspa

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
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
import ru.samates.gardenspa.data.database.entity.GardenEntity
import ru.samates.gardenspa.domain.*
import ru.samates.gardenspa.presentation.*
import ru.samates.gardenspa.ui.theme.MyApplicationTheme
import ru.samates.gardenspa.viewmodel.UserViewModel

@RunWith(AndroidJUnit4::class)
class ProgramPresentationInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as BookeeperApp

    private fun compositionLayout(): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag("recipe-composition").performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            assertTrue(it(layouts))
        }
        return layouts.single()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.waitForIdle(500, 5_000)
        val bitmap = automation.takeScreenshot()
        val directory = File(app.getExternalFilesDir(null), "program-presentation-qa").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun preview(steps: List<GeneratedCareStep>) = GeneratedCareProgram(
        UUID.randomUUID().toString(), "apple", EradicationCare.PROGRAM_VERSION, "Яблоня",
        CultivationType.OPEN_GROUND, LocalDate.now(), LocalDate.now(), "Проверка отображения", null, steps)

    private fun step(id: String, title: String, date: LocalDate, product: String = title, note: String = "") =
        GeneratedCareStep(id, title, date, date, date, null, false, false, "", product, note)

    @Test fun compositionRendersFiveLinesThenAllLinesAndCanCollapseAgain() {
        val text = (1..8).joinToString("\n") { "Компонент $it — $it мл" }
        compose.setContent { MyApplicationTheme { BotanicalBackground {
            Column(Modifier.width(300.dp).verticalScroll(rememberScrollState())) { RecipeComposition(text) }
        } } }
        val compact = compositionLayout()
        assertEquals(5, compact.lineCount)
        assertTrue(compact.isLineEllipsized(4))
        assertEquals(text, compact.layoutInput.text.text)
        compose.onNodeWithText("Показать весь состав").performClick()
        val expanded = compositionLayout()
        assertEquals(8, expanded.lineCount)
        assertFalse(expanded.hasVisualOverflow)
        assertEquals(text.length, expanded.getLineEnd(7))
        capture("composition-expanded")
        compose.onNodeWithText("Свернуть состав").performScrollTo().performClick()
        assertEquals(5, compositionLayout().lineCount)
        capture("composition-five-lines")
    }

    @Test fun greenConeShowsItsCompositionAndOnlyOneRecipeHeading() {
        val recipe = FolkFertilizers.tankMixes.single { it.id == FolkFertilizers.GREEN_CONE_ID }
        compose.setContent { MyApplicationTheme { BotanicalBackground {
            Column(Modifier.width(300.dp).verticalScroll(rememberScrollState())) {
                Text(recipe.name)
                ProgramProductSummary(SpringCarePrograms.GREEN_STEP, recipe.name, "Старая инструкция", recipe.name)
            }
        } } }
        compose.onAllNodesWithText(recipe.name).assertCountEquals(1)
        compose.onNodeWithTag("recipe-composition").assertTextEquals(ProgramRecipes.composition(recipe))
        assertEquals(5, compositionLayout().lineCount)
        compose.onNodeWithText("Показать весь состав").performScrollTo().performClick()
        assertTrue(compositionLayout().lineCount > 5)
        assertFalse(compositionLayout().hasVisualOverflow)
        capture("green-cone-composition")
    }

    @Test fun personalRecipeRetainsItsSavedTextInsteadOfReplacingItWithCatalogComposition() {
        val recipe = FolkFertilizers.tankMixes.single { it.id == FolkFertilizers.GREEN_CONE_ID }
        val personal = (1..8).joinToString("\n") { "Моя строка $it: средство $it, дозировка $it мл." }
        compose.setContent { MyApplicationTheme { BotanicalBackground {
            Column(Modifier.width(300.dp).verticalScroll(rememberScrollState())) {
                ProgramProductSummary("${SpringCarePrograms.GREEN_STEP}~personal:42", recipe.name, personal, "Моя работа")
            }
        } } }
        compose.onNodeWithTag("recipe-composition").assertTextEquals(personal)
        compose.onNodeWithText("Показать весь состав").performScrollTo().performClick()
        assertEquals(personal, compositionLayout().layoutInput.text.text)
        assertFalse(compositionLayout().hasVisualOverflow)
        assertFalse(compositionLayout().layoutInput.text.text.contains("Микохелп"))
    }

    @Test fun autumnPreviewShowsOneHeadingAndTheWholeDefaultComposition() {
        val recipe = EradicationCare.autumnRecipe
        val program = preview(listOf(step(EradicationCare.AUTUMN_MIX_STEP, recipe.name, LocalDate.now(), note = recipe.purposeForDrug())))
        compose.setContent { MyApplicationTheme { CareProgramPreviewDialog(program, {}, null, false, {}, {}) } }
        compose.onAllNodesWithText(recipe.name).assertCountEquals(1)
        compose.onNodeWithTag("recipe-composition").assertTextEquals(ProgramRecipes.composition(recipe))
        compose.onNodeWithTag("recipe-composition").assertTextContains("Эпин 1 мл", substring = true)
            .assertTextContains("Прилипатель 10 мл", substring = true)
        assertFalse(compositionLayout().layoutInput.text.text.contains(recipe.name))
        capture("autumn-single-heading")
    }

    @Test fun editorKeepsPersonalAutumnCompositionWhileSavingAnotherChange() {
        val card = UUID.randomUUID().toString()
        val garden = runBlocking { app.repository.insertGarden(GardenEntity(name = "Проверка личного состава")).toInt() }
        val personalProduct = (1..8).joinToString("\n") { "$it. Средство $it — $it мл" }
        val personalNote = "Моя инструкция\nСохранить выбранные дозировки.\nОбработать только эту яблоню."
        val choices = ProgramVariants.editRecipe("autumn_main", "", personalProduct, personalNote)
        val seed = PlantEntity(plantName = "Яблоня — личная смесь", taskName = EradicationCare.autumnRecipe.name,
            wateringInterval = 1, creationDate = LocalDate.now().toString(), drugId = null, gardenId = garden,
            drugName = "", gardenName = "Проверка", plantCardId = card, programId = "apple",
            programVersion = EradicationCare.PROGRAM_VERSION, programStepId = EradicationCare.AUTUMN_MIX_STEP)
            .withProgramVariant("autumn_main", choices)
        fun saved() = runBlocking { app.repository.getAllPlantsOnce().single { it.plantCardId == card } }
        try {
            runBlocking { app.repository.insertPlant(seed) }
            val plant = saved()
            val user = UserViewModel(PreferencesManager(app), app.climateService)
            compose.setContent { MyApplicationTheme {
                PlantAdd(rememberNavController(), "", plantId = plant.id, userViewModel = user)
            } }
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("recipe-composition").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("recipe-composition").performScrollTo().assertTextEquals(personalProduct)
            assertEquals(5, compositionLayout().lineCount)
            assertTrue(compositionLayout().isLineEllipsized(4))
            compose.onNodeWithText("Показать весь состав").performScrollTo().performClick()
            assertEquals(8, compositionLayout().lineCount)
            assertFalse(compositionLayout().hasVisualOverflow)
            assertEquals(personalProduct, compositionLayout().layoutInput.text.text)
            compose.onNodeWithText(personalNote).assertDoesNotExist()
            compose.onNodeWithText("Инструкция и ограничения ▾").performScrollTo().performClick()
            compose.onNodeWithText(personalNote).performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription("Повтор работы: Без повтора").performScrollTo().performClick()
            compose.onNode(hasText("14 дней") and hasAnyAncestor(isPopup())).performClick()
            compose.onNodeWithText("Сохранить программу").performScrollTo().assertIsEnabled().performClick()
            compose.waitUntil(10_000) { saved().repeatInterval == 14 }
            assertEquals(personalProduct, saved().drugName)
            assertEquals(personalNote, saved().programNote)
            assertEquals(choices, saved().variantChoices)
            assertNull(saved().drugId)
            capture("editor-personal-autumn-composition")
        } finally { runBlocking { app.repository.deletePlantCard(card); app.repository.deleteGarden(garden) } }
    }

    private fun withAutumnEditor(choices: String, check: (PlantEntity, () -> PlantEntity) -> Unit) {
        val card = UUID.randomUUID().toString()
        val garden = runBlocking { app.repository.insertGarden(GardenEntity(name = "Проверка осенних компонентов")).toInt() }
        val seed = PlantEntity(plantName = "Яблоня — состав смеси", taskName = EradicationCare.autumnRecipe.name,
            wateringInterval = 1, creationDate = LocalDate.now().toString(), drugId = null, gardenId = garden,
            drugName = "", gardenName = "Проверка осенних компонентов", plantCardId = card, programId = "apple",
            programVersion = EradicationCare.PROGRAM_VERSION, programStepId = EradicationCare.AUTUMN_MIX_STEP)
            .withProgramVariant("autumn_main", choices)
        fun saved() = runBlocking { app.repository.getAllPlantsOnce().single { it.plantCardId == card } }
        try {
            runBlocking { app.repository.insertPlant(seed) }
            val plant = saved()
            val user = UserViewModel(PreferencesManager(app), app.climateService)
            compose.setContent { MyApplicationTheme {
                PlantAdd(rememberNavController(), "", plantId = plant.id, userViewModel = user)
            } }
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("recipe-composition").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("recipe-composition").performScrollTo()
            check(plant, ::saved)
        } finally { runBlocking { app.repository.deletePlantCard(card); app.repository.deleteGarden(garden) } }
    }

    @Test fun editorKeepsTheReferenceCompositionForUnchangedAutumnComponents() = withAutumnEditor("") { _, _ ->
        compose.onNodeWithTag("recipe-composition").assertTextEquals(ProgramRecipes.composition(EradicationCare.autumnRecipe))
        assertEquals(5, compositionLayout().lineCount)
        capture("editor-default-autumn-composition")
    }

    @Test fun editorShowsChosenAutumnComponentsAndPreservesThemWhenSavingRepeat() =
        withAutumnEditor("insecticide=coragen|fungicide=raek") { original, saved ->
            compose.onNodeWithTag("recipe-composition")
                .assertTextContains("Кораген", substring = true)
                .assertTextContains("Раёк", substring = true)
                .assertTextContains("Эпин, 1 мл", substring = true)
                .assertTextContains("прилипатель, 10 мл", substring = true)
            val composition = compositionLayout().layoutInput.text.text
            assertFalse(composition.contains("Актара"))
            assertFalse(composition.contains("Хорус"))
            assertTrue(compositionLayout().lineCount <= 5)
            compose.onNodeWithContentDescription("Повтор работы: Без повтора").performScrollTo().performClick()
            compose.onNode(hasText("14 дней") and hasAnyAncestor(isPopup())).performClick()
            compose.onNodeWithText("Сохранить программу").performScrollTo().assertIsEnabled().performClick()
            compose.waitUntil(10_000) { saved().repeatInterval == 14 }
            assertEquals(original.drugName, saved().drugName)
            assertEquals(original.programNote, saved().programNote)
            assertEquals(original.variantChoices, saved().variantChoices)
            assertEquals("autumn_main", saved().selectedVariantId)
            capture("editor-selected-autumn-components")
        }

    @Test fun previewSortsIronBeforeWhitewashAndEditsTheDisplayedWork() {
        val ironDate = LocalDate.now().plusDays(20)
        val iron = step(EradicationCare.AUTUMN_IRON_STEP, EradicationCare.ironRecipe.name, ironDate)
        val whitewash = step(EradicationCare.WHITEWASH_STEP, "Побелка деревьев", ironDate.plusDays(14), "Садовая побелка")
        var program by mutableStateOf(preview(listOf(whitewash, iron)))
        compose.setContent { MyApplicationTheme { CareProgramPreviewDialog(program, { program = it }, null, false, {}, {}) } }
        compose.onNodeWithText(iron.title).performScrollTo()
        assertTrue(compose.onNodeWithText(iron.title).getUnclippedBoundsInRoot().top <
            compose.onNodeWithText(whitewash.title).getUnclippedBoundsInRoot().top)
        compose.onAllNodesWithContentDescription("Повтор работы: Без повтора").onFirst().performScrollTo().performClick()
        compose.onNode(hasText("14 дней") and hasAnyAncestor(isPopup())).performClick()
        compose.runOnIdle {
            assertNull(program.steps[0].recurrence)
            assertEquals(14, program.steps[1].recurrence?.interval)
            assertEquals(ironDate, program.steps[1].scheduledDate)
            assertEquals(ironDate.plusDays(14), program.steps[0].scheduledDate)
        }
        capture("preview-chronological-work")
    }

    @Test fun plantCardOrdersWorkByItsPostponedDateAndShowsThatDate() {
        val card = UUID.randomUUID().toString()
        val ironDate = LocalDate.now().plusDays(20)
        val whitewashDate = ironDate.plusDays(14)
        val postponedDate = whitewashDate.plusDays(5)
        val ironTitle = EradicationCare.ironRecipe.name
        val whitewashTitle = "Побелка деревьев"
        fun row(title: String, id: String, date: LocalDate) = PlantEntity(
            plantName = "Яблоня — проверка порядка", taskName = title, wateringInterval = 1, creationDate = date.toString(),
            drugId = null, gardenId = null, drugName = title, gardenName = "Проверка", plantCardId = card,
            programId = "apple", programVersion = EradicationCare.PROGRAM_VERSION, programStepId = id)
        fun rows() = runBlocking { app.repository.getAllPlantsOnce().filter { it.plantCardId == card } }
        try {
            runBlocking {
                app.repository.insertPlant(row(ironTitle, EradicationCare.AUTUMN_IRON_STEP, ironDate))
                app.repository.insertPlant(row(whitewashTitle, EradicationCare.WHITEWASH_STEP, whitewashDate)
                    .copy(userEdited = true, userLockedDate = true))
            }
            val iron = rows().single { it.taskName == ironTitle }
            runBlocking { app.repository.insertProcedure(ProcedureEntity(plantId = iron.id, procedureName = ironTitle,
                scheduledDate = ironDate.toString(), rescheduledDate = postponedDate.toString(), status = "PLANNED")) }
            compose.setContent { MyApplicationTheme { PlantDetails(rememberNavController(), iron.id) } }
            val list = compose.onNode(hasScrollToNodeAction())
            list.performScrollToNode(hasText(whitewashTitle))
            val whitewashScroll = list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
            val whitewashTop = compose.onNodeWithText(whitewashTitle).getUnclippedBoundsInRoot().top
            list.performScrollToNode(hasText(ironTitle))
            val ironScroll = list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
            val ironTop = compose.onNodeWithText(ironTitle).getUnclippedBoundsInRoot().top
            assertTrue(ironScroll > whitewashScroll || ironScroll == whitewashScroll && ironTop > whitewashTop)
            list.performScrollToNode(hasText("Дата: ${postponedDate.toRussianDate()}"))
            compose.onNodeWithText("Дата: ${postponedDate.toRussianDate()}").assertIsDisplayed()
            compose.onNodeWithText("Дата: ${ironDate.toRussianDate()}").assertDoesNotExist()
            capture("plant-card-postponed-order")
        } finally { runBlocking { app.repository.deletePlantCard(card) } }
    }
}
