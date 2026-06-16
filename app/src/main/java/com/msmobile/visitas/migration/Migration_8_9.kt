package com.msmobile.visitas.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE visit ADD COLUMN isDraft INTEGER NOT NULL DEFAULT 0"
        )

        // The visit_householder view references the new isDraft column, so it must be
        // recreated to match the @DatabaseView definition in VisitHouseholder.kt.
        // Room compares view SQL character-by-character during schema validation, so
        // this must stay in sync with that annotation (see Migration_4_5 for the same pattern).
        db.execSQL("DROP VIEW IF EXISTS visit_householder")
        db.execSQL(
            """
            |CREATE VIEW `visit_householder` AS SELECT
            |        v.id as visitId,
            |        v.subject as subject,
            |        v.date as date,
            |        v.isDone as isDone,
            |        v.isDraft as isDraft,
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
