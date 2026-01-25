package com.msmobile.visitas.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // SQLite doesn't support dropping columns directly
        // We need to recreate the table without the isResponsive column

        // 1. Create new householder table without isResponsive column
        db.execSQL("""
            CREATE TABLE householder_new (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                address TEXT NOT NULL,
                notes TEXT,
                addressLatitude REAL,
                addressLongitude REAL,
                preferredDay TEXT NOT NULL,
                preferredTime TEXT NOT NULL
            )
        """.trimIndent())

        // 2. Copy data from old table to new table (excluding isResponsive)
        db.execSQL("""
            INSERT INTO householder_new (id, name, address, notes, addressLatitude, addressLongitude, preferredDay, preferredTime)
            SELECT id, name, address, notes, addressLatitude, addressLongitude, preferredDay, preferredTime
            FROM householder
        """.trimIndent())

        // 3. Drop old table
        db.execSQL("DROP TABLE householder")

        // 4. Rename new table to original name
        db.execSQL("ALTER TABLE householder_new RENAME TO householder")

        // 5. Recreate the visit_householder view without isHouseholderResponsive
        // NOTE: Room compares view SQL character-by-character with the @DatabaseView annotation.
        // We use trimMargin() to keep code readable while matching VisitHouseholder.kt's format.
        db.execSQL("DROP VIEW IF EXISTS visit_householder")
        db.execSQL(
            """
            |CREATE VIEW `visit_householder` AS SELECT
            |        v.id as visitId,
            |        v.subject as subject,
            |        v.date as date,
            |        v.isDone as isDone,
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

