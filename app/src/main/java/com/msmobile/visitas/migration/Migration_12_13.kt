package com.msmobile.visitas.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds the nullable `phoneNumber` column to `householder` and its snapshot mirror
 * `householder_snapshot`. This lets the visit detail screen store a contact number
 * for the householder and reach them via call / SMS / WhatsApp. Existing rows have
 * no number, so the column is nullable with no default.
 */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `householder` ADD COLUMN `phoneNumber` TEXT")
        db.execSQL("ALTER TABLE `householder_snapshot` ADD COLUMN `phoneNumber` TEXT")
    }
}
