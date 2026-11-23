package com.msmobile.visitas.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds conversation drafts: the `isDraft` flag on `conversation` and the `conversation_snapshot`
 * table that holds the last-committed copy of each dirtied row. The CREATE statement must match
 * the schema Room generates for [com.msmobile.visitas.conversation.ConversationSnapshot] exactly,
 * including the embedded `isDraft` column, or migration validation fails.
 */
val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `conversation` ADD COLUMN `isDraft` INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            """
            |CREATE TABLE IF NOT EXISTS `conversation_snapshot` (
            |    `id` TEXT NOT NULL,
            |    `question` TEXT NOT NULL,
            |    `response` TEXT NOT NULL,
            |    `orderIndex` INTEGER NOT NULL,
            |    `conversationGroupId` TEXT,
            |    `isDraft` INTEGER NOT NULL,
            |    PRIMARY KEY(`id`),
            |    FOREIGN KEY(`id`) REFERENCES `conversation`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            |)
            """.trimMargin()
        )
    }
}
