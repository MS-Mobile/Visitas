package com.msmobile.visitas.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Snapshot tables holding the last-committed copy of a householder and its visits while
        // a draft edit session is open. They embed the live entities, so their columns must stay
        // in sync with the householder/visit tables. The SQL must match Room's generated schema
        // (app/schemas/.../10.json) exactly.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `householder_snapshot` (" +
                "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `address` TEXT NOT NULL, " +
                "`notes` TEXT, `addressLatitude` REAL, `addressLongitude` REAL, " +
                "`preferredDay` TEXT NOT NULL, `preferredTime` TEXT NOT NULL, " +
                "`isNewDraft` INTEGER NOT NULL, `createdAt` TEXT NOT NULL, " +
                "PRIMARY KEY(`id`), " +
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
