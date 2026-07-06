package com.msmobile.visitas.visit

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.msmobile.visitas.householder.Householder
import java.util.UUID

@Dao
interface SnapshotDao {
    @Upsert
    suspend fun saveHouseholderSnapshot(snapshot: HouseholderSnapshot)

    @Upsert
    suspend fun saveVisitSnapshots(snapshots: List<VisitSnapshot>)

    @Query("SELECT * FROM householder_snapshot WHERE id = :householderId")
    suspend fun getHouseholderSnapshot(householderId: UUID): HouseholderSnapshot?

    @Query("SELECT * FROM visit_snapshot WHERE householderId = :householderId")
    suspend fun getVisitSnapshots(householderId: UUID): List<VisitSnapshot>

    @Query("DELETE FROM householder_snapshot WHERE id = :householderId")
    suspend fun deleteHouseholderSnapshot(householderId: UUID)

    @Query("DELETE FROM visit_snapshot WHERE householderId = :householderId")
    suspend fun deleteVisitSnapshots(householderId: UUID)

    @Upsert
    suspend fun restoreHouseholder(householder: Householder)

    @Query("DELETE FROM visit WHERE householderId = :householderId")
    suspend fun deleteVisitsByHouseholderId(householderId: UUID)

    @Insert
    suspend fun restoreVisits(visits: List<Visit>)

    @Transaction
    suspend fun save(householderSnapshot: HouseholderSnapshot, visitSnapshots: List<VisitSnapshot>) {
        saveHouseholderSnapshot(householderSnapshot)
        saveVisitSnapshots(visitSnapshots)
    }

    @Transaction
    suspend fun delete(householderId: UUID) {
        deleteVisitSnapshots(householderId)
        deleteHouseholderSnapshot(householderId)
    }

    /**
     * Atomically overwrites the live householder and visit rows with the snapshot taken at the
     * start of the draft session, then deletes the snapshot. Visits added during the draft are
     * removed and visits removed during the draft come back, because the whole visit set is
     * replaced. Returns false when no snapshot exists for [householderId].
     */
    @Transaction
    suspend fun restore(householderId: UUID): Boolean {
        val snapshot = getHouseholderSnapshot(householderId) ?: return false
        restoreHouseholder(snapshot.householder)
        val visitSnapshots = getVisitSnapshots(householderId)
        deleteVisitsByHouseholderId(householderId)
        restoreVisits(visitSnapshots.map { it.visit })
        delete(householderId)
        return true
    }
}
