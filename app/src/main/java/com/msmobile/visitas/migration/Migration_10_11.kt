package com.msmobile.visitas.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds the `isDraft` column to `householder` and its snapshot mirror `householder_snapshot`.
 * `isDraft` marks a householder row as uncommitted (a persisted draft), symmetric with the
 * column already present on `visit` / `visit_snapshot`. Existing rows are committed data, so
 * they default to 0 (false).
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `householder` ADD COLUMN `isDraft` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `householder_snapshot` ADD COLUMN `isDraft` INTEGER NOT NULL DEFAULT 0")
    }
}
