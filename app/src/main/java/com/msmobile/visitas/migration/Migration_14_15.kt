package com.msmobile.visitas.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds `preferredCalendarId` to `preference` — the calendar the user picked for visit events.
 *
 * Nullable with no default on purpose: null is the meaningful "let the app pick" value, so existing
 * installs keep the automatic choice they have been getting until the user changes it.
 */
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `preference` ADD COLUMN `preferredCalendarId` INTEGER")
    }
}
