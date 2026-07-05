package com.msmobile.visitas.visit

import java.util.UUID

class DraftSnapshotRepository(private val draftSnapshotDao: DraftSnapshotDao) {
    suspend fun getHouseholderSnapshot(householderId: UUID): HouseholderSnapshot? {
        return draftSnapshotDao.getHouseholderSnapshot(householderId)
    }

    suspend fun save(householderSnapshot: HouseholderSnapshot, visitSnapshots: List<VisitSnapshot>) {
        draftSnapshotDao.save(householderSnapshot, visitSnapshots)
    }

    suspend fun restore(householderId: UUID): Boolean {
        return draftSnapshotDao.restore(householderId)
    }

    suspend fun delete(householderId: UUID) {
        draftSnapshotDao.delete(householderId)
    }
}
