package ru.samates.gardenspa

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
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
import ru.samates.gardenspa.data.database.entity.*
import ru.samates.gardenspa.domain.*
import ru.samates.gardenspa.presentation.*
import ru.samates.gardenspa.ui.theme.MyApplicationTheme
import ru.samates.gardenspa.viewmodel.PlantsViewmodel

@RunWith(AndroidJUnit4::class)
class VariantWorkflowInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as BookeeperApp
    private val today get() = LocalDate.now()
    private fun rows(card: String) = runBlocking { app.repository.getAllPlantsOnce().filter { it.plantCardId == card } }
    private fun history(card: String) = runBlocking { app.repository.getAllProceduresOnce().filter { it.plantCardId == card } }

    private fun capture(name: String) {
        compose.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.waitForIdle(500, 5_000)
        val bitmap = automation.takeScreenshot()
        val directory = File(app.getExternalFilesDir(null), "variant-workflow-qa").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun chooseComponent(group: EradicationComponentGroup, optionId: String) {
        compose.onNode(hasContentDescription("${group.title}. Сейчас:", substring = true)).performScrollTo().performClick()
        val option = group.options.single { it.id == optionId }
        compose.onNode(hasText(option.product) and hasAnyAncestor(isPopup())).performClick()
    }

    @Test fun springChoiceCollapsesOtherRecipesAndCanBeExpandedAndChanged() {
        var selected by mutableStateOf<String?>(null)
        var choices by mutableStateOf("")
        compose.setContent { MyApplicationTheme { BotanicalBackground {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                ProgramVariantChooser(EradicationCare.EARLY_STEP, selected, choices,
                    onSelect = { variant, picked -> selected = variant; choices = picked })
            }
        } } }
        compose.onNodeWithTag("choose-variant:urea_young").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("urea_young", selected) }
        val matureProduct = EradicationCare.springVariants.single { it.id == "urea_mature" }.product
        compose.onNodeWithText(matureProduct).assertDoesNotExist()
        compose.onNode(hasText("Развернуть") and hasAnyAncestor(hasTestTag("variant:copper")))
            .performScrollTo().performClick()
        chooseComponent(EradicationCare.copperComponents.single(), "bordeaux_liquid")
        compose.onNodeWithTag("choose-variant:copper").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("copper", selected)
            assertEquals("bordeaux_liquid", ProgramVariants.choices(choices)["copper_source"])
        }
        compose.onNodeWithText(EradicationCare.springVariants.single { it.id == "urea_young" }.product).assertDoesNotExist()
        compose.onNode(hasText("Развернуть") and hasAnyAncestor(hasTestTag("variant:urea_young")))
            .performScrollTo().assertIsDisplayed()
        capture("spring-selected-copper")
    }

    @Test fun previewAutumnPersistsOnlyTheChosenInsecticideAndFungicide() {
        val card = UUID.randomUUID().toString()
        val template = PlantCareCatalog.all().single { it.id == "apple" }
        val source = template.steps.single { it.id == EradicationCare.AUTUMN_MIX_STEP }
        var program by mutableStateOf(GeneratedCareProgram(card, "apple", template.version, "Яблоня — проверка осени",
            CultivationType.OPEN_GROUND, today, today, "Проверка вариантов", null,
            listOf(GeneratedCareStep(source.id, source.title, today, today, today, null, false, false,
                "Проверка календарной даты", source.productDescription, source.note))))
        val model = PlantsViewmodel(app.repository)
        var error by mutableStateOf<String?>(null)
        try {
            compose.setContent { MyApplicationTheme {
                CareProgramPreviewDialog(program, { program = it }, error, false, {}, {
                    model.importCareProgram(program, null, "Проверка вариантов", onError = { error = it })
                })
            } }
            compose.onNodeWithText(EradicationCare.AUTUMN_FIXED_INGREDIENTS).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(EradicationCare.AUTUMN_ADJUVANT_NOTICE).performScrollTo().assertIsDisplayed()
            chooseComponent(EradicationCare.autumnComponents.single { it.id == "insecticide" }, "coragen")
            chooseComponent(EradicationCare.autumnComponents.single { it.id == "fungicide" }, "raek")
            compose.onNodeWithTag("choose-variant:autumn_main").performScrollTo().performClick()
            compose.onNodeWithText("Добавить растение и работы").performClick()
            compose.waitUntil(10_000) { rows(card).isNotEmpty() || error != null }
            assertNull(error)
            val saved = rows(card).single { it.programStepId == EradicationCare.AUTUMN_MIX_STEP }
            assertEquals("autumn_main", saved.selectedVariantId)
            assertEquals(mapOf("insecticide" to "coragen", "fungicide" to "raek"), ProgramVariants.choices(saved.variantChoices))
            assertTrue(saved.drugName.contains("Кораген"))
            assertTrue(saved.drugName.contains("Раёк"))
            assertTrue(saved.drugName.contains("Эпин, 1 мл"))
            assertTrue(saved.drugName.contains("прилипатель, 10 мл"))
            assertFalse(saved.drugName.contains("Актара"))
            assertFalse(saved.drugName.contains("Скор"))
            assertFalse(saved.drugName.contains("Топаз"))
            capture("autumn-chosen-components")
        } finally { runBlocking { app.repository.deletePlantCard(card) } }
    }

    @Test fun calendarArchivesCopperChoiceAndCommentThenReopensAllVariantsNextYear() {
        val card = UUID.randomUUID().toString()
        val template = PlantCareCatalog.all().single { it.id == "apple" }
        val source = template.steps.single { it.id == EradicationCare.EARLY_STEP }
        val seed = PlantEntity(plantName = "Яблоня — проверка вариантов", taskName = source.title,
            wateringInterval = 1, creationDate = today.toString(), drugId = null, gardenId = null,
            drugName = source.productDescription.orEmpty(), gardenName = "Проверка вариантов",
            plantCardId = card, programId = "apple", programVersion = template.version,
            programStepId = source.id, programNote = source.note).withProgramVariant("copper", "copper_source=copper_mature")
        runBlocking { app.repository.insertPlant(seed) }
        try {
            val plant = rows(card).single()
            lateinit var navigation: NavHostController
            compose.setContent { MyApplicationTheme {
                navigation = rememberNavController()
                NavHost(navigation, "calendar") {
                    composable("calendar") { BotanicalBackground { Calendar(PaddingValues(), navigation) } }
                    composable("card") { PlantDetails(navigation, plant.id) }
                }
            } }
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("variant:copper"))
            chooseComponent(EradicationCare.copperComponents.single(), "bordeaux_liquid")
            compose.waitUntil(10_000) {
                rows(card).first { it.id == plant.id }.variantChoices.contains("bordeaux_liquid")
            }
            val selected = rows(card).first { it.id == plant.id }
            assertTrue(selected.drugName.contains("Бордоская жидкость"))
            compose.onNode(hasText("Выполнено") and hasAnyAncestor(hasTestTag("variant:copper")))
                .performScrollTo().performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Сохранить работу на следующий год?").fetchSemanticsNodes().isNotEmpty()
            }
            val comment = "Почки закрыты. Выбрана бордоская жидкость; замечания сохранены."
            compose.onNodeWithText("Комментарий к выполненной работе").performTextInput(comment)
            Espresso.closeSoftKeyboard()
            capture("completed-variant-comment")
            compose.onNodeWithText("Да, сохранить").performClick()
            compose.waitUntil(10_000) {
                history(card).singleOrNull()?.comment == comment && rows(card).any { it.programImportKey?.startsWith("archive-next-season:") == true }
            }
            val archived = history(card).single()
            assertEquals("copper", archived.selectedVariantId)
            assertEquals(selected.variantChoices, archived.variantChoices)
            assertEquals(selected.drugName, archived.drugName)
            val next = rows(card).single { it.programImportKey == archived.nextSeasonImportKey }
            assertEquals(archived.nextSeasonDate().toString(), next.creationDate)
            assertNull(next.selectedVariantId)
            assertEquals("", next.variantChoices)
            compose.runOnIdle { navigation.navigate("card") }
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("choose-variant:urea_young"))
            listOf("urea_young", "urea_mature", "copper").forEach {
                compose.onNodeWithTag("choose-variant:$it").assertExists()
            }
            capture("next-year-all-variants")
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Комментарий: $comment"))
            compose.onNodeWithText("Комментарий: $comment").assertIsDisplayed()
            capture("archived-variant-comment")
        } finally { runBlocking { app.repository.deletePlantCard(card) } }
    }

    @Test fun decliningNextSeasonReturnsTheEditedComment() {
        val procedure = ProcedureEntity(id = 42, plantId = 12, procedureName = "Проверка комментария",
            scheduledDate = today.toString(), completedDate = today.toString(), status = "COMPLETED",
            comment = "Исходная заметка")
        var savedComment: String? = null
        var confirmed = false
        compose.setContent { MyApplicationTheme {
            NextSeasonDialog(procedure, false, null, { savedComment = it }, { confirmed = true })
        } }
        compose.onNodeWithText("Комментарий к выполненной работе").performTextReplacement("Заметка после выполнения")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Нет, только в архив").performClick()
        compose.runOnIdle {
            assertEquals("Заметка после выполнения", savedComment)
            assertFalse(confirmed)
        }
    }
}
