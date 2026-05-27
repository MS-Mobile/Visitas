package com.msmobile.visitas

data class DetailFooterActions(
    val onBack: () -> Unit,
    val onSave: () -> Unit,
    val onAdd: () -> Unit,
)
