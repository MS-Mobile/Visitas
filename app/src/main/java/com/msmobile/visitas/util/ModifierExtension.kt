package com.msmobile.visitas.util

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier

fun Modifier.snackbarPadding(): Modifier {
    return navigationBarsPadding()
        .imePadding()
        .padding(borderPadding)
        .padding(bottom = verticalFieldPadding)
        .padding(bottom = verticalFieldPadding + floatingBarBottomPadding)
}