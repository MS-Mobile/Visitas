package com.msmobile.visitas.conversation

import java.util.UUID

class ConversationSnapshotRepository(private val snapshotDao: ConversationSnapshotDao) {
    suspend fun save(snapshot: ConversationSnapshot) = snapshotDao.save(snapshot)

    suspend fun listByGroup(groupId: UUID): List<ConversationSnapshot> =
        snapshotDao.listByGroup(groupId)

    suspend fun deleteByGroup(groupId: UUID) = snapshotDao.deleteByGroup(groupId)
}
