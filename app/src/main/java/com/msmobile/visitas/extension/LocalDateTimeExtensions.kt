package com.msmobile.visitas.extension

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val DATE_FORMAT = "dd/MM/yyyy HH:mm"

fun LocalDateTime.toString(locale: Locale): String {
    return DateTimeFormatter.ofPattern(DATE_FORMAT, locale).format(this)
}

fun String.fromString(locale: Locale): LocalDateTime {
    return DateTimeFormatter.ofPattern(DATE_FORMAT, locale).parse(this).run {
        LocalDateTime.from(this)
    }
}