package com.example.myapplication.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class MainScreenViewmodel : ViewModel() {
    var selectedScreen by mutableStateOf("Профиль")
    fun changeScreen(newText: String) {
        selectedScreen = newText
    }
}