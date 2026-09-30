package ru.samates.gardenspa

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.samates.gardenspa.data.database.entity.GardenWorkEntity
import ru.samates.gardenspa.notifications.GardenWorkReminderScheduler
import ru.samates.gardenspa.presentation.*
import ru.samates.gardenspa.ui.theme.MyApplicationTheme
import ru.samates.gardenspa.viewmodel.*

@RunWith(AndroidJUnit4::class)
class HelpCalendarInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as BookeeperApp

    private fun withPreferences(block: (PreferencesManager, UserViewModel) -> Unit) {
        val file = File(app.cacheDir, "help-${UUID.randomUUID()}.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = scope) { file }
        val manager = PreferencesManager(app, store)
        val holder = ViewModelStore()
        lateinit var user: UserViewModel
        compose.runOnIdle { user = UserViewModel(manager, app.climateService); holder.put("help", user) }
        try { block(manager, user) }
        finally {
            compose.runOnIdle { holder.clear() }
            runBlocking { scope.coroutineContext[Job]!!.cancelAndJoin() }
            file.delete()
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.waitForIdle(500, 5_000)
        val bitmap = automation.takeScreenshot()
        val directory = File(app.getExternalFilesDir(null), "help-calendar-qa").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun calendarIsFirstAndLegacyTodayHistoryReturnsToCalendar() = withPreferences { _, user ->
        val main = MainScreenViewmodel()
        compose.setContent { MyApplicationTheme { MainScreen(main, rememberNavController(), user) } }
        compose.runOnIdle { assertEquals("Календарь", main.selectedScreen); assertFalse(main.canGoBack) }
        val labels = listOf("Календарь работ", "Мои сады и растения", "Средства и народные рецепты", "Помощь и настройки")
        val positions = labels.map { compose.onNodeWithContentDescription(it).fetchSemanticsNode().boundsInRoot.left }
        assertEquals(positions.sorted(), positions)
        compose.onNodeWithContentDescription("Календарь работ").assertIsSelected()
        compose.onNodeWithText("Сегодня", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("Логотип GardenSpa").assertIsDisplayed()
        compose.onNodeWithContentDescription("Назад").assertDoesNotExist()
        compose.onNodeWithContentDescription("Следующий месяц").assertIsDisplayed()
        val monthTop = compose.onNodeWithContentDescription("Следующий месяц").fetchSemanticsNode().boundsInRoot.top
        val addTop = compose.onNodeWithText("Добавить работу").fetchSemanticsNode().boundsInRoot.top
        assertTrue(monthTop < addTop)
        capture("calendar-default")
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Погодное окно"))
        compose.onNodeWithText("Погодное окно").assertIsDisplayed()
        compose.onNodeWithContentDescription("Помощь и настройки").performClick()
        compose.onNodeWithText("Настройки и подсказки").assertExists()
        compose.onNodeWithContentDescription("Назад").performClick()
        compose.runOnIdle { assertEquals("Календарь", main.selectedScreen) }
        compose.runOnIdle {
            val restored = MainScreenViewmodel(SavedStateHandle(mapOf("screens" to arrayListOf("Сегодня", "Календарь", "Настройки"))))
            assertEquals("Помощь", restored.selectedScreen)
            restored.goBack()
            assertEquals("Календарь", restored.selectedScreen)
            assertFalse(restored.canGoBack)
            restored.changeScreen("Профиль")
            assertFalse(restored.canGoBack)
        }
    }

    @Test fun activityTogglePersistsAndDoesNotDeleteCalendarRecords() = withPreferences { preferences, user ->
        val activityDate = LocalDate.of(2001, 1, 9)
        val oldEntries = runBlocking { app.repository.getGardenWorkForDate(activityDate) }
        val record = GardenWorkEntity(workDate = activityDate.toString(), activityCode = "test",
            activityName = "Проверочная работа", minutes = 20, met = 3.0, weightKg = 70.0, calories = 70.0)
        runBlocking {
            preferences.setGardenActivityEnabled(false)
            app.repository.replaceGardenWorkForDate(activityDate, listOf(record))
        }
        val originalReminder = GardenWorkReminderScheduler.isEnabled(app)
        try {
            val main = MainScreenViewmodel()
            compose.setContent { MyApplicationTheme { MainScreen(main, rememberNavController(), user) } }
            compose.onNodeWithContentDescription("Помощь и настройки").performClick()
            listOf("Крупный интерфейс", "Повышенный контраст", "Напоминания об уходе", "Садовая активность",
                "Вечерний подсчёт садовой активности", "Данные и приватность", "Договор оферты").forEach {
                compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(it))
                compose.onNodeWithText(it).assertExists()
            }
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasContentDescription("Садовая активность"))
            compose.onNodeWithContentDescription("Садовая активность").assertIsOff().performClick()
            compose.waitUntil(10_000) { user.gardenActivityEnabled.value }
            assertTrue(runBlocking { preferences.gardenActivityEnabled.first() })
            assertEquals(originalReminder, GardenWorkReminderScheduler.isEnabled(app))
            capture("help-activity-enabled")
            compose.onNodeWithContentDescription("Календарь работ").performClick()
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Садовая активность"))
            compose.onNodeWithText("Садовая активность")
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Свёрнуто"))
            compose.onNodeWithText("Сколько энергии вы потратили?").assertDoesNotExist()
            capture("calendar-activity-collapsed")
            compose.onNodeWithText("Садовая активность").performClick()
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Сколько энергии вы потратили?"))
            compose.onNodeWithText("Сколько энергии вы потратили?").assertIsDisplayed()
            capture("calendar-activity-enabled")
            compose.onNodeWithText("Ваш вес, кг").performTextReplacement("76")
            compose.onNodeWithText("Минут").performScrollTo().performTextReplacement("42")
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Садовая активность"))
            compose.onNodeWithText("Садовая активность").performClick()
            compose.onNodeWithText("Сколько энергии вы потратили?").assertDoesNotExist()
            compose.onNodeWithText("Садовая активность").performClick()
            compose.onNodeWithText("Ваш вес, кг").assertTextContains("76")
            compose.onNodeWithText("Минут").assertTextContains("42")
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Садовая активность"))
            compose.onNodeWithText("Садовая активность").performClick()
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Следующие 7 дней"))
            compose.onNodeWithText("Следующие 7 дней").assertIsDisplayed()
            compose.onNodeWithContentDescription("Помощь и настройки").performClick()
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasContentDescription("Садовая активность"))
            compose.onNodeWithContentDescription("Садовая активность").assertIsOn().performClick()
            compose.waitUntil(10_000) { !user.gardenActivityEnabled.value }
            assertFalse(runBlocking { preferences.gardenActivityEnabled.first() })
            compose.onNodeWithContentDescription("Календарь работ").performClick()
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Следующие 7 дней"))
            compose.onNodeWithText("Садовая активность").assertDoesNotExist()
            compose.onNodeWithText("Сколько энергии вы потратили?").assertDoesNotExist()
            val retained = runBlocking { app.repository.getGardenWorkForDate(activityDate) }
            assertEquals(1, retained.size)
            assertEquals(record.activityName, retained.single().activityName)
            assertEquals(originalReminder, GardenWorkReminderScheduler.isEnabled(app))
        } finally { runBlocking { app.repository.replaceGardenWorkForDate(activityDate, oldEntries) } }
    }

    @Test fun newActivityPreferenceDefaultsOffAndRetainsExplicitPreviousUse() = withPreferences { preferences, _ ->
        val reminder = GardenWorkReminderScheduler.isEnabled(app)
        try {
            GardenWorkReminderScheduler.setEnabled(app, false)
            assertFalse(runBlocking { preferences.gardenActivityEnabled.first() })
            runBlocking { preferences.setUserWeightKg(75.0) }
            assertTrue(runBlocking { preferences.gardenActivityEnabled.first() })
            runBlocking { preferences.setGardenActivityEnabled(false) }
            assertFalse(runBlocking { preferences.gardenActivityEnabled.first() })
        } finally { GardenWorkReminderScheduler.setEnabled(app, reminder) }
    }
}
