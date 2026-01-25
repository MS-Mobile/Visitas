package com.msmobile.visitas.extension

import java.util.Locale
import kotlin.time.Duration

fun Duration.toClockString(): String {
    return toComponents { hours, minutes, seconds, _ ->
        String.format(
            Locale.getDefault(),
            "%02d:%02d:%02d",
            hours,
            minutes,
            seconds,
        )
    }
}