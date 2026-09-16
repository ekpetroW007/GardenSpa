package ru.samates.gardenspa

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.samates.gardenspa.others.MainActivity

@RunWith(AndroidJUnit4::class)
class ManualCareScrollInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun openingManualCareScrollsToNewFieldsIncludingOnRepeatedClicks() {
        composeRule.waitForIdle()
        if (composeRule.onAllNodesWithText("Как к вам обращаться?").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Test")
            composeRule.onNodeWithText("Начать настройку").performClick()
        }
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Добавить растение"))
        composeRule.onNodeWithText("Добавить растение").performClick()
        composeRule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Азалия")
        closeSoftKeyboard()

        repeat(2) {
            composeRule.onNodeWithText("Настроить уход самостоятельно").performScrollTo().performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithText("Работы по уходу").assertIsDisplayed()
            composeRule.onNodeWithText("Работа 1").assertIsDisplayed()
        }
    }

    @Test
    fun openingGardenActivityScrollsToFormAndReopeningDoesTheSame() {
        composeRule.waitForIdle()
        if (composeRule.onAllNodesWithText("Как к вам обращаться?").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Test")
            composeRule.onNodeWithText("Начать настройку").performClick()
        }
        val activityButton = "Садовая активность и калории"
        repeat(2) { attempt ->
            composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(activityButton))
            composeRule.onNodeWithText(activityButton).performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithText("Сколько энергии вы потратили?").assertIsDisplayed()
            if (attempt == 0) {
                composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(activityButton))
                composeRule.onNodeWithText(activityButton).performClick()
                composeRule.onNodeWithText("Сколько энергии вы потратили?").assertDoesNotExist()
            }
        }
    }
}
