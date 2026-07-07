package com.msmobile.visitas.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Recreates the `visit_householder` view so its draft signal reflects the whole householder
 * aggregate instead of only the latest visit. The old `isDraft` column exposed just the latest
 * visit's flag; the new `hasDrafts` column is true when the householder row is a draft OR any of
 * its visits is a draft, matching the aggregate used by the detail screen.
 *
 * Room does not recreate views automatically during a migration, and it compares the view SQL
 * character-by-character with the @DatabaseView annotation, so this must match VisitHouseholder.kt
 * exactly. We use trimMargin() to keep the code readable while preserving that format.
 */
val MIGRATION_11_12 = object : Migration(11, 12) {
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
            |        h.addressLongitude as householderLongitude
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
