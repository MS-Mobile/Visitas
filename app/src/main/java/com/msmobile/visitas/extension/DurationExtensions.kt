package com.msmobile.visitas.extension

import java.util.Locale
import kotlin.time.Duration

fun Duration.toClockString(locale: Locale = Locale.getDefault()): String {
    return toComponents { hours, minutes, seconds, _ ->
        String.format(
            locale,
            "%02d:%02d:%02d",
            hours,
            minutes,
            seconds,
        )
    }
}