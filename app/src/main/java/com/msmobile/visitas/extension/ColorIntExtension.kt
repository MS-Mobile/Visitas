package com.msmobile.visitas.extension

import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorLong

fun @receiver:ColorInt Int.toComposeColor(): Color {
    return Color(toColorLong())
}