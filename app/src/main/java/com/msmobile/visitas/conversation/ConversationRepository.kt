package com.msmobile.visitas.conversation

import java.util.UUID

class ConversationRepository(private val conversationDao: ConversationDao) {

    suspend fun listAll(): List<Conversation> {
        return conversationDao.listAll()
    }

    suspend fun save(conversation: Conversation) {
        conversationDao.save(conversation)
    }

    suspend fun listByIdOrGroupId(id: UUID): List<Conversation> {
        return conversationDao.listByIdOrGroupId(id)
    }

    suspend fun deleteById(id: UUID) {
        conversationDao.deleteById(id)
    }
}