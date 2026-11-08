package com.msmobile.visitas.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

class DateTimeProvider {
    fun nowDate(): Date {
        return Date()
    }

    fun nowLocalDateTime(): LocalDateTime {
        return LocalDateTime.now()
    }

    fun nowLocalDate(): LocalDate {
        return LocalDate.now()
    }

    fun zoneId(): ZoneId {
        return ZoneId.systemDefault()
    }

    fun nanoTime(): Long {
        return System.nanoTime()
    }
}