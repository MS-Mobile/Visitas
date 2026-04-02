package com.msmobile.visitas.summary

import androidx.room.Dao
import androidx.room.Query
import java.time.LocalDateTime

@Dao
interface SummaryDao {
    @Query("""
        SELECT 
            (SELECT COUNT(*) FROM Visit WHERE isDone = 1 AND date >= :startDate AND date < :endDate AND (visitType = 'RETURN_VISIT' OR visitType = 'BIBLE_STUDY')) AS returnVisitCount,
            (SELECT COUNT(DISTINCT householderId) FROM Visit WHERE isDone = 1 AND date >= :startDate AND date < :endDate AND visitType = 'BIBLE_STUDY') AS bibleStudyCount
    """)
    suspend fun getSummary(startDate: LocalDateTime, endDate: LocalDateTime): SummaryResult
}