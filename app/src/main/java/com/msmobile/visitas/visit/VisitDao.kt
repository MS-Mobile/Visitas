package com.msmobile.visitas.visit

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import java.util.UUID

@Dao
interface VisitDao {
    @Query("SELECT * FROM visit WHERE householderId = :householderId ORDER BY date DESC")
    suspend fun getByHouseholderId(householderId: UUID): List<Visit>

    @Upsert
    suspend fun save(visit: Visit)

    @Query("SELECT * FROM visit WHERE id = :id")
    suspend fun getById(id: UUID): Visit

    @Query("DELETE FROM visit WHERE id IN (:idList)")
    suspend fun deleteBulk(idList: List<UUID>)

    @Query("SELECT * FROM visit")
    suspend fun listAll(): List<Visit>
}