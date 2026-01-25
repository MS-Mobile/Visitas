package com.msmobile.visitas.util

import android.net.Uri

sealed class IntentState {
    data object None : IntentState()
    data class PreviewBackupFile(val uri: Uri) : IntentState()
}