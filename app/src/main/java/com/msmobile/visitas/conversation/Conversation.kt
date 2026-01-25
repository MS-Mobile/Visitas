package com.msmobile.visitas.conversation

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "conversation")
data class Conversation(
    @PrimaryKey val id: UUID,
    val question: String,
    val response: String,
    val orderIndex: Int,
    val conversationGroupId: UUID?
)