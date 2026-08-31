package ru.samates.gardenspa

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.samates.gardenspa.others.MainActivity

@RunWith(AndroidJUnit4::class)
class PlantNameSuggestionsInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun suggestionsAppearBelowFieldAndCanBeScrolledAndSelected() {
        composeRule.waitForIdle()
        if (composeRule.onAllNodesWithText("Как к вам обращаться?").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Test")
            composeRule.onNodeWithText("Начать настройку").performClick()
        }

        composeRule.onNodeWithText("Добавить растение").performClick()
        composeRule.onNodeWithText("1. Что вы выращиваете?").assertIsDisplayed()
        val plantField = composeRule.onAllNodes(hasSetTextAction()).onFirst()
        plantField.performTextInput("о")

        val firstSuggestion = composeRule.onNodeWithText("Облепиха").assertIsDisplayed()
        assertTrue(
            "Подсказки должны располагаться под полем ввода",
            firstSuggestion.getUnclippedBoundsInRoot().top >= plantField.getUnclippedBoundsInRoot().bottom
        )

        composeRule.onNodeWithContentDescription("Подсказки растений")
            .performScrollToNode(hasText("Остеоспермум"))
        composeRule.onNodeWithText("Остеоспермум").assertIsDisplayed().performClick()
        plantField.assertTextContains("Остеоспермум")
    }
}
