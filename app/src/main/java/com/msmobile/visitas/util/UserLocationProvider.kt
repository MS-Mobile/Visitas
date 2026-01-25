package com.msmobile.visitas.util

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UserLocationProvider(private val context: Context) {
    private val _location = MutableStateFlow<UserLocation>(UserLocation.NotAvailable)
    val location: StateFlow<UserLocation> = _location

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }
    private val locationRequest: LocationRequest by lazy {
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_REQUEST_INTERVAL).apply {
            setMinUpdateIntervalMillis(LOCATION_REQUEST_FASTEST_INTERVAL)
            setMinUpdateDistanceMeters(LOCATION_REQUEST_SMALLEST_DISPLACEMENT)
        }.build()
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                _location.value = UserLocation.Available(location.latitude, location.longitude)
            }
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun startLocationUpdates() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                _location.value = UserLocation.Available(it.latitude, it.longitude)
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    sealed class UserLocation {
        data object NotAvailable : UserLocation()
        data class Available(val latitude: Double, val longitude: Double) : UserLocation()
    }

    companion object {
        private const val LOCATION_REQUEST_INTERVAL = 10000L // 10 seconds
        private const val LOCATION_REQUEST_FASTEST_INTERVAL = 5000L // 5 seconds
        private const val LOCATION_REQUEST_SMALLEST_DISPLACEMENT = 10f // 10 meters
    }
}