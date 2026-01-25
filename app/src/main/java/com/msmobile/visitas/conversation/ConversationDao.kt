package com.msmobile.visitas.conversation

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import java.util.UUID

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversation WHERE conversationGroupId IS NULL ORDER BY question ASC")
    suspend fun listParents(): List<Conversation>

    @Query("SELECT * FROM conversation ORDER BY question ASC")
    suspend fun listAll(): List<Conversation>

    @Upsert
    suspend fun save(conversation: Conversation)

    @Query("SELECT * FROM conversation WHERE id = :id OR conversationGroupId = :id ORDER BY orderIndex ASC")
    suspend fun listByIdOrGroupId(id: UUID): List<Conversation>

    @Query("DELETE FROM conversation WHERE id = :id")
    suspend fun deleteById(id: UUID)

    @Query("SELECT * FROM conversation WHERE id = :id")
    suspend fun getById(id: UUID): Conversation
}
