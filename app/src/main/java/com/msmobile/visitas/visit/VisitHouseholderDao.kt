package com.msmobile.visitas.visit
import androidx.room.Dao
import androidx.room.Query

@Dao
interface VisitHouseholderDao {
    @Query("SELECT * FROM visit_householder ORDER BY date ASC")
    suspend fun getAll(): List<VisitHouseholder>
}