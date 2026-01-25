package com.msmobile.visitas.visit

import java.util.UUID

class VisitRepository(private val visitDao: VisitDao) {
    suspend fun getAll(householderId: UUID): List<Visit> {
        return visitDao.getByHouseholderId(householderId)
    }

    suspend fun save(visit: Visit) {
        visitDao.save(visit)
    }

    suspend fun getById(id: UUID): Visit {
        return visitDao.getById(id)
    }

    suspend fun deleteBulk(idList: List<UUID>) {
        visitDao.deleteBulk(idList)
    }
}