package com.msmobile.visitas.summary

data class SummaryResult(
    val returnVisitCount: Int,
    val bibleStudyCount: Int,
    val totalFieldServiceSeconds: Int,
    val fieldServiceInProgressSeconds: Int
)
