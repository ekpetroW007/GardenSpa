package ru.samates.gardenspa.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle

class MainScreenViewmodel(private val savedState: SavedStateHandle = SavedStateHandle()) : ViewModel() {
    private var history = ArrayList((savedState.get<ArrayList<String>>("screens") ?: arrayListOf("Календарь"))
        .map(::currentRoute).fold(emptyList<String>()) { pages, page ->
            if (pages.lastOrNull() == page) pages else pages + page
        }.ifEmpty { listOf("Календарь") })
    var selectedScreen by mutableStateOf(history.last())
        private set
    val canGoBack: Boolean get() = history.size > 1

    init { savedState["screens"] = history }

    fun changeScreen(newText: String) {
        val route = currentRoute(newText)
        if (route == selectedScreen) return
        history = ArrayList(history + route)
        selectedScreen = route
        savedState["screens"] = history
    }

    fun goBack() {
        if (!canGoBack) return
        history = ArrayList(history.dropLast(1))
        selectedScreen = history.last()
        savedState["screens"] = history
    }

    private fun currentRoute(route: String): String = when (route) {
        "Сегодня", "Главная", "Профиль" -> "Календарь"
        "Настройки" -> "Помощь"
        else -> route
    }
}
