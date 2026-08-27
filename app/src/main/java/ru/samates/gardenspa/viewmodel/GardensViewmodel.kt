package ru.samates.gardenspa.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.samates.gardenspa.data.database.entity.GardenEntity
import ru.samates.gardenspa.data.repository.BookeeperRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.samates.gardenspa.domain.ClimateFingerprint
import ru.samates.gardenspa.domain.GardenLocation
import java.time.LocalDateTime

class GardensViewmodel(
    private val repository: BookeeperRepository
) : ViewModel() {

    val gardens = repository.allGardens.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun gardenAdd(
        name: String,
        location: GardenLocation? = null,
        climate: ClimateFingerprint? = null,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val newGarden = GardenEntity(
                    name = name,
                    locationName = location?.localityName,
                    latitude = location?.latitude,
                    longitude = location?.longitude,
                    elevationMeters = location?.elevationMeters,
                    locationSource = location?.source?.name,
                    locationAccuracyKm = location?.accuracyKm,
                    climateSafeSpringDay = climate?.safeSpringDay?.toString(),
                    climateSafeAutumnDay = climate?.safeAutumnDay?.toString(),
                    climateFrostFreeDays = climate?.frostFreeDays,
                    climateGdd5 = climate?.growingDegreeDays5,
                    climateGdd10 = climate?.growingDegreeDays10,
                    climateWarmPrecipitation = climate?.warmSeasonPrecipitationMm,
                    climateWinterMinimumP10 = climate?.winterMinimumP10,
                    climateConfidence = climate?.confidence?.name,
                    climateSourceYears = climate?.sourceYears,
                    climateUpdatedAt = climate?.let { LocalDateTime.now().toString() }
                )
                repository.insertGarden(newGarden)
                onSaved()
            } catch (e: Exception) {
                Log.d("addGarden", e.toString())
            }
        }
    }

    fun updateClimate(garden: GardenEntity, location: GardenLocation, climate: ClimateFingerprint, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateGarden(
                garden.copy(
                    locationName = location.localityName,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    elevationMeters = location.elevationMeters,
                    locationSource = location.source.name,
                    locationAccuracyKm = location.accuracyKm,
                    climateSafeSpringDay = climate.safeSpringDay.toString(),
                    climateSafeAutumnDay = climate.safeAutumnDay.toString(),
                    climateFrostFreeDays = climate.frostFreeDays,
                    climateGdd5 = climate.growingDegreeDays5,
                    climateGdd10 = climate.growingDegreeDays10,
                    climateWarmPrecipitation = climate.warmSeasonPrecipitationMm,
                    climateWinterMinimumP10 = climate.winterMinimumP10,
                    climateConfidence = climate.confidence.name,
                    climateSourceYears = climate.sourceYears,
                    climateUpdatedAt = LocalDateTime.now().toString()
                )
            )
            onSaved()
        }
    }

    fun deleteGarden(id: Int) {
        viewModelScope.launch {
            try {
                repository.deleteGarden(id)
            } catch (e: Exception) {
                Log.d("deleteGarden", e.toString())
            }
        }
    }
}
