package com.msmobile.visitas.util

import javax.inject.Inject

data class LatLong(
    val latitude: Double,
    val longitude: Double
)

class LatLongParser @Inject constructor() {
    // Matches a plain "lat,long" or "lat long" string in its entirety.
    private val plainPattern = Regex(
        """^\s*(-?\d+(?:\.\d+)?)\s*[,\s]\s*(-?\d+(?:\.\d+)?)\s*$"""
    )

    // Ordered list of patterns used to extract a coordinate pair from within a
    // larger string such as a map URL. Each pattern captures latitude in group 1
    // and longitude in group 2. Patterns require decimal coordinates so unrelated
    // URL segments (e.g. "!3m4!1e1" in Google Maps links) are not matched.
    private val urlPatterns = listOf(
        // Query parameters: ?q=lat,long, &query=lat,long, ll=, destination=, etc.
        Regex(
            """[?&](?:q|query|ll|sll|saddr|daddr|destination|center)=""" +
                """(-?\d+\.\d+)\s*,\s*(-?\d+\.\d+)"""
        ),
        // Map center / street view: @lat,long
        Regex("""@(-?\d+\.\d+)\s*,\s*(-?\d+\.\d+)"""),
        // Path segment: /place/lat,long or /lat,long
        Regex("""/(-?\d+\.\d+)\s*,\s*(-?\d+\.\d+)"""),
        // Generic fallback: first decimal coordinate pair anywhere in the string.
        Regex("""(-?\d+\.\d+)\s*,\s*(-?\d+\.\d+)""")
    )

    fun parse(input: String): Result<LatLong> {
        plainPattern.matchEntire(input)?.let { match ->
            return buildResult(match.groupValues[1], match.groupValues[2])
        }

        for (pattern in urlPatterns) {
            val match = pattern.find(input) ?: continue
            val result = buildResult(match.groupValues[1], match.groupValues[2])
            if (result.isSuccess) {
                return result
            }
        }

        return Result.failure(IllegalArgumentException("Invalid lat,long format"))
    }

    private fun buildResult(latText: String, longText: String): Result<LatLong> {
        val latitude = latText.toDoubleOrNull()
            ?: return Result.failure(IllegalArgumentException("Invalid latitude"))
        val longitude = longText.toDoubleOrNull()
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
