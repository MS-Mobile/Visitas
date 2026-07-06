package com.msmobile.visitas.householder

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import java.time.LocalDateTime

/**
 * Last-committed copy of a householder taken right before the first draft auto-save of an
 * edit session overwrites the live row. Discarding the draft restores from this snapshot;
 * saving deletes it. [isNewDraft] marks a householder that was never saved by the user, in
 * which case discarding deletes the householder instead of restoring it.
 *
 * The primary key is declared at table level because @PrimaryKey inside an @Embedded class
 * is not honored by Room.
 */
@Entity(
    tableName = "householder_snapshot",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(
            entity = Householder::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HouseholderSnapshot(
    @Embedded val householder: Householder,
    val isNewDraft: Boolean,
    val createdAt: LocalDateTime
)