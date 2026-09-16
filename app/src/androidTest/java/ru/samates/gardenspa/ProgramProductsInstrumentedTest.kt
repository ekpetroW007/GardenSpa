package ru.samates.gardenspa

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.samates.gardenspa.data.database.AppDatabase
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import ru.samates.gardenspa.domain.*
import ru.samates.gardenspa.presentation.ProgramProductDialog
import ru.samates.gardenspa.ui.theme.MyApplicationTheme

@RunWith(AndroidJUnit4::class)
class ProgramProductsInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val date = LocalDate.now()

    @Test fun canSkipDiagnosisAndConfirmOneProduct() {
        var chosen: String? = null
        var diagnosis: String? = "not-called"
        compose.setContent {
            MyApplicationTheme {
                ProgramProductDialog("tomato", onDismiss = {}, onConfirm = { product, problem, _, _, _ ->
                    chosen = product
                    diagnosis = problem
                })
            }
        }
        compose.onNodeWithText("Перейти к препаратам").assertIsNotEnabled()
        compose.onNodeWithText("Открытый грунт").performClick()
        compose.onNodeWithText("Перейти к препаратам").performClick()
        compose.onNodeWithText("Подробнее: Биозащитин, концентрат 5 мл").performScrollTo().performClick()
        compose.onNodeWithText("Добавить обработку").assertIsNotEnabled()
        compose.onNodeWithText("Я сверил(а) этикетку: средство подходит для моей культуры, цели и условий")
            .performScrollTo().performClick()
        compose.onNodeWithText("Добавить обработку").performClick()
        compose.runOnIdle { assertEquals("biozashchitin", chosen); assertNull(diagnosis) }
    }

    @Test fun unsafeCandidateCannotBeConfirmed() {
        var confirmed = false
        compose.setContent {
            MyApplicationTheme {
                ProgramProductDialog("cucumber", initialCultivation = CultivationType.GREENHOUSE,
                    onDismiss = {}, onConfirm = { _, _, _, _, _ -> confirmed = true })
            }
        }
        compose.onNodeWithText("Вредитель: Паутинный клещ").performScrollTo().performClick()
        compose.onNodeWithText("Перейти к препаратам").performClick()
        compose.onNodeWithText("Подробнее: MAXI Гумат противоклещ с метаризином").performScrollTo().performClick()
        compose.onNodeWithText("Добавить обработку").assertIsNotEnabled()
        compose.runOnIdle { assertFalse(confirmed) }
    }

    @Test fun replacementAndAdditionalWorkPersistAndHistoryBlocksDestructiveReplacement() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "product-qa-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, AppDatabase::class.java, databaseName).build()
        var db = open()
        try {
            val input = PlantEntity(
                plantName = "Томат", taskName = "Профилактика", wateringInterval = 21,
                creationDate = date.toString(), drugId = null, gardenId = null, drugName = "Фитоспорин", gardenName = "Тест",
                plantCardId = "test-card", programId = "tomato", programVersion = 4, programStepId = "fitosporin_spraying",
                programImportKey = "test-key", repeatType = "CUSTOM", repeatInterval = 21, repeatEndType = "COUNT", repeatCount = 2,
                photoUri = "file:///private/photo.png"
            )
            db.plantDao().insertPlant(input)
            val original = db.plantDao().getAllPlantsOnce().single()
            val replaced = original.withProgramProduct("silver_prevention", date.plusDays(1), 5)
            db.plantDao().replaceUnusedProgramProduct(original, replaced)
            val additional = replaced.problemTreatment("whitefly", "boverix", CultivationType.GREENHOUSE, date, 0, "fixed-request")
            db.plantDao().addProgramTreatment(replaced, additional)
            db.close()
            db = open()
            val saved = db.plantDao().getAllPlantsOnce()
            assertEquals(2, saved.size)
            assertEquals(replaced, saved.first { it.id == replaced.id })
            assertEquals(1, scheduledTreatmentsOn(saved, emptyList(), date).size)
            assertEquals(1, scheduledTreatmentsOn(saved, emptyList(), date.plusDays(1)).size)
            assertTrue(scheduledTreatmentsOn(saved, emptyList(), date.plusDays(22)).isEmpty())
            val history = ProcedureEntity(plantId = replaced.id, procedureName = "Старая работа", scheduledDate = date.toString(),
                completedDate = date.toString(), status = "COMPLETED", note = "Старая инструкция")
            db.procedureDao().insertProcedure(history)
            val attempt = runCatching {
                db.plantDao().replaceUnusedProgramProduct(replaced, replaced.withProgramProduct("silver_prevention", date.plusDays(2), 1))
            }
            assertTrue(attempt.isFailure)
            assertEquals(replaced, db.plantDao().getAllPlantsOnce().first { it.id == replaced.id })
            assertEquals("Старая инструкция", db.procedureDao().getAllProceduresOnce().single().note)
            val stale = runCatching { db.plantDao().addProgramTreatment(original, additional) }
            assertTrue(stale.isFailure)
            assertEquals(2, db.plantDao().getAllPlantsOnce().size)
        } finally {
            db.close()
            context.deleteDatabase(databaseName)
        }
    }
}
