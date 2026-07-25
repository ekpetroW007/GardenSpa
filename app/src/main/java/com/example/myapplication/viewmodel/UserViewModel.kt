package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.presentation.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserViewModel(private val preferencesManager: PreferencesManager) : ViewModel() {

    private val _isRegistered = MutableStateFlow(false)
    val isRegistered: StateFlow<Boolean> = _isRegistered

    private val _userLogin = MutableStateFlow("Гость")
    val userLogin: StateFlow<String> = _userLogin

    init {
        viewModelScope.launch {
            preferencesManager.isRegistered.collect { registered ->
                _isRegistered.value = registered
            }
        }

        viewModelScope.launch {
            preferencesManager.userLogin.collect { login ->
                _userLogin.value = login
            }
        }
    }

    fun registerUser(login: String) {
        viewModelScope.launch {
            preferencesManager.setUserLogin(login)
            preferencesManager.setRegistered(true)
        }
    }
}