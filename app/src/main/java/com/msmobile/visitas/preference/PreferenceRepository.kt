package com.msmobile.visitas.preference

import com.msmobile.visitas.visit.VisitListDateFilterOption
import com.msmobile.visitas.visit.VisitListDistanceFilterOption

class PreferenceRepository(private val preferenceDao: PreferenceDao) {
    suspend fun get(): Preference {
        return preferenceDao.getPreference() ?: Preference(
            visitListDateFilterOption = VisitListDateFilterOption.All,
            visitListDistanceFilterOption = VisitListDistanceFilterOption.All
        )
    }

    suspend fun save(preference: Preference) {
        preferenceDao.save(preference)
    }
}