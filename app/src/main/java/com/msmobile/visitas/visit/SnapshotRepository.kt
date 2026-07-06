package com.msmobile.visitas.visit

import java.util.UUID

class SnapshotRepository(private val snapshotDao: SnapshotDao) {
    suspend fun getHouseholderSnapshot(householderId: UUID): HouseholderSnapshot? {
        return snapshotDao.getHouseholderSnapshot(householderId)
    }

    suspend fun save(householderSnapshot: HouseholderSnapshot, visitSnapshots: List<VisitSnapshot>) {
        snapshotDao.save(householderSnapshot, visitSnapshots)
    }

    suspend fun restore(householderId: UUID): Boolean {
        return snapshotDao.restore(householderId)
    }

    suspend fun delete(householderId: UUID) {
        snapshotDao.delete(householderId)
    }
}
