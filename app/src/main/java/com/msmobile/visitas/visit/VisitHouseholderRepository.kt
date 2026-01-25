package com.msmobile.visitas.visit

class VisitHouseholderRepository(private val visitHouseholderDao: VisitHouseholderDao) {
    suspend fun getAll(): List<VisitHouseholder> {
        return visitHouseholderDao.getAll()
    }
}