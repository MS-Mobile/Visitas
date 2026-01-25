package com.msmobile.visitas.routing

import com.msmobile.visitas.util.DispatcherProvider
import com.msmobile.visitas.util.Logger
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class OsrmRoutingProvider @Inject constructor(
    private val osrmService: OsrmService,
    private val dispatcherProvider: DispatcherProvider,
    private val logger: Logger
) {
    suspend fun optimizeVisitRoute(
        currentLocation: Pair<Double, Double>,
        visitLocations: List<Pair<Double, Double>>
    ): RouteOptimizationResult = withContext(dispatcherProvider.io) {
        try {
            if (visitLocations.isEmpty()) {
                return@withContext RouteOptimizationResult(
                    orderedVisits = emptyList(),
                    routeGeometry = null
                )
            }

            // Use nearest neighbor algorithm for more intuitive routing
            val orderedVisits = optimizeWithNearestNeighbor(currentLocation, visitLocations)

            // Try to get route geometry from OSRM for the optimized order
            val routeGeometry = try {
                getRouteGeometry(currentLocation, orderedVisits)
            } catch (e: Exception) {
                logger.error("OSRM", "Failed to get route geometry: ${e.message}", e)
                null
            }

            return@withContext RouteOptimizationResult(
                orderedVisits = orderedVisits,
                routeGeometry = routeGeometry
            )
        } catch (e: Exception) {
            logger.error("OSRM", "Error optimizing route: ${e.message}", e)

            // Fallback: return visits in original order
            val fallbackVisits = visitLocations.mapIndexed { index, (lat, lng) ->
                OptimizedVisit(
                    originalIndex = index,
                    optimizedOrder = index,
                    latitude = lat,
                    longitude = lng
                )
            }

            return@withContext RouteOptimizationResult(
                orderedVisits = fallbackVisits,
                routeGeometry = null
            )
        }
    }

    private fun optimizeWithNearestNeighbor(
        startLocation: Pair<Double, Double>,
        visitLocations: List<Pair<Double, Double>>
    ): List<OptimizedVisit> {
        val unvisited = visitLocations.mapIndexed { index, location ->
            index to location
        }.toMutableList()

        val orderedVisits = mutableListOf<OptimizedVisit>()
        var currentPos = startLocation

        // Nearest neighbor algorithm
        while (unvisited.isNotEmpty()) {
            // Find the closest unvisited location
            val closest = unvisited.minByOrNull { (_, location) ->
                calculateDistance(currentPos, location)
            }

            if (closest != null) {
                val (originalIndex, location) = closest
                unvisited.remove(closest)

                orderedVisits.add(
                    OptimizedVisit(
                        originalIndex = originalIndex,
                        optimizedOrder = orderedVisits.size,
                        latitude = location.first,
                        longitude = location.second
                    )
                )

                currentPos = location
            }
        }

        return orderedVisits
    }

    private suspend fun getRouteGeometry(
        startLocation: Pair<Double, Double>,
        orderedVisits: List<OptimizedVisit>
    ): String? {
        if (orderedVisits.isEmpty()) return null

        // Prepare coordinates for OSRM route request
        val allCoordinates = mutableListOf<List<Double>>().apply {
            // Start with current location
            add(listOf(startLocation.second, startLocation.first)) // OSRM uses [longitude, latitude]

            // Add all visit locations in order
            orderedVisits.forEach { visit ->
                add(listOf(visit.longitude, visit.latitude))
            }
        }

        val coordinatesString = allCoordinates.joinToString(";") { coords ->
            "${coords[0]},${coords[1]}"
        }

        return try {
            val response = osrmService.getRoute(coordinatesString)
            if (response.isSuccessful && response.body()?.code == "Ok") {
                response.body()?.routes?.firstOrNull()?.geometry
            } else {
                logger.error("OSRM", "Route request failed: ${response.body()?.code ?: "Unknown error"}")
                null
            }
        } catch (e: Exception) {
            logger.error("OSRM", "Error getting route geometry: ${e.message}", e)
            null
        }
    }

    // Calculate distance between two points using Haversine formula
    private fun calculateDistance(
        point1: Pair<Double, Double>,
        point2: Pair<Double, Double>
    ): Double {
        val lat1 = point1.first
        val lon1 = point1.second
        val lat2 = point2.first
        val lon2 = point2.second

        val earthRadius = 6371.0 // Earth radius in kilometers

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }
}
