package com.msmobile.visitas.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds the snapshot tables that back the "discard draft" feature. Each table mirrors its live
 * counterpart so a pre-edit copy can be restored when the user discards changes. The CREATE
 * statements must match the schema Room generates for [com.msmobile.visitas.householder.HouseholderSnapshot]
 * and [com.msmobile.visitas.visit.VisitSnapshot] exactly, or the migration validation fails.
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `householder_snapshot` (" +
                "`id` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, `address` TEXT NOT NULL, `notes` TEXT, " +
                "`addressLatitude` REAL, `addressLongitude` REAL, `preferredDay` TEXT NOT NULL, " +
                "`preferredTime` TEXT NOT NULL, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`id`) REFERENCES `householder`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `visit_snapshot` (" +
                "`id` TEXT NOT NULL, `subject` TEXT NOT NULL, `date` TEXT NOT NULL, " +
                "`isDone` INTEGER NOT NULL, `householderId` TEXT NOT NULL, " +
                "`orderIndex` INTEGER NOT NULL, `visitType` TEXT NOT NULL, " +
                "`nextConversationId` TEXT, `calendarEventId` INTEGER, `isDraft` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`householderId`) REFERENCES `householder`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_visit_snapshot_householderId` " +
                "ON `visit_snapshot` (`householderId`)"
        )
    }
}
