package ru.samates.gardenspa.data.climate

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import ru.samates.gardenspa.domain.GardenLocation
import ru.samates.gardenspa.domain.LocationSource
import kotlin.coroutines.resume

class AndroidGardenLocationResolver(context: Context) {
    private val appContext = context.applicationContext

    @Suppress("DEPRECATION")
    suspend fun resolvePlace(query: String): GardenLocation = withContext(Dispatchers.IO) {
        require(query.isNotBlank()) { "Введите населённый пункт" }
        require(Geocoder.isPresent()) { "На устройстве недоступен поиск населённых пунктов" }
        val address = Geocoder(appContext, Locale("ru", "RU"))
            .getFromLocationName(query.trim(), 1)
            ?.firstOrNull()
            ?: error("Не удалось найти населённый пункт. Уточните название.")
        GardenLocation(
            latitude = address.latitude,
            longitude = address.longitude,
            localityName = listOfNotNull(address.locality, address.adminArea)
                .distinct()
                .joinToString(", ")
                .ifBlank { query.trim() },
            source = LocationSource.PLACE_SEARCH,
            accuracyKm = 5.0
        )
    }

    @SuppressLint("MissingPermission")
    suspend fun resolveApproximateDeviceLocation(): GardenLocation {
        check(
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        ) { "Нет разрешения на приблизительное местоположение" }
        val locationManager = appContext.getSystemService(LocationManager::class.java)
        val location = bestLastKnownLocation(locationManager)
            ?: withTimeoutOrNull(12_000L) { requestSingleNetworkLocation(locationManager) }
            ?: error("Не удалось определить местоположение. Введите населённый пункт.")
        return GardenLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            elevationMeters = location.altitude.takeIf { location.hasAltitude() }?.toInt(),
            localityName = reverseGeocode(location) ?: "Текущее местоположение",
            source = LocationSource.APPROXIMATE_DEVICE,
            accuracyKm = (location.accuracy / 1_000.0).coerceAtLeast(1.0)
        )
    }

    @SuppressLint("MissingPermission")
    private fun bestLastKnownLocation(locationManager: LocationManager): Location? =
        listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .filter { provider -> runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false) }
            .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
            .maxWithOrNull(compareBy<Location> { it.time }.thenByDescending { -it.accuracy })

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private suspend fun requestSingleNetworkLocation(locationManager: LocationManager): Location? =
        suspendCancellableCoroutine { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    if (continuation.isActive) continuation.resume(location)
                }

                override fun onProviderDisabled(provider: String) {
                    locationManager.removeUpdates(this)
                    if (continuation.isActive) continuation.resume(null)
                }

                override fun onProviderEnabled(provider: String) = Unit

                @Deprecated("Deprecated in Android")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            }
            continuation.invokeOnCancellation { locationManager.removeUpdates(listener) }
            runCatching {
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, null)
            }.onFailure {
                if (continuation.isActive) continuation.resume(null)
            }
        }

    @Suppress("DEPRECATION")
    private suspend fun reverseGeocode(location: Location): String? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        runCatching {
            val address = Geocoder(appContext, Locale("ru", "RU"))
                .getFromLocation(location.latitude, location.longitude, 1)
                ?.firstOrNull()
            listOfNotNull(address?.locality, address?.adminArea)
                .distinct()
                .joinToString(", ")
                .ifBlank { null }
        }.getOrNull()
    }
}
