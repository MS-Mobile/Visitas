package com.msmobile.visitas.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds the chosen-calendar columns to `preference`.
 *
 * The calendar is identified by its account triple rather than its `CalendarContract` row id: the
 * provider reuses ids after a delete, and the same id names an unrelated calendar on another
 * device, so a restored backup would quietly write events into the wrong place.
 *
 * All three are nullable with no default, because null is the meaningful "let the app pick" value —
 * existing installs keep the automatic choice until the user changes it.
 */
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `preference` ADD COLUMN `preferredCalendarAccountType` TEXT")
        db.execSQL("ALTER TABLE `preference` ADD COLUMN `preferredCalendarAccountName` TEXT")
        db.execSQL("ALTER TABLE `preference` ADD COLUMN `preferredCalendarOwnerAccount` TEXT")
    }
}
