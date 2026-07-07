package com.msmobile.visitas.visit

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.msmobile.visitas.householder.Householder

/**
 * Last-committed copy of a visit belonging to a [com.msmobile.visitas.householder.HouseholderSnapshot]. The foreign key points
 * at the householder (not the visit) because the live visit row may be deleted while the
 * draft session is open, and the snapshot must survive to allow restoring it.
 */
@Entity(
    tableName = "visit_snapshot",
    primaryKeys = ["id"],
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
data class VisitSnapshot(
    @Embedded val visit: Visit
)