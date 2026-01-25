package com.msmobile.visitas.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // SQLite doesn't support adding foreign keys to existing tables
        // We need to recreate the table with the foreign key constraint
        
        // 1. Create new table with foreign key
        db.execSQL("""
            CREATE TABLE visit_new (
                id TEXT NOT NULL PRIMARY KEY,
                subject TEXT NOT NULL,
                date TEXT NOT NULL,
                isDone INTEGER NOT NULL,
                householderId TEXT NOT NULL,
                orderIndex INTEGER NOT NULL,
                visitType TEXT NOT NULL,
                nextConversationId TEXT,
                calendarEventId INTEGER DEFAULT NULL,
                FOREIGN KEY (householderId) REFERENCES householder(id) ON DELETE CASCADE
            )
        """.trimIndent())
        
        // 2. Create index on householderId
        db.execSQL("CREATE INDEX index_visit_householderId ON visit_new(householderId)")
        
        // 3. Copy data from old table to new table (only visits with valid householder)
        db.execSQL("""
            INSERT INTO visit_new (id, subject, date, isDone, householderId, orderIndex, visitType, nextConversationId, calendarEventId)
            SELECT v.id, v.subject, v.date, v.isDone, v.householderId, v.orderIndex, v.visitType, v.nextConversationId, v.calendarEventId
            FROM visit v
            INNER JOIN householder h ON v.householderId = h.id
        """.trimIndent())
        
        // 4. Drop old table
        db.execSQL("DROP TABLE visit")
        
        // 5. Rename new table to original name
        db.execSQL("ALTER TABLE visit_new RENAME TO visit")
    }
}
