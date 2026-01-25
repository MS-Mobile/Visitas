package com.msmobile.visitas.fieldservice

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import java.util.UUID

@Dao
interface FieldServiceDao {
    @Query("SELECT * FROM field_service WHERE endTime IS NULL LIMIT 1")
    suspend fun getInProgress(): FieldService?

    @Update
    suspend fun update(fieldService: FieldService)

    @Query("DELETE FROM field_service WHERE id = :id")
    suspend fun delete(id: UUID)

    @Query("INSERT INTO field_service (startTime) VALUES (CURRENT_TIMESTAMP)")
    suspend fun addFieldServiceAtCurrentTime()

    @Query("UPDATE field_service SET endTime = CURRENT_TIMESTAMP WHERE id = :id")
    suspend fun updateFieldServiceEndTime(id: UUID)

    @Query("SELECT * FROM field_service WHERE id = :id")
    suspend fun getById(id: UUID): FieldService?

    @Upsert
    suspend fun save(fieldService: FieldService)

    @Query("SELECT * FROM field_service")
    suspend fun listAll(): List<FieldService>
}