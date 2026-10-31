package com.msmobile.visitas.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Recreates the `visit_householder` view to expose the householder's `neighborhood` and `city`
 * (added in [MIGRATION_16_17]) so the visit list search can match them.
 *
 * Room does not recreate views automatically during a migration, and it compares the view SQL
 * character-by-character with the @DatabaseView annotation, so this must match VisitHouseholder.kt
 * exactly. We use trimMargin() to keep the code readable while preserving that format.
 */
val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP VIEW IF EXISTS visit_householder")
        db.execSQL(
            """
            |CREATE VIEW `visit_householder` AS SELECT
            |        v.id as visitId,
            |        v.subject as subject,
            |        v.date as date,
            |        v.isDone as isDone,
            |        (h.isDraft OR EXISTS(SELECT 1 FROM visit vd WHERE vd.householderId = v.householderId AND vd.isDraft)) as hasDrafts,
            |        v.householderId as householderId,
            |        v.visitType as type,
            |        h.name as householderName,
            |        h.address as householderAddress,
            |        h.addressLatitude as householderLatitude,
            |        h.addressLongitude as householderLongitude,
            |        h.neighborhood as householderNeighborhood,
            |        h.city as householderCity
            |    FROM visit v
            |    JOIN householder h ON v.householderId = h.id
            |    INNER JOIN (
            |        SELECT householderId, MAX(date) as max_date
            |        FROM visit
            |        GROUP BY householderId
            |    ) latest ON v.householderId = latest.householderId
            |        AND v.date = latest.max_date
            |    ORDER BY v.householderId, v.date DESC
            |    """.trimMargin()
        )
    }
}
