package ru.samates.gardenspa.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.MonthDay
import ru.samates.gardenspa.domain.ClimateConfidence
import ru.samates.gardenspa.domain.ClimateFingerprint
import ru.samates.gardenspa.domain.GardenLocation
import ru.samates.gardenspa.domain.LocationSource

@Entity(tableName = "garden")
data class GardenEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "location_name") val locationName: String? = null,
    @ColumnInfo(name = "latitude") val latitude: Double? = null,
    @ColumnInfo(name = "longitude") val longitude: Double? = null,
    @ColumnInfo(name = "elevation_meters") val elevationMeters: Int? = null,
    @ColumnInfo(name = "location_source") val locationSource: String? = null,
    @ColumnInfo(name = "location_accuracy_km") val locationAccuracyKm: Double? = null,
    @ColumnInfo(name = "climate_safe_spring_day") val climateSafeSpringDay: String? = null,
    @ColumnInfo(name = "climate_safe_autumn_day") val climateSafeAutumnDay: String? = null,
    @ColumnInfo(name = "climate_frost_free_days") val climateFrostFreeDays: Int? = null,
    @ColumnInfo(name = "climate_gdd_5") val climateGdd5: Double? = null,
    @ColumnInfo(name = "climate_gdd_10") val climateGdd10: Double? = null,
    @ColumnInfo(name = "climate_warm_precipitation") val climateWarmPrecipitation: Double? = null,
    @ColumnInfo(name = "climate_winter_minimum_p10") val climateWinterMinimumP10: Double? = null,
    @ColumnInfo(name = "climate_confidence") val climateConfidence: String? = null,
    @ColumnInfo(name = "climate_source_years") val climateSourceYears: Int? = null,
    @ColumnInfo(name = "climate_updated_at") val climateUpdatedAt: String? = null
)

fun GardenEntity.locationOrNull(): GardenLocation? {
    val lat = latitude ?: return null
    val lon = longitude ?: return null
    return GardenLocation(
        latitude = lat,
        longitude = lon,
        elevationMeters = elevationMeters,
        localityName = locationName.orEmpty().ifBlank { name },
        source = locationSource?.let { runCatching { LocationSource.valueOf(it) }.getOrNull() }
            ?: LocationSource.MANUAL_COORDINATES,
        accuracyKm = locationAccuracyKm ?: 5.0
    )
}

fun GardenEntity.climateOrNull(): ClimateFingerprint? {
    return ClimateFingerprint(
        safeSpringDay = climateSafeSpringDay?.let { runCatching { MonthDay.parse(it) }.getOrNull() } ?: return null,
        safeAutumnDay = climateSafeAutumnDay?.let { runCatching { MonthDay.parse(it) }.getOrNull() } ?: return null,
        frostFreeDays = climateFrostFreeDays ?: return null,
        growingDegreeDays5 = climateGdd5 ?: return null,
        growingDegreeDays10 = climateGdd10 ?: return null,
        warmSeasonPrecipitationMm = climateWarmPrecipitation ?: return null,
        winterMinimumP10 = climateWinterMinimumP10 ?: return null,
        confidence = climateConfidence?.let { runCatching { ClimateConfidence.valueOf(it) }.getOrNull() }
            ?: ClimateConfidence.LOW,
        sourceYears = climateSourceYears ?: return null
    )
}
