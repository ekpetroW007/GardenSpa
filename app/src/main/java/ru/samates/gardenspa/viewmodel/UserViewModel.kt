package ru.samates.gardenspa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.samates.gardenspa.data.climate.ClimateService
import ru.samates.gardenspa.presentation.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import ru.samates.gardenspa.domain.ClimateFingerprint
import ru.samates.gardenspa.domain.GardenLocation
import ru.samates.gardenspa.domain.RegistrationState

sealed interface ClimateSetupState {
    data object Idle : ClimateSetupState
    data object Loading : ClimateSetupState
    data class Success(val locationName: String) : ClimateSetupState
    data class Error(val message: String) : ClimateSetupState
}

class UserViewModel(
    private val preferencesManager: PreferencesManager,
    private val climateService: ClimateService
) : ViewModel() {

    val offer = preferencesManager.offer
    private val _registrationState = MutableStateFlow<RegistrationState?>(null)
    val registrationState: StateFlow<RegistrationState?> = _registrationState
    private val _registrationError = MutableStateFlow<String?>(null)
    val registrationError: StateFlow<String?> = _registrationError
    private val _registrationSaving = MutableStateFlow(false)
    val registrationSaving: StateFlow<Boolean> = _registrationSaving
    private var registrationJob: Job? = null

    private val _userLogin = MutableStateFlow("Гость")
    val userLogin: StateFlow<String> = _userLogin

    private val _userWeightKg = MutableStateFlow(70.0)
    val userWeightKg: StateFlow<Double> = _userWeightKg

    private val _gardenLocation = MutableStateFlow<GardenLocation?>(null)
    val gardenLocation: StateFlow<GardenLocation?> = _gardenLocation

    private val _climateFingerprint = MutableStateFlow<ClimateFingerprint?>(null)
    val climateFingerprint: StateFlow<ClimateFingerprint?> = _climateFingerprint

    private val _climateSetupState = MutableStateFlow<ClimateSetupState>(ClimateSetupState.Idle)
    val climateSetupState: StateFlow<ClimateSetupState> = _climateSetupState

    private val _largeInterface = MutableStateFlow(false)
    val largeInterface: StateFlow<Boolean> = _largeInterface

    private val _highContrast = MutableStateFlow(false)
    val highContrast: StateFlow<Boolean> = _highContrast

    init {
        reloadRegistration()

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

        viewModelScope.launch {
            preferencesManager.gardenLocation.collect { location ->
                _gardenLocation.value = location
            }
        }

        viewModelScope.launch {
            preferencesManager.climateFingerprint.collect { fingerprint ->
                _climateFingerprint.value = fingerprint
            }
        }

        viewModelScope.launch {
            preferencesManager.largeInterface.collect { _largeInterface.value = it }
        }

        viewModelScope.launch {
            preferencesManager.highContrast.collect { _highContrast.value = it }
        }
    }

    fun reloadRegistration() {
        registrationJob?.cancel()
        _registrationError.value = null
        registrationJob = viewModelScope.launch {
            try {
                preferencesManager.registrationState.collect { _registrationState.value = it }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _registrationState.value = null
                _registrationError.value = "Не удалось прочитать настройки. Повторите попытку."
            }
        }
    }

    fun registerUser(login: String, accepted: Boolean) {
        if (_registrationSaving.value) return
        _registrationError.value = null
        if (login.isBlank() || !accepted) {
            _registrationError.value = "Укажите имя и примите Договор оферты."
            return
        }
        _registrationSaving.value = true
        viewModelScope.launch {
            try {
                preferencesManager.completeRegistration(login, accepted)
                // Navigation is unlocked only by the persisted DataStore state, never optimistically.
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _registrationError.value = "Не удалось сохранить принятие условий. Повторите попытку."
            } finally {
                _registrationSaving.value = false
            }
        }
    }

    fun setLargeInterface(enabled: Boolean) {
        _largeInterface.value = enabled
        viewModelScope.launch { preferencesManager.setLargeInterface(enabled) }
    }

    fun setHighContrast(enabled: Boolean) {
        _highContrast.value = enabled
        viewModelScope.launch { preferencesManager.setHighContrast(enabled) }
    }

    fun updateWeightKg(weightKg: Double) {
        viewModelScope.launch {
            preferencesManager.setUserWeightKg(weightKg)
        }
    }

    fun configureClimate(location: GardenLocation) {
        viewModelScope.launch {
            _climateSetupState.value = ClimateSetupState.Loading
            runCatching { climateService.calculateFingerprint(location) }
                .onSuccess { fingerprint ->
                    preferencesManager.setGardenClimate(location, fingerprint)
                    _gardenLocation.value = location
                    _climateFingerprint.value = fingerprint
                    _climateSetupState.value = ClimateSetupState.Success(location.localityName)
                }
                .onFailure { error ->
                    _climateSetupState.value = ClimateSetupState.Error(
                        error.message ?: "Не удалось рассчитать климатический профиль"
                    )
                }
        }
    }

    fun refreshClimate() {
        _gardenLocation.value?.let(::configureClimate)
    }

    fun resetClimateSetupState() {
        _climateSetupState.value = ClimateSetupState.Idle
    }
}
