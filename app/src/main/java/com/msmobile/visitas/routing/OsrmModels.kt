package com.msmobile.visitas.routing

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OsrmRouteRequest(
    val coordinates: List<List<Double>>
)

@JsonClass(generateAdapter = true)
data class OsrmRouteResponse(
    @param:Json(name = "routes")
    val routes: List<OsrmRoute>?,
    @param:Json(name = "waypoints")
    val waypoints: List<OsrmWaypoint>?,
    @param:Json(name = "code")
    val code: String
)

@JsonClass(generateAdapter = true)
data class OsrmRoute(
    @param:Json(name = "geometry")
    val geometry: String,
    @param:Json(name = "legs")
    val legs: List<OsrmLeg>,
    @param:Json(name = "distance")
    val distance: Double,
    @param:Json(name = "duration")
    val duration: Double,
    @param:Json(name = "weight_name")
    val weightName: String,
    @param:Json(name = "weight")
    val weight: Double
)

@JsonClass(generateAdapter = true)
data class OsrmLeg(
    @param:Json(name = "steps")
    val steps: List<OsrmStep>,
    @param:Json(name = "distance")
    val distance: Double,
    @param:Json(name = "duration")
    val duration: Double,
    @param:Json(name = "summary")
    val summary: String,
    @param:Json(name = "weight")
    val weight: Double
)

@JsonClass(generateAdapter = true)
data class OsrmStep(
    @param:Json(name = "intersections")
    val intersections: List<OsrmIntersection>,
    @param:Json(name = "driving_side")
    val drivingSide: String,
    @param:Json(name = "geometry")
    val geometry: String,
    @param:Json(name = "mode")
    val mode: String,
    @param:Json(name = "maneuver")
    val maneuver: OsrmManeuver,
    @param:Json(name = "weight")
    val weight: Double,
    @param:Json(name = "duration")
    val duration: Double,
    @param:Json(name = "name")
    val name: String,
    @param:Json(name = "distance")
    val distance: Double
)

@JsonClass(generateAdapter = true)
data class OsrmIntersection(
    @param:Json(name = "out")
    val out: Int?,
    @param:Json(name = "entry")
    val entry: List<Boolean>,
    @param:Json(name = "bearings")
    val bearings: List<Int>,
    @param:Json(name = "location")
    val location: List<Double>,
    @param:Json(name = "in")
    val `in`: Int?
)

@JsonClass(generateAdapter = true)
data class OsrmManeuver(
    @param:Json(name = "bearing_after")
    val bearingAfter: Int,
    @param:Json(name = "bearing_before")
    val bearingBefore: Int,
    @param:Json(name = "location")
    val location: List<Double>,
    @param:Json(name = "modifier")
    val modifier: String?,
    @param:Json(name = "type")
    val type: String
)

@JsonClass(generateAdapter = true)
data class OsrmWaypoint(
    @param:Json(name = "hint")
    val hint: String,
    @param:Json(name = "distance")
    val distance: Double,
    @param:Json(name = "name")
    val name: String,
    @param:Json(name = "location")
    val location: List<Double>
)

data class RouteOptimizationResult(
    val orderedVisits: List<OptimizedVisit>,
    val routeGeometry: String?
)

data class OptimizedVisit(
    val originalIndex: Int,
    val optimizedOrder: Int,
    val latitude: Double,
    val longitude: Double
)
