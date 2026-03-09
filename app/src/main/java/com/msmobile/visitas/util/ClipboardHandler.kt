package com.msmobile.visitas.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

class ClipboardHandler(private val context: Context) {
    fun copyToClipboard(text: String) {
        val clipboardManager = context.getSystemService(ClipboardManager::class.java)
        val clip = ClipData.newPlainText(null, text)
        clipboardManager.setPrimaryClip(clip)
    }
}

