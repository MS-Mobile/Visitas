package com.msmobile.visitas.util

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

fun ContentResolver.isValidBackupUri(uri: Uri): Boolean {
    val scheme = uri.scheme
    if (scheme != "file" && scheme != "content") return false
    return hasVisitasExtension(this, uri)
}

private fun hasVisitasExtension(contentResolver: ContentResolver, uri: Uri): Boolean {
    // First try to get the display name from ContentResolver (works for content:// URIs)
    if (uri.scheme == "content") {
        val displayName = getDisplayName(contentResolver, uri)
        if (displayName?.endsWith(BackupHandler.BACKUP_FILE_EXTENSION) == true) return true
    }
    // Fallback to checking the URI path directly (works for file:// URIs)
    val path = uri.path
    val fileName = uri.lastPathSegment
    return path?.endsWith(BackupHandler.BACKUP_FILE_EXTENSION) == true
            || fileName?.endsWith(BackupHandler.BACKUP_FILE_EXTENSION) == true
}

private fun getDisplayName(contentResolver : ContentResolver, uri: Uri): String? {
    return try {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else null
            }
    } catch (e: Exception) {
        null
    }
}