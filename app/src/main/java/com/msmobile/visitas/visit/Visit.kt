package com.msmobile.visitas.visit

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.msmobile.visitas.householder.Householder
import java.time.LocalDateTime
import java.util.UUID

@Entity(
    tableName = "visit",
    foreignKeys = [
        ForeignKey(
            entity = Householder::class,
            parentColumns = ["id"],
            childColumns = ["householderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("householderId")]
)
data class Visit(
    @PrimaryKey val id: UUID,
    val subject: String,
    val date: LocalDateTime,
    val isDone: Boolean,
    val householderId: UUID,
    val orderIndex: Int,
    val visitType: VisitType,
    val nextConversationId: UUID?,
    val calendarEventId: Long? = null
)
