package com.msmobile.visitas.util

import javax.inject.Inject

data class LatLong(
    val latitude: Double,
    val longitude: Double
)

class LatLongParser @Inject constructor() {
    private val latLongPattern = Regex(
        """^\s*(-?\d+(?:\.\d+)?)\s*[,\s]\s*(-?\d+(?:\.\d+)?)\s*$"""
    )

    fun parse(input: String): Result<LatLong> {
        val match = latLongPattern.matchEntire(input)
            ?: return Result.failure(IllegalArgumentException("Invalid lat,long format"))

        val latitude = match.groupValues[1].toDoubleOrNull()
            ?: return Result.failure(IllegalArgumentException("Invalid latitude"))
        val longitude = match.groupValues[2].toDoubleOrNull()
            ?: return Result.failure(IllegalArgumentException("Invalid longitude"))

        if (latitude !in -90.0..90.0) {
            return Result.failure(IllegalArgumentException("Latitude must be between -90 and 90"))
        }
        if (longitude !in -180.0..180.0) {
            return Result.failure(IllegalArgumentException("Longitude must be between -180 and 180"))
        }

        return Result.success(LatLong(latitude, longitude))
    }
}

