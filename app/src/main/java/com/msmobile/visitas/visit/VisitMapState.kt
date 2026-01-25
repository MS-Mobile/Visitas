package com.msmobile.visitas.visit

sealed class VisitMapState {
    object Empty : VisitMapState()
    object Error : VisitMapState()
    object Loading : VisitMapState()
    data class Visits(
        val serialized: String
    ) : VisitMapState()
}