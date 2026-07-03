package com.msmobile.visitas.summary

import androidx.room.Dao
import androidx.room.Query
import java.time.LocalDateTime

@Dao
interface SummaryDao {
    @Query("""
        SELECT COUNT(*) FROM Visit WHERE isDone = 1 AND date >= :startDate AND date < :endDate AND (visitType = 'RETURN_VISIT' OR visitType = 'BIBLE_STUDY')
    """)
    suspend fun getReturnVisitCount(startDate: LocalDateTime, endDate: LocalDateTime): Int

    @Query("""
        SELECT DISTINCT householder.name FROM Visit
        JOIN householder ON Visit.householderId = householder.id
        WHERE Visit.isDone = 1 AND Visit.date >= :startDate AND Visit.date < :endDate AND Visit.visitType = 'BIBLE_STUDY'
        ORDER BY householder.name
    """)
    suspend fun getBibleStudentNames(startDate: LocalDateTime, endDate: LocalDateTime): List<String>
}
