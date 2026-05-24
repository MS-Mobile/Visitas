package com.msmobile.visitas.util

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import com.msmobile.visitas.ScaffoldState

val LocalTopBarActions = compositionLocalOf { mutableStateOf<@Composable RowScope.() -> Unit>({}) }
val LocalAppScaffoldState = compositionLocalOf { mutableStateOf(ScaffoldState()) }
