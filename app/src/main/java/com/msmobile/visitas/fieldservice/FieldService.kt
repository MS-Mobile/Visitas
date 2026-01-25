package com.msmobile.visitas.fieldservice

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

@Entity(tableName = "field_service")
data class FieldService(
    @PrimaryKey val id: UUID,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime?,
    val type: String,
    val duration: Int
)