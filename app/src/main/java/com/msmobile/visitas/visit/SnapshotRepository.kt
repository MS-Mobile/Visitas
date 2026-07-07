package com.msmobile.visitas.visit

import com.msmobile.visitas.householder.HouseholderSnapshot
import java.util.UUID

class SnapshotRepository(private val snapshotDao: SnapshotDao) {
    suspend fun getHouseholderSnapshot(householderId: UUID): HouseholderSnapshot? {
        return snapshotDao.getHouseholderSnapshot(householderId)
    }

    suspend fun getVisitSnapshots(householderId: UUID): List<VisitSnapshot> {
        return snapshotDao.getVisitSnapshots(householderId)
    }

    suspend fun saveHouseholderSnapshot(householderSnapshot: HouseholderSnapshot) {
        snapshotDao.saveHouseholderSnapshot(householderSnapshot)
    }

    suspend fun saveVisitSnapshot(visitSnapshot: VisitSnapshot) {
        snapshotDao.saveVisitSnapshot(visitSnapshot)
    }

    suspend fun deleteHouseholderSnapshot(householderId: UUID) {
        snapshotDao.deleteHouseholderSnapshot(householderId)
    }

    suspend fun deleteVisitSnapshots(householderId: UUID) {
        snapshotDao.deleteVisitSnapshots(householderId)
    }
}
