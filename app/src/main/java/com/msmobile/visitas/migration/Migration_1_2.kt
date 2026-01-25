package com.msmobile.visitas.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE householder ADD COLUMN preferredDay TEXT NOT NULL DEFAULT 'ANY'")
        db.execSQL("ALTER TABLE householder ADD COLUMN preferredTime TEXT NOT NULL DEFAULT 'ANY'")
    }
}
