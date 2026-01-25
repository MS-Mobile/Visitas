package com.msmobile.visitas.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // SQLite doesn't support dropping columns directly
        // We need to recreate the table without the visitListAttendanceFilterOption column

        // 1. Create new preference table without visitListAttendanceFilterOption column
        db.execSQL("""
            CREATE TABLE preference_new (
                id TEXT NOT NULL PRIMARY KEY,
                visitListDateFilterOption TEXT NOT NULL,
                visitListDistanceFilterOption TEXT NOT NULL
            )
        """.trimIndent())

        // 2. Copy data from old table to new table
        db.execSQL("""
            INSERT INTO preference_new (id, visitListDateFilterOption, visitListDistanceFilterOption)
            SELECT id, visitListDateFilterOption, visitListDistanceFilterOption FROM preference
        """.trimIndent())

        // 3. Drop the old table
        db.execSQL("DROP TABLE preference")

        // 4. Rename new table to original name
        db.execSQL("ALTER TABLE preference_new RENAME TO preference")
    }
}

