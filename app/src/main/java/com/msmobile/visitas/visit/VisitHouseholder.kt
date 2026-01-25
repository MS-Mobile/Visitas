package com.msmobile.visitas.visit

import androidx.room.DatabaseView
import java.time.LocalDateTime
import java.util.UUID

@DatabaseView(
    """
    SELECT
        v.id as visitId,
        v.subject as subject,
        v.date as date,
        v.isDone as isDone,
        v.householderId as householderId,
        v.visitType as type,
        h.name as householderName,
        h.address as householderAddress,
        h.addressLatitude as householderLatitude,
        h.addressLongitude as householderLongitude
    FROM visit v
    JOIN householder h ON v.householderId = h.id
    INNER JOIN (
        SELECT householderId, MAX(date) as max_date
        FROM visit
        GROUP BY householderId
    ) latest ON v.householderId = latest.householderId 
        AND v.date = latest.max_date
    ORDER BY v.householderId, v.date DESC
    """,
    viewName = "visit_householder"
)
data class VisitHouseholder(
    val visitId: UUID,
    val subject: String,
    val date: LocalDateTime,
    val isDone: Boolean,
    val householderId: UUID,
    val householderName: String,
    val householderAddress: String,
    val type: VisitType,
    val householderLatitude: Double?,
    val householderLongitude: Double?,
)