package ru.samates.gardenspa.domain

import java.time.MonthDay
import java.util.Base64

data class GardenClimate(val location: GardenLocation, val fingerprint: ClimateFingerprint)

fun GardenClimate.encode(): String = listOf(
    "1",
    Base64.getUrlEncoder().withoutPadding().encodeToString(location.localityName.toByteArray()),
    location.latitude,
    location.longitude,
    location.elevationMeters ?: "",
    location.source.name,
    location.accuracyKm,
    fingerprint.safeSpringDay,
    fingerprint.safeAutumnDay,
    fingerprint.frostFreeDays,
    fingerprint.growingDegreeDays5,
    fingerprint.growingDegreeDays10,
    fingerprint.warmSeasonPrecipitationMm,
    fingerprint.winterMinimumP10,
    fingerprint.confidence.name,
    fingerprint.sourceYears
).joinToString("|")

fun String.decodeGardenClimate(): GardenClimate? = runCatching {
    val values = split('|')
    require(values.size == 16 && values[0] == "1")
    GardenClimate(
        GardenLocation(
            localityName = Base64.getUrlDecoder().decode(values[1]).toString(Charsets.UTF_8),
            latitude = values[2].toDouble(),
            longitude = values[3].toDouble(),
            elevationMeters = values[4].toIntOrNull(),
            source = LocationSource.valueOf(values[5]),
            accuracyKm = values[6].toDouble()
        ),
        ClimateFingerprint(
            safeSpringDay = MonthDay.parse(values[7]),
            safeAutumnDay = MonthDay.parse(values[8]),
            frostFreeDays = values[9].toInt(),
            growingDegreeDays5 = values[10].toDouble(),
            growingDegreeDays10 = values[11].toDouble(),
            warmSeasonPrecipitationMm = values[12].toDouble(),
            winterMinimumP10 = values[13].toDouble(),
            confidence = ClimateConfidence.valueOf(values[14]),
            sourceYears = values[15].toInt()
        )
    )
}.getOrNull()
