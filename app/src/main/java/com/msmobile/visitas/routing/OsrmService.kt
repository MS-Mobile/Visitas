package com.msmobile.visitas.routing

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OsrmService {

    @GET("route/v1/driving/{coordinates}")
    suspend fun getRoute(
        @Path("coordinates") coordinates: String,
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "geojson",
        @Query("steps") steps: Boolean = false
    ): Response<OsrmRouteResponse>

    @GET("trip/v1/driving/{coordinates}")
    suspend fun optimizeTrip(
        @Path("coordinates") coordinates: String,
        @Query("roundtrip") roundtrip: String = "false",
        @Query("source") source: String = "first",
        @Query("destination") destination: String = "any",
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "geojson",
        @Query("steps") steps: Boolean = false
    ): Response<OsrmRouteResponse>
}
