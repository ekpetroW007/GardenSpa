package ru.samates.gardenspa.presentation


import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import java.time.MonthDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.samates.gardenspa.domain.ClimateConfidence
import ru.samates.gardenspa.domain.ClimateFingerprint
import ru.samates.gardenspa.domain.GardenLocation
import ru.samates.gardenspa.domain.LocationSource

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class PreferencesManager(context: Context) {
    private val dataStore = context.dataStore

    companion object {
        val IS_REGISTERED = booleanPreferencesKey("is_registered")
        val USER_LOGIN = stringPreferencesKey("user_login")
        val USER_WEIGHT_KG = stringPreferencesKey("user_weight_kg")
        val GARDEN_LOCALITY = stringPreferencesKey("garden_locality")
        val GARDEN_LATITUDE = stringPreferencesKey("garden_latitude")
        val GARDEN_LONGITUDE = stringPreferencesKey("garden_longitude")
        val GARDEN_ELEVATION = stringPreferencesKey("garden_elevation")
        val GARDEN_LOCATION_SOURCE = stringPreferencesKey("garden_location_source")
        val GARDEN_LOCATION_ACCURACY_KM = stringPreferencesKey("garden_location_accuracy_km")
        val CLIMATE_SAFE_SPRING_DAY = stringPreferencesKey("climate_safe_spring_day")
        val CLIMATE_SAFE_AUTUMN_DAY = stringPreferencesKey("climate_safe_autumn_day")
        val CLIMATE_FROST_FREE_DAYS = stringPreferencesKey("climate_frost_free_days")
        val CLIMATE_GDD_5 = stringPreferencesKey("climate_gdd_5")
        val CLIMATE_GDD_10 = stringPreferencesKey("climate_gdd_10")
        val CLIMATE_WARM_PRECIPITATION = stringPreferencesKey("climate_warm_precipitation")
        val CLIMATE_WINTER_MINIMUM_P10 = stringPreferencesKey("climate_winter_minimum_p10")
        val CLIMATE_CONFIDENCE = stringPreferencesKey("climate_confidence")
        val CLIMATE_SOURCE_YEARS = stringPreferencesKey("climate_source_years")
        val LARGE_INTERFACE = booleanPreferencesKey("large_interface")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
    }

    suspend fun setRegistered(isRegistered: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_REGISTERED] = isRegistered
        }
    }

    suspend fun setUserLogin(login: String) {
        dataStore.edit { preferences ->
            preferences[USER_LOGIN] = login
        }
    }

    suspend fun setUserWeightKg(weightKg: Double) {
        dataStore.edit { preferences ->
            preferences[USER_WEIGHT_KG] = weightKg.toString()
        }
    }

    suspend fun setGardenClimate(location: GardenLocation, fingerprint: ClimateFingerprint) {
        dataStore.edit { preferences ->
            preferences[GARDEN_LOCALITY] = location.localityName
            preferences[GARDEN_LATITUDE] = location.latitude.toString()
            preferences[GARDEN_LONGITUDE] = location.longitude.toString()
            preferences[GARDEN_LOCATION_SOURCE] = location.source.name
            preferences[GARDEN_LOCATION_ACCURACY_KM] = location.accuracyKm.toString()
            location.elevationMeters?.let { preferences[GARDEN_ELEVATION] = it.toString() }
                ?: preferences.remove(GARDEN_ELEVATION)
            preferences[CLIMATE_SAFE_SPRING_DAY] = fingerprint.safeSpringDay.toString()
            preferences[CLIMATE_SAFE_AUTUMN_DAY] = fingerprint.safeAutumnDay.toString()
            preferences[CLIMATE_FROST_FREE_DAYS] = fingerprint.frostFreeDays.toString()
            preferences[CLIMATE_GDD_5] = fingerprint.growingDegreeDays5.toString()
            preferences[CLIMATE_GDD_10] = fingerprint.growingDegreeDays10.toString()
            preferences[CLIMATE_WARM_PRECIPITATION] = fingerprint.warmSeasonPrecipitationMm.toString()
            preferences[CLIMATE_WINTER_MINIMUM_P10] = fingerprint.winterMinimumP10.toString()
            preferences[CLIMATE_CONFIDENCE] = fingerprint.confidence.name
            preferences[CLIMATE_SOURCE_YEARS] = fingerprint.sourceYears.toString()
        }
    }

    suspend fun setLargeInterface(enabled: Boolean) {
        dataStore.edit { it[LARGE_INTERFACE] = enabled }
    }

    suspend fun setHighContrast(enabled: Boolean) {
        dataStore.edit { it[HIGH_CONTRAST] = enabled }
    }

    val isRegistered: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[IS_REGISTERED] ?: false
        }

    val userLogin: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[USER_LOGIN] ?: "Гость"
        }

    val userWeightKg: Flow<Double> = dataStore.data
        .map { preferences ->
            preferences[USER_WEIGHT_KG]?.toDoubleOrNull() ?: 70.0
        }

    val largeInterface: Flow<Boolean> = dataStore.data.map { it[LARGE_INTERFACE] ?: false }

    val highContrast: Flow<Boolean> = dataStore.data.map { it[HIGH_CONTRAST] ?: false }

    val gardenLocation: Flow<GardenLocation?> = dataStore.data.map { preferences ->
        val latitude = preferences[GARDEN_LATITUDE]?.toDoubleOrNull() ?: return@map null
        val longitude = preferences[GARDEN_LONGITUDE]?.toDoubleOrNull() ?: return@map null
        GardenLocation(
            latitude = latitude,
            longitude = longitude,
            elevationMeters = preferences[GARDEN_ELEVATION]?.toIntOrNull(),
            localityName = preferences[GARDEN_LOCALITY].orEmpty().ifBlank { "Выбранная точка" },
            source = preferences[GARDEN_LOCATION_SOURCE]
                ?.let { runCatching { LocationSource.valueOf(it) }.getOrNull() }
                ?: LocationSource.MANUAL_COORDINATES,
            accuracyKm = preferences[GARDEN_LOCATION_ACCURACY_KM]?.toDoubleOrNull() ?: 5.0
        )
    }

    val climateFingerprint: Flow<ClimateFingerprint?> = dataStore.data.map { preferences ->
        val spring = preferences[CLIMATE_SAFE_SPRING_DAY]
            ?.let { runCatching { MonthDay.parse(it) }.getOrNull() }
            ?: return@map null
        val autumn = preferences[CLIMATE_SAFE_AUTUMN_DAY]
            ?.let { runCatching { MonthDay.parse(it) }.getOrNull() }
            ?: return@map null
        ClimateFingerprint(
            safeSpringDay = spring,
            safeAutumnDay = autumn,
            frostFreeDays = preferences[CLIMATE_FROST_FREE_DAYS]?.toIntOrNull() ?: return@map null,
            growingDegreeDays5 = preferences[CLIMATE_GDD_5]?.toDoubleOrNull() ?: return@map null,
            growingDegreeDays10 = preferences[CLIMATE_GDD_10]?.toDoubleOrNull() ?: return@map null,
            warmSeasonPrecipitationMm = preferences[CLIMATE_WARM_PRECIPITATION]?.toDoubleOrNull() ?: return@map null,
            winterMinimumP10 = preferences[CLIMATE_WINTER_MINIMUM_P10]?.toDoubleOrNull() ?: return@map null,
            confidence = preferences[CLIMATE_CONFIDENCE]
                ?.let { runCatching { ClimateConfidence.valueOf(it) }.getOrNull() }
                ?: ClimateConfidence.LOW,
            sourceYears = preferences[CLIMATE_SOURCE_YEARS]?.toIntOrNull() ?: return@map null
        )
    }
}
