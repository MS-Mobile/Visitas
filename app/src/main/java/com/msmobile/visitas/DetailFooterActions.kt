package com.msmobile.visitas

data class DetailFooterActions(
    val onSave: () -> Unit,
    val onAdd: () -> Unit,
    val onDiscard: () -> Unit,
)
