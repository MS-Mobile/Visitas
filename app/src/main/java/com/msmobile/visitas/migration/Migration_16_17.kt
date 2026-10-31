package com.msmobile.visitas.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds the nullable `neighborhood` (Brazilian "bairro") and `city` columns to `householder` and
 * its snapshot mirror `householder_snapshot`. Both are filled from the geocoder when the visit
 * detail screen resolves an address. Existing rows have neither, so the columns are nullable with
 * no default.
 */
val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `householder` ADD COLUMN `neighborhood` TEXT")
        db.execSQL("ALTER TABLE `householder` ADD COLUMN `city` TEXT")
        db.execSQL("ALTER TABLE `householder_snapshot` ADD COLUMN `neighborhood` TEXT")
        db.execSQL("ALTER TABLE `householder_snapshot` ADD COLUMN `city` TEXT")
    }
}
