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

    suspend fun getByIdOrNull(id: UUID): Conversation? = conversationDao.getByIdOrNull(id)

    suspend fun deleteBulk(idList: List<UUID>) = conversationDao.deleteBulk(idList)

    suspend fun deleteGroup(groupId: UUID) = conversationDao.deleteGroup(groupId)

    suspend fun listForSuggestions(): List<Conversation> = conversationDao.listForSuggestions()
}