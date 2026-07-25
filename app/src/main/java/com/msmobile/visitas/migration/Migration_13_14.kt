package com.msmobile.visitas.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds the calendar columns to `preference`. `addVisitsToCalendar` gates whether saving a visit
 * also writes it to the device calendar, and `hasSeenAddVisitToCalendarMessage` records that the
 * one-time discovery message on the visit detail screen has been acknowledged.
 *
 * Both default to 0: calendar sync is opt-in, and existing installs should still be offered the
 * discovery message.
 */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `preference` ADD COLUMN `addVisitsToCalendar` INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "ALTER TABLE `preference` ADD COLUMN `hasSeenAddVisitToCalendarMessage` INTEGER NOT NULL DEFAULT 0"
        )
    }
}
