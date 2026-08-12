package com.msmobile.visitas.preference

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.msmobile.visitas.visit.VisitListDateFilterOption
import com.msmobile.visitas.visit.VisitListDistanceFilterOption
import com.msmobile.visitas.visit.VisitMapEngineOption
import java.util.UUID

@Entity(tableName = "preference")
data class Preference(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val visitListDateFilterOption: VisitListDateFilterOption,
    val visitListDistanceFilterOption: VisitListDistanceFilterOption,
    val visitMapEngineOption: VisitMapEngineOption = VisitMapEngineOption.MapLibre,
    val addVisitsToCalendar: Boolean = false,
    val hasSeenAddVisitToCalendarMessage: Boolean = false,
    /**
     * The [android.provider.CalendarContract.Calendars] row id of the calendar the user chose.
     * Provider-assigned, so it is not stable across devices — it is deliberately dropped on backup
     * restore. Null means "let the app pick"; see
     * [com.msmobile.visitas.util.resolvePreferred].
     */
    val preferredCalendarId: Long? = null
)