package com.msmobile.visitas.util

import android.annotation.SuppressLint
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class AddressProvider(
    private val geocoder: Geocoder,
    private val locationProviderClient: FusedLocationProviderClient,
    private val looper: Looper
) {
    suspend fun getAddressListFromCurrentLocation(): List<AddressSpecs> {
        // Get location with retries
        val location = getCurrentLocationWithRetry() ?: return listOf()

        // If we have a location, try to get addresses with retries
        return getAddressesFromLocationWithRetry(location)
    }

    suspend fun getAddressFromLatLong(latitude: Double, longitude: Double): AddressSpecs {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getAddressFromLatLongAsync(latitude, longitude)
        } else {
            getAddressFromLatLongSync(latitude, longitude)
        }
    }

    fun calculateDistance(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double
    ): AddressDistance {
        val results = FloatArray(1)
        Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, results)
        val distance = results.firstOrNull() ?: return AddressDistance.NoData
        return when {
            distance <= CLOSE_DISTANCE_IN_METERS -> AddressDistance.Nearby(distance)
            distance < MEDIUM_DISTANCE_IN_METERS -> AddressDistance.Medium(distance)
            else -> AddressDistance.FarAway(distance / 1000)
        }
    }

    private suspend fun getCurrentLocationWithRetry(): Location? {
        var attempts = 0
        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                val accuracy = LOCATION_ACCURACY + (attempts * LOCATION_ACCURACY_TOLERANCE)
                val location = requestLocation(accuracy)
                if (location != null) {
                    return location
                }
            } catch (_: Throwable) {
                // Log error if needed
            }

            attempts++
            if (attempts < MAX_RETRY_ATTEMPTS) {
                delay(RETRY_DELAY)
            }
        }
        return null
    }

    private suspend fun getAddressesFromLocationWithRetry(location: Location): List<AddressSpecs> {
        var attempts = 0
        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    getAddressesFromLocationAsync(location)
                } else {
                    getAddressesFromLocation(location)
                }
                if (addresses.isNotEmpty()) {
                    return addresses
                }
            } catch (_: Throwable) {
                // Log error if needed
            }

            attempts++
            if (attempts < MAX_RETRY_ATTEMPTS) {
                delay(RETRY_DELAY)
            }
        }
        return listOf()
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestLocation(accuracy: Int): Location? {
        return withTimeoutOrNull(LOCATION_REQUEST_INTERVAL) {
            suspendCancellableCoroutine { cont ->
                var locationListener: LocationListener? = null
                locationListener = LocationListener { location ->
                    if (location.hasAccuracy() && location.accuracy < accuracy) {
                        cont.resume(location)
                        locationListener?.let { listener ->
                            locationProviderClient.removeLocationUpdates(listener)
                        }
                    }
                }

                val locationRequest = LocationRequest
                    .Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_REQUEST_INTERVAL)
                    .setMinUpdateDistanceMeters(MIN_UPDATE_DISTANCE_IN_METERS)
                    .build()

                locationProviderClient.requestLocationUpdates(
                    locationRequest,
                    locationListener,
                    looper
                ).addOnFailureListener {
                    cont.resume(null)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun getAddressesFromLocationAsync(location: Location): List<AddressSpecs> {
        return withTimeoutOrNull(LOCATION_REQUEST_INTERVAL) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    MAX_ADDRESS_RESULTS
                ) { addresses ->
                    val addressSpecs = addresses.mapNotNull { address ->
                        if (address.thoroughfare == null) return@mapNotNull AddressSpecs.NoData
                        AddressSpecs.Data(
                            address = address.toStreetAddressString(),
                            latitude = address.latitude,
                            longitude = address.longitude
                        )
                    }
                    cont.resume(addressSpecs)
                }
            }
        } ?: listOf()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun getAddressFromLatLongAsync(
        latitude: Double,
        longitude: Double
    ): AddressSpecs {
        return withTimeoutOrNull(LOCATION_REQUEST_INTERVAL) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                    val address = addresses.firstOrNull() ?: run {
                        cont.resume(AddressSpecs.NoData)
                        return@getFromLocation
                    }
                    if (address.thoroughfare == null) {
                        cont.resume(AddressSpecs.NoData)
                    } else {
                        cont.resume(
                            AddressSpecs.Data(
                                address = address.toStreetAddressString(),
                                latitude = address.latitude,
                                longitude = address.longitude
                            )
                        )
                    }
                }
            }
        } ?: AddressSpecs.NoData
    }

    private fun getAddressesFromLocation(location: Location): List<AddressSpecs> {
        return geocoder.getFromLocation(location.latitude, location.longitude, MAX_ADDRESS_RESULTS)
            .orEmpty()
            .mapNotNull { address ->
                if (address.thoroughfare == null) return@mapNotNull AddressSpecs.NoData
                AddressSpecs.Data(
                    address = address.toStreetAddressString(),
                    latitude = address.latitude,
                    longitude = address.longitude
                )
            }
    }

    private fun getAddressFromLatLongSync(latitude: Double, longitude: Double): AddressSpecs {
        return try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            val address = addresses?.firstOrNull() ?: return AddressSpecs.NoData
            if (address.thoroughfare == null) {
                AddressSpecs.NoData
            } else {
                AddressSpecs.Data(
                    address = address.toStreetAddressString(),
                    latitude = address.latitude,
                    longitude = address.longitude
                )
            }
        } catch (_: Throwable) {
            AddressSpecs.NoData
        }
    }

    private fun Address.toStreetAddressString(): String = "$thoroughfare, $subThoroughfare"

    sealed class AddressSpecs {
        data object NoData : AddressSpecs()
        data class Data(
            val address: String,
            val latitude: Double,
            val longitude: Double
        ) : AddressSpecs()
    }

    sealed class AddressDistance {
        data object NoData : AddressDistance()
        data class Nearby(val distance: Float) : AddressDistance()
        data class Medium(val distance: Float) : AddressDistance()
        data class FarAway(val distance: Float) : AddressDistance()
    }

    private companion object {
        private const val LOCATION_REQUEST_INTERVAL = 5_000L
        private const val MAX_ADDRESS_RESULTS = 10
        private const val LOCATION_ACCURACY = 20
        private const val LOCATION_ACCURACY_TOLERANCE = 5
        private const val CLOSE_DISTANCE_IN_METERS = 100.0
        private const val MEDIUM_DISTANCE_IN_METERS = 500.0
        private const val MAX_RETRY_ATTEMPTS = 5
        private const val RETRY_DELAY = 2000L // 2 seconds
        private const val MIN_UPDATE_DISTANCE_IN_METERS = 0f
    }
}