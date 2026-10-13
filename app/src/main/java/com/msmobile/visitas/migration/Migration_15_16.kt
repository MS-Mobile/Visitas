package com.msmobile.visitas.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds the calendar event durations to `preference`, in milliseconds: how long the event written
 * for a return visit and for a Bible study lasts.
 *
 * The defaults (30 and 60 minutes) live in the table so existing installs pick them up without a
 * write. They must match the `@ColumnInfo(defaultValue)` on
 * [com.msmobile.visitas.preference.Preference], or Room's schema validation fails on open.
 */
val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `preference` ADD COLUMN `returnVisitDurationMillis` INTEGER NOT NULL DEFAULT 1800000"
        )
        db.execSQL(
            "ALTER TABLE `preference` ADD COLUMN `bibleStudyDurationMillis` INTEGER NOT NULL DEFAULT 3600000"
        )
    }
}
