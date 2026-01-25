package com.msmobile.visitas.preference

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface PreferenceDao {
    @Query("SELECT * FROM preference LIMIT 1")
    suspend fun getPreference(): Preference?

    @Upsert
    suspend fun save(preference: Preference)

    @Query("SELECT * FROM preference")
    suspend fun listAll(): List<Preference>
}