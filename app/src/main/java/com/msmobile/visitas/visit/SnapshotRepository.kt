package com.msmobile.visitas.visit

import com.msmobile.visitas.householder.HouseholderSnapshot
import java.util.UUID

class SnapshotRepository(private val snapshotDao: SnapshotDao) {
    suspend fun getHouseholderSnapshot(householderId: UUID): HouseholderSnapshot? {
        return snapshotDao.getHouseholderSnapshot(householderId)
    }

    suspend fun saveHouseholderSnapshot(householderSnapshot: HouseholderSnapshot) {
        snapshotDao.saveHouseholderSnapshot(householderSnapshot)
    }

    suspend fun saveVisitSnapshot(visitSnapshot: VisitSnapshot) {
        snapshotDao.saveVisitSnapshot(visitSnapshot)
    }
}
