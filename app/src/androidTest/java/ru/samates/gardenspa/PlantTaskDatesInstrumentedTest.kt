package ru.samates.gardenspa

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.samates.gardenspa.data.database.entity.GardenEntity
import ru.samates.gardenspa.others.MainActivity
import ru.samates.gardenspa.presentation.toRussianDate

@RunWith(AndroidJUnit4::class)
class PlantTaskDatesInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun independentDatesSurviveSavingEditingAndDeletingAnotherTask() {
        val repository = (composeRule.activity.application as BookeeperApp).repository
        val gardenName = "Тест дат ${System.nanoTime()}"
        val gardenId = runBlocking { repository.insertGarden(GardenEntity(name = gardenName)).toInt() }
        val plantName = "Азалия тест дат"
        fun rows() = runBlocking { repository.getAllPlantsOnce().filter { it.gardenId == gardenId } }
        try {
            composeRule.waitUntil(30_000) {
                composeRule.onAllNodes(hasScrollToIndexAction()).fetchSemanticsNodes().isNotEmpty() ||
                    composeRule.onAllNodesWithText("Как к вам обращаться?").fetchSemanticsNodes().isNotEmpty()
            }
            if (composeRule.onAllNodesWithText("Как к вам обращаться?").fetchSemanticsNodes().isNotEmpty()) {
                composeRule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Test")
                composeRule.onNodeWithText("Начать настройку").performClick()
            }
            composeRule.waitUntil(10_000) {
                composeRule.onAllNodes(hasScrollToIndexAction()).fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Добавить растение"))
            composeRule.onNodeWithText("Добавить растение").performClick()
            composeRule.onAllNodes(hasSetTextAction()).onFirst().performTextInput(plantName)
            closeSoftKeyboard()
            composeRule.onNode(hasContentDescription("Сад. Сейчас:", substring = true)).performScrollTo().performClick()
            composeRule.onNode(hasText(gardenName) and hasAnyAncestor(isPopup())).performClick()
            composeRule.onNodeWithText("Настроить уход самостоятельно").performScrollTo().performClick()
            composeRule.onNodeWithText("Работа 1").performTextInput("Обрезка")
            closeSoftKeyboard()
            val firstDate = LocalDate.now().withDayOfMonth(1).plusMonths(1).withDayOfMonth(5)
            val secondDate = firstDate.withDayOfMonth(12)
            chooseDate(1, firstDate, capture = true)
            composeRule.onNodeWithText("+ Добавить ещё одну работу").performScrollTo().performClick()
            composeRule.onNodeWithText("Работа 2").performScrollTo().performTextInput("Обработка")
            closeSoftKeyboard()
            chooseDate(2, secondDate, cancel = true)
            composeRule.onNodeWithContentDescription("Дата начала работы 2")
                .assertTextContains("Начало: ${LocalDate.now().toRussianDate()}")
            chooseDate(2, secondDate)
            composeRule.onNodeWithText("Сохранить растение").performScrollTo().performClick()
            composeRule.waitUntil(10_000) { rows().size == 2 }
            val saved = rows().associateBy { it.taskName }
            assertEquals(firstDate.toString(), saved.getValue("Обрезка").creationDate)
            assertEquals(secondDate.toString(), saved.getValue("Обработка").creationDate)

            composeRule.onNodeWithText("Мой сад").performClick()
            composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(plantName))
            composeRule.onNodeWithText(plantName).performClick()
            composeRule.onNodeWithText("Редактировать карточку").performScrollTo().performClick()
            composeRule.onNodeWithContentDescription("Дата начала работы 1")
                .assertTextContains("Начало: ${firstDate.toRussianDate()}")
            composeRule.onNodeWithContentDescription("Дата начала работы 2")
                .assertTextContains("Начало: ${secondDate.toRussianDate()}")
            composeRule.onAllNodesWithText("Удалить").onFirst().performScrollTo().performClick()
            composeRule.onNodeWithText("Сохранить изменения").performScrollTo().performClick()
            composeRule.waitUntil(10_000) { rows().size == 1 }
            assertEquals(saved.getValue("Обработка").id, rows().single().id)
            assertEquals(secondDate.toString(), rows().single().creationDate)
            assertEquals("Обработка", rows().single().taskName)
        } finally {
            runBlocking {
                rows().forEach { repository.deletePlant(it.id) }
                repository.deleteGarden(gardenId)
            }
        }
    }

    private fun chooseDate(index: Int, date: LocalDate, cancel: Boolean = false, capture: Boolean = false) {
        composeRule.onNodeWithContentDescription("Дата начала работы $index").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Следующий месяц").performClick()
        composeRule.onNodeWithContentDescription(date.toRussianDate()).performClick()
        composeRule.waitForIdle()
        if (capture) {
            val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            File(composeRule.activity.getExternalFilesDir(null), "task-date-calendar.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        }
        composeRule.onNodeWithText(if (cancel) "Отмена" else "Выбрать дату").performClick()
    }
}
