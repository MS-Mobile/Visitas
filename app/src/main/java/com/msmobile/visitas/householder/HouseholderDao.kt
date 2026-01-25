package com.msmobile.visitas.householder

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import java.util.UUID

@Dao
interface HouseholderDao {
    @Upsert
    suspend fun save(householder: Householder)

    @Query("SELECT * FROM Householder WHERE id = :id")
    suspend fun getById(id: UUID): Householder

    @Query("DELETE FROM Householder WHERE id = :id")
    suspend fun deleteById(id: UUID)

    @Query("SELECT * FROM Householder")
    suspend fun listAll(): List<Householder>
}