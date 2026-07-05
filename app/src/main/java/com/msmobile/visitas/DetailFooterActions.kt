package com.msmobile.visitas

data class DetailFooterActions(
    val onSave: () -> Unit,
    val onAdd: () -> Unit,
    // Discards the open draft, restoring the last-committed state. Null hides the button
    // (no discardable draft, or the screen doesn't support drafts).
    val onDiscard: (() -> Unit)? = null,
)
