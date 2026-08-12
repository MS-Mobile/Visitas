package com.msmobile.visitas.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds `preferredCalendarId` to `preference` — the `CalendarContract.Calendars` row id of the
 * calendar the user picked for visit events. The id is assigned by the device's calendar provider,
 * so it is meaningful only on the device that stored it.
 *
 * Nullable with no default on purpose: null is the meaningful "let the app pick" value, so existing
 * installs keep the automatic choice they have been getting until the user changes it.
 */
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `preference` ADD COLUMN `preferredCalendarId` INTEGER")
    }
}
