package com.msmobile.visitas.preference

import androidx.room.TypeConverter
import com.msmobile.visitas.visit.VisitListDateFilterOption
import com.msmobile.visitas.visit.VisitListDistanceFilterOption

class PreferenceTypeConverters {
    @TypeConverter
    fun fromVisitListFilterOption(value: VisitListDateFilterOption): String {
        return value.name
    }

    @TypeConverter
    fun toVisitListFilterOption(value: String): VisitListDateFilterOption {
        return try {
            VisitListDateFilterOption.valueOf(value)
        } catch (e: IllegalArgumentException) {
            VisitListDateFilterOption.PastDue
        }
    }


    @TypeConverter
    fun fromDistanceFilterOption(value: VisitListDistanceFilterOption): String {
        return value.name
    }

    @TypeConverter
    fun toDistanceFilterOption(value: String): VisitListDistanceFilterOption {
        return try {
            VisitListDistanceFilterOption.valueOf(value)
        } catch (e: IllegalArgumentException) {
            VisitListDistanceFilterOption.All
        }
    }
}