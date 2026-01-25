package com.msmobile.visitas.util

import androidx.room.TypeConverter
import java.time.LocalDateTime

class RoomLocalDateTimeConverter {
    @TypeConverter
    fun fromLocalDateTime(localDateTime: LocalDateTime): String {
        return localDateTime.toString()
    }

    @TypeConverter
    fun toLocalDateTime(localDateTimeString: String): LocalDateTime {
        return LocalDateTime.parse(localDateTimeString)
    }
}