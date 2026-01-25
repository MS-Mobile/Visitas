package com.msmobile.visitas.householder

import java.util.UUID

class HouseholderRepository(private val householderDao: HouseholderDao) {
    suspend fun save(householder: Householder) {
        householderDao.save(householder)
    }

    suspend fun getById(id: UUID): Householder {
        return householderDao.getById(id)
    }

    suspend fun deleteById(id: UUID) {
        householderDao.deleteById(id)
    }
}