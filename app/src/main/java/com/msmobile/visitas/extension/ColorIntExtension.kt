package com.msmobile.visitas.extension

import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color

/**
 * Converts an ARGB [ColorInt] to a Compose [Color].
 *
 * Uses Compose's `Color(Int)` constructor, which reads the int as ARGB directly. Routing through
 * `toColorLong()` first was wrong: that produces an `android.graphics` color-long (color space id
 * in the low bits), and Compose's `Color(Long)` keeps the low 32 bits, yielding a transparent
 * color — which is why the calendar color square rendered invisibly.
 */
fun @receiver:ColorInt Int.toComposeColor(): Color {
    return Color(this)
}
