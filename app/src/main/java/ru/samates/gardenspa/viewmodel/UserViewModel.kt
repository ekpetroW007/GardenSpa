package ru.samates.gardenspa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.samates.gardenspa.presentation.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserViewModel(private val preferencesManager: PreferencesManager) : ViewModel() {

    private val _isRegistered = MutableStateFlow(false)
    val isRegistered: StateFlow<Boolean> = _isRegistered

    private val _userLogin = MutableStateFlow("Гость")
    val userLogin: StateFlow<String> = _userLogin

    private val _userWeightKg = MutableStateFlow(70.0)
    val userWeightKg: StateFlow<Double> = _userWeightKg

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

        viewModelScope.launch {
            preferencesManager.userWeightKg.collect { weightKg ->
                _userWeightKg.value = weightKg
            }
        }
    }

    fun registerUser(login: String) {
        viewModelScope.launch {
            preferencesManager.setUserLogin(login)
            preferencesManager.setRegistered(true)
        }
    }

    fun updateWeightKg(weightKg: Double) {
        viewModelScope.launch {
            preferencesManager.setUserWeightKg(weightKg)
        }
    }
}
