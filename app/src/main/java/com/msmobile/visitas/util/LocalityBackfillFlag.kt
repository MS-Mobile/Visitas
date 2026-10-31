package com.msmobile.visitas.util

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Remembers that the householder locality backfill finished a run with no failed lookup. Kept
 * outside the database on purpose: the backfill needs no schema change.
 */
class LocalityBackfillFlag(private val preferences: SharedPreferences) {
    fun isDone(): Boolean = preferences.getBoolean(KEY_DONE, false)

    fun markDone() = preferences.edit { putBoolean(KEY_DONE, true) }

    fun clear() = preferences.edit { remove(KEY_DONE) }

    private companion object {
        private const val KEY_DONE = "householder_locality_backfill_done"
    }
}
