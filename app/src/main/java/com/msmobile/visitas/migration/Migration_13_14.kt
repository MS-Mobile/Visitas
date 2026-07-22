package com.msmobile.visitas.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds the nullable `calendarColorKey` column to `visit` and its snapshot mirror
 * `visit_snapshot`. It stores the calendar event color the user picked for the
 * visit (a key into the account's synced palette, see
 * [android.provider.CalendarContract.Colors]). Existing visits have no explicit
 * choice, so the column is nullable and null falls back to the app default.
 */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `visit` ADD COLUMN `calendarColorKey` TEXT")
        db.execSQL("ALTER TABLE `visit_snapshot` ADD COLUMN `calendarColorKey` TEXT")
    }
}
