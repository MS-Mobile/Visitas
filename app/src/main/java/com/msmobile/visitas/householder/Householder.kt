package com.msmobile.visitas.householder

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.msmobile.visitas.visit.VisitPreferredDay
import com.msmobile.visitas.visit.VisitPreferredTime
import java.util.UUID

@Entity(tableName = "householder")
data class Householder(
    @PrimaryKey val id: UUID,
    val name: String,
    val address: String,
    val notes: String?,
    val addressLatitude: Double? = null,
    val addressLongitude: Double? = null,
    val preferredDay: VisitPreferredDay = VisitPreferredDay.ANY,
    val preferredTime: VisitPreferredTime = VisitPreferredTime.ANY
)