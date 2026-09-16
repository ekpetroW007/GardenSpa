package ru.samates.gardenspa

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesOf
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.samates.gardenspa.data.climate.ClimateService
import ru.samates.gardenspa.presentation.ExpandableInfo
import ru.samates.gardenspa.presentation.PreferencesManager
import ru.samates.gardenspa.presentation.RegistrationForm
import ru.samates.gardenspa.presentation.navigation.AppNavigation
import ru.samates.gardenspa.ui.theme.MyApplicationTheme
import ru.samates.gardenspa.viewmodel.UserViewModel

@RunWith(AndroidJUnit4::class)
class OfferInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun explicitAcceptanceAndNameAreRequiredAndReadingDoesNotAccept() {
        val document = PreferencesManager(context).offer
        var result: Pair<String, Boolean>? = null
        compose.setContent {
            MyApplicationTheme { RegistrationForm("", document, false, null) { name, accepted -> result = name to accepted } }
        }
        compose.onNodeWithText("Продолжить").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Я принимаю Договор оферты").performScrollTo().performClick()
        compose.onNodeWithText("Продолжить").assertIsNotEnabled()
        compose.onNodeWithText("Я принимаю Договор оферты").performClick()
        compose.onNodeWithText("Имя или псевдоним").performScrollTo().performTextInput("Анна")
        compose.onNodeWithText("Продолжить").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Договор оферты").performScrollTo().performClick()
        compose.onNodeWithText("ДОГОВОР ОФЕРТЫ GARDENSPA", substring = true).assertExists()
        compose.onNodeWithText("Закрыть документ").performClick()
        compose.onNodeWithText("Продолжить").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Я принимаю Договор оферты").performScrollTo().performClick()
        compose.onNodeWithText("Продолжить").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("Анна" to true, result) }
    }

    @Test fun oldInstallationNeedsAcceptanceAndAtomicRecordSurvivesReopening() = runBlocking {
        val file = File(context.cacheDir, "offer-${UUID.randomUUID()}.preferences_pb")
        var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        var store = PreferenceDataStoreFactory.create(scope = scope) { file }
        try {
            store.edit {
                it[PreferencesManager.IS_REGISTERED] = true
                it[PreferencesManager.USER_LOGIN] = "Мария"
                it[PreferencesManager.USER_WEIGHT_KG] = "72.0"
            }
            var manager = PreferencesManager(context, store)
            assertEquals("Мария", manager.registrationState.first().name)
            assertFalse(manager.registrationState.first().canEnter)
            assertTrue(runCatching { manager.completeRegistration("Мария", false) }.isFailure)
            assertTrue(runCatching { manager.completeRegistration(" ", true) }.isFailure)
            assertNull(store.data.first()[PreferencesManager.OFFER_ACCEPTED_AT])
            manager.completeRegistration(" Мария ", true)
            assertTrue(manager.registrationState.first().canEnter)
            val saved = store.data.first()
            assertEquals(manager.offer.text, saved[PreferencesManager.OFFER_TEXT])
            assertEquals(manager.offer.sha256, saved[PreferencesManager.OFFER_SHA256])
            assertEquals(context.packageManager.getPackageInfo(context.packageName, 0).versionName, saved[PreferencesManager.OFFER_APP_VERSION])
            assertTrue(saved[PreferencesManager.OFFER_ACCEPTED_AT]!! > 0)
            scope.coroutineContext[Job]!!.cancelAndJoin()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            store = PreferenceDataStoreFactory.create(scope = scope) { file }
            manager = PreferencesManager(context, store)
            assertTrue(manager.registrationState.first().canEnter)
            assertEquals("Мария", manager.registrationState.first().name)
            assertEquals(72.0, manager.userWeightKg.first(), 0.0)
            store.edit { it[PreferencesManager.OFFER_VERSION] = "older" }
            assertFalse(manager.registrationState.first().canEnter)
            store.edit { it[PreferencesManager.OFFER_VERSION] = manager.offer.version; it[PreferencesManager.OFFER_SHA256] = "changed" }
            assertFalse(manager.registrationState.first().canEnter)
        } finally {
            scope.coroutineContext[Job]!!.cancelAndJoin()
            file.delete()
        }
    }

    @Test fun failedSaveDoesNotOpenTheMainScreenForAnExistingProfile() {
        val data = preferencesOf(PreferencesManager.IS_REGISTERED to true, PreferencesManager.USER_LOGIN to "Анна")
        val failingStore = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flowOf(data)
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = throw IOException("Test write failure")
        }
        val holder = ViewModelStore()
        lateinit var vm: UserViewModel
        compose.runOnIdle { vm = UserViewModel(PreferencesManager(context, failingStore), ClimateService()); holder.put("offer", vm) }
        try {
            compose.setContent { MyApplicationTheme { AppNavigation(vm) } }
            compose.waitUntil(10_000) { vm.registrationState.value != null }
            compose.onNodeWithText("Сегодня", useUnmergedTree = true).assertDoesNotExist()
            compose.onNodeWithText("Я принимаю Договор оферты").performScrollTo().performClick()
            compose.onNodeWithText("Продолжить").performScrollTo().performClick()
            compose.waitUntil(10_000) { vm.registrationError.value != null }
            compose.onNodeWithText("Не удалось сохранить принятие условий. Повторите попытку.").assertExists()
            compose.runOnIdle { assertFalse(vm.registrationState.value!!.canEnter) }
            compose.onNodeWithText("Сегодня", useUnmergedTree = true).assertDoesNotExist()
        } finally { compose.runOnIdle { holder.clear() } }
    }

    @Test fun detailsAreCollapsedButCanBeRead() {
        compose.setContent { MyApplicationTheme { ExpandableInfo("Инструкция и ограничения", "Проверочная инструкция") } }
        compose.onNodeWithText("Проверочная инструкция").assertDoesNotExist()
        compose.onNodeWithText("Инструкция и ограничения ▾").performClick()
        compose.onNodeWithText("Проверочная инструкция").assertIsDisplayed()
        compose.onNodeWithText("Инструкция и ограничения ▴").performClick()
        compose.onNodeWithText("Проверочная инструкция").assertDoesNotExist()
    }

    @Test fun readErrorKeepsNavigationClosedAndCanBeRetried() {
        var failRead = true
        val store = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow {
                if (failRead) throw IOException("Test read failure")
                emit(preferencesOf(PreferencesManager.USER_LOGIN to "Анна"))
            }
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = error("Not used")
        }
        val holder = ViewModelStore()
        lateinit var vm: UserViewModel
        compose.runOnIdle { vm = UserViewModel(PreferencesManager(context, store), ClimateService()); holder.put("offer", vm) }
        try {
            compose.setContent { MyApplicationTheme { AppNavigation(vm) } }
            compose.waitUntil(10_000) { vm.registrationError.value != null }
            compose.onNodeWithText("Сегодня", useUnmergedTree = true).assertDoesNotExist()
            compose.onNodeWithText("Не удалось прочитать настройки. Повторите попытку.").assertExists()
            compose.runOnIdle { failRead = false }
            compose.onNodeWithText("Повторить").performClick()
            compose.waitUntil(10_000) { vm.registrationState.value != null }
            compose.onNodeWithText("Я принимаю Договор оферты").assertExists()
            compose.onNodeWithText("Продолжить").performScrollTo().assertIsNotEnabled()
        } finally { compose.runOnIdle { holder.clear() } }
    }

    @Test fun savedConsentOpensMainAndSettingsKeepDocumentAvailable() {
        val file = File(context.cacheDir, "offer-ui-${UUID.randomUUID()}.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = scope) { file }
        val holder = ViewModelStore()
        lateinit var vm: UserViewModel
        compose.runOnIdle { vm = UserViewModel(PreferencesManager(context, store), ClimateService()); holder.put("offer", vm) }
        try {
            compose.setContent { MyApplicationTheme { AppNavigation(vm) } }
            compose.waitUntil(10_000) { vm.registrationState.value != null }
            compose.onNodeWithText("Имя или псевдоним").performTextInput("Анна")
            compose.onNodeWithText("Я принимаю Договор оферты").performScrollTo().performClick()
            compose.onNodeWithText("Я принимаю Договор оферты").assertIsOn()
            compose.onNodeWithText("Продолжить").performScrollTo().assertIsEnabled().performClick()
            compose.waitUntil(10_000) { vm.registrationState.value?.canEnter == true || vm.registrationError.value != null }
            compose.runOnIdle { assertNull(vm.registrationError.value); assertTrue(vm.registrationState.value!!.canEnter) }
            compose.onNodeWithText("Я принимаю Договор оферты").assertDoesNotExist()
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Настройки и помощь"))
            compose.onNodeWithText("Настройки и помощь").performClick()
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Договор оферты"))
            compose.onNodeWithText("Договор оферты").performClick()
            compose.onNodeWithText("ДОГОВОР ОФЕРТЫ GARDENSPA", substring = true).assertExists()
            compose.onNodeWithText("Закрыть документ").performClick()
            compose.onNodeWithText("Настройки").assertExists()
        } finally {
            compose.runOnIdle { holder.clear() }
            runBlocking { scope.coroutineContext[Job]!!.cancelAndJoin() }
            file.delete()
        }
    }
}
