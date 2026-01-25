package com.msmobile.visitas.visit

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VisitMapData(
    val householderName: String,
    val visitSubject: String,
    val householderAddress: String,
    val householderLatitude: Double,
    val householderLongitude: Double,
    val householderDistance: Int?,
    val visitOrder: Int,
    val routeGeometry: String? = null
)
