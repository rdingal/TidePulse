package com.example.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.model.BeachLocation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class LocationProvider(private val context: Context) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        val coarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return coarse || fine
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentBeachLocation(): Result<BeachLocation> {
        if (!hasLocationPermission()) {
            return Result.failure(SecurityException("Location permission not granted"))
        }

        val tokenSource = CancellationTokenSource()
        val currentLocation = runCatching {
            fusedClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                tokenSource.token
            ).awaitOrNull(onCancel = { tokenSource.cancel() })
        }.getOrNull()

        val resolvedLocation = currentLocation ?: runCatching {
            fusedClient.lastLocation.awaitOrNull()
        }.getOrNull()

        if (resolvedLocation == null) {
            return Result.failure(
                IllegalStateException("Unable to acquire GPS fix. Try enabling location services or search a beach manually.")
            )
        }

        val (name, subtitle) = reverseGeocode(resolvedLocation.latitude, resolvedLocation.longitude)
        return Result.success(
            BeachLocation(
                id = "gps_${"%.4f".format(Locale.US, resolvedLocation.latitude)}_${"%.4f".format(Locale.US, resolvedLocation.longitude)}",
                name = name,
                subtitle = subtitle,
                latitude = resolvedLocation.latitude,
                longitude = resolvedLocation.longitude,
                isGps = true,
            )
        )
    }

    private suspend fun reverseGeocode(lat: Double, lon: Double): Pair<String, String> =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) {
                return@withContext "Current Location" to formatCoords(lat, lon)
            }
            val geocoder = Geocoder(context, Locale.getDefault())
            val address: Address? = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(lat, lon, 1) { addresses ->
                            if (cont.isActive) {
                                cont.resume(addresses.firstOrNull())
                            }
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
                }
            } catch (_: Exception) {
                null
            }

            if (address == null) {
                "Current GPS Location" to formatCoords(lat, lon)
            } else {
                val primary = address.locality
                    ?: address.subLocality
                    ?: address.subAdminArea
                    ?: address.featureName
                    ?: "Current GPS Location"
                val region = listOfNotNull(address.adminArea, address.countryName)
                    .distinct()
                    .joinToString(", ")
                    .ifBlank { formatCoords(lat, lon) }
                primary to region
            }
        }

    private fun formatCoords(lat: Double, lon: Double): String {
        val latDir = if (lat >= 0) "N" else "S"
        val lonDir = if (lon >= 0) "E" else "W"
        return String.format(
            Locale.US,
            "%.3f° %s, %.3f° %s",
            kotlin.math.abs(lat),
            latDir,
            kotlin.math.abs(lon),
            lonDir
        )
    }
}

private suspend fun <T> Task<T>.awaitOrNull(onCancel: (() -> Unit)? = null): T? =
    suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { onCancel?.invoke() }
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener {
            if (cont.isActive) cont.resume(null)
        }
        addOnCanceledListener {
            if (cont.isActive) cont.resume(null)
        }
    }
