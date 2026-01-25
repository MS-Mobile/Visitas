package com.msmobile.visitas.preference

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.msmobile.visitas.visit.VisitListDateFilterOption
import com.msmobile.visitas.visit.VisitListDistanceFilterOption
import java.util.UUID

@Entity(tableName = "preference")
data class Preference(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val visitListDateFilterOption: VisitListDateFilterOption,
    val visitListDistanceFilterOption: VisitListDistanceFilterOption
)