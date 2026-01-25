package com.msmobile.visitas.summary

import androidx.room.Dao
import androidx.room.Query
import java.time.LocalDateTime

@Dao
interface SummaryDao {
    @Query("""
        SELECT 
            (SELECT COUNT(*) FROM Visit WHERE isDone = 1 AND date >= :startDate AND date < :endDate AND (visitType = 'RETURN_VISIT' OR visitType = 'BIBLE_STUDY')) AS returnVisitCount,
            (SELECT COUNT(DISTINCT householderId) FROM Visit WHERE isDone = 1 AND date >= :startDate AND date < :endDate AND visitType = 'BIBLE_STUDY') AS bibleStudyCount,
            (SELECT COALESCE(SUM(duration), 0) FROM field_service WHERE startTime >= :startDate AND endTime < :endDate) AS totalFieldServiceSeconds,
            (SELECT COALESCE(SUM(CASE WHEN endTime IS NULL THEN strftime('%s', 'now') - strftime('%s', startTime) ELSE duration END), 0) 
             FROM field_service 
             WHERE startTime >= :startDate AND (endTime IS NULL OR endTime <= :endDate)) AS fieldServiceInProgressSeconds
    """)
    suspend fun getSummary(startDate: LocalDateTime, endDate: LocalDateTime): SummaryResult
}