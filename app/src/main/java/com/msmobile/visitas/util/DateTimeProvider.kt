package com.msmobile.visitas.util

import java.time.LocalDateTime
import java.util.Date

class DateTimeProvider {
    fun nowDate(): Date {
        return Date()
    }

    fun nowLocalDateTime(): LocalDateTime {
        return LocalDateTime.now()
    }
}