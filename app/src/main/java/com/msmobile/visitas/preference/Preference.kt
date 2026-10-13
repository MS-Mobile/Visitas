package com.msmobile.visitas.preference

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.msmobile.visitas.util.CalendarIdentity
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
    // The chosen calendar is stored as its account identity rather than its CalendarContract row
    // id, which the provider reuses and which means something else on another device. Read and
    // write these through preferredCalendar / withPreferredCalendar rather than directly.
    val preferredCalendarAccountType: String? = null,
    val preferredCalendarAccountName: String? = null,
    val preferredCalendarOwnerAccount: String? = null,
    // How long the calendar event written for each visit type lasts.
    @ColumnInfo(defaultValue = "1800000")
    val returnVisitDurationMillis: Long = DEFAULT_RETURN_VISIT_DURATION_MILLIS,
    @ColumnInfo(defaultValue = "3600000")
    val bibleStudyDurationMillis: Long = DEFAULT_BIBLE_STUDY_DURATION_MILLIS
)

/** 30 minutes. Mirrored by the `@ColumnInfo` default above and in `MIGRATION_15_16`. */
const val DEFAULT_RETURN_VISIT_DURATION_MILLIS = 1_800_000L

/** 60 minutes. Mirrored by the `@ColumnInfo` default above and in `MIGRATION_15_16`. */
const val DEFAULT_BIBLE_STUDY_DURATION_MILLIS = 3_600_000L

/** The calendar the user chose, or null to let the app pick automatically. */
val Preference.preferredCalendar: CalendarIdentity?
    get() {
        val accountType = preferredCalendarAccountType
        val accountName = preferredCalendarAccountName
        val ownerAccount = preferredCalendarOwnerAccount
        if (accountType == null || accountName == null || ownerAccount == null) return null
        return CalendarIdentity(accountType, accountName, ownerAccount)
    }

/** A copy with [calendar] as the chosen calendar; null restores the automatic pick. */
fun Preference.withPreferredCalendar(calendar: CalendarIdentity?): Preference = copy(
    preferredCalendarAccountType = calendar?.accountType,
    preferredCalendarAccountName = calendar?.accountName,
    preferredCalendarOwnerAccount = calendar?.ownerAccount
)