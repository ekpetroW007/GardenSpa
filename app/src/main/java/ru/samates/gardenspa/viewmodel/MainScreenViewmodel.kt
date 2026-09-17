package ru.samates.gardenspa.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle

class MainScreenViewmodel(private val savedState: SavedStateHandle = SavedStateHandle()) : ViewModel() {
    private var history = savedState.get<ArrayList<String>>("screens") ?: arrayListOf("Сегодня")
    var selectedScreen by mutableStateOf(history.last())
        private set
    val canGoBack: Boolean get() = history.size > 1

    fun changeScreen(newText: String) {
        if (newText == selectedScreen) return
        history = ArrayList(history + newText)
        selectedScreen = newText
        savedState["screens"] = history
    }

    fun goBack() {
        if (!canGoBack) return
        history = ArrayList(history.dropLast(1))
        selectedScreen = history.last()
        savedState["screens"] = history
    }
}
