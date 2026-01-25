package com.msmobile.visitas.fieldservice

import java.util.UUID

class FieldServiceRepository(private val fieldServiceDao: FieldServiceDao) {
    suspend fun getInProgress(): FieldService? {
        return fieldServiceDao.getInProgress()
    }

    suspend fun update(fieldService: FieldService) {
        fieldServiceDao.update(fieldService)
    }

    suspend fun delete(id: UUID) {
        fieldServiceDao.delete(id)
    }

    suspend fun addFieldServiceAtCurrentTime() {
        fieldServiceDao.addFieldServiceAtCurrentTime()
    }

    suspend fun updateFieldServiceEndTime(id: UUID) {
        fieldServiceDao.updateFieldServiceEndTime(id)
    }
}