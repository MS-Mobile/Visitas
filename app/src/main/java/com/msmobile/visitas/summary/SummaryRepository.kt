package com.msmobile.visitas.summary

import java.time.LocalDateTime

class SummaryRepository(private val summaryDao: SummaryDao) {
    suspend fun getSummary(start: LocalDateTime, end: LocalDateTime): SummaryResult {
        return SummaryResult(
            returnVisitCount = summaryDao.getReturnVisitCount(start, end),
            bibleStudentNames = summaryDao.getBibleStudentNames(start, end)
        )
    }
}
