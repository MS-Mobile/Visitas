package com.msmobile.visitas.conversation

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import java.util.UUID

@Dao
interface ConversationSnapshotDao {
    @Upsert
    suspend fun save(snapshot: ConversationSnapshot)

    @Query("SELECT * FROM conversation_snapshot WHERE id = :groupId OR conversationGroupId = :groupId")
    suspend fun listByGroup(groupId: UUID): List<ConversationSnapshot>

    @Query("DELETE FROM conversation_snapshot WHERE id = :groupId OR conversationGroupId = :groupId")
    suspend fun deleteByGroup(groupId: UUID)

    @Query("SELECT * FROM conversation_snapshot")
    suspend fun listAll(): List<ConversationSnapshot>
}
