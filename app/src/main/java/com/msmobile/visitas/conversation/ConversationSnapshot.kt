package com.msmobile.visitas.conversation

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey

/**
 * Last-committed copy of a conversation row, taken the first time an auto-save dirties it.
 * Undo restores from it and Save deletes it. The foreign key on the row's own id makes every
 * delete of the live row (trash, removed-row cleanup, undo of added rows) clean the snapshot up.
 * The live row must only ever be written with @Upsert: a REPLACE insert deletes first and would
 * cascade-wipe the snapshot.
 * The primary key is declared at table level because @PrimaryKey inside an @Embedded class is not
 * honored by Room.
 */
@Entity(
    tableName = "conversation_snapshot",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(
            entity = Conversation::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ConversationSnapshot(
    @Embedded val conversation: Conversation
)
