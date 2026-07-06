package com.msmobile.visitas.visit

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.msmobile.visitas.householder.HouseholderSnapshot
import java.util.UUID

@Dao
interface SnapshotDao {
    @Upsert
    suspend fun saveHouseholderSnapshot(snapshot: HouseholderSnapshot)

    @Upsert
    suspend fun saveVisitSnapshot(snapshot: VisitSnapshot)

    @Query("SELECT * FROM householder_snapshot WHERE id = :householderId")
    suspend fun getHouseholderSnapshot(householderId: UUID): HouseholderSnapshot?

    @Query("SELECT * FROM visit_snapshot WHERE householderId = :householderId")
    suspend fun getVisitSnapshots(householderId: UUID): List<VisitSnapshot>

    @Query("DELETE FROM householder_snapshot WHERE id = :householderId")
    suspend fun deleteHouseholderSnapshot(householderId: UUID)

    @Query("DELETE FROM visit_snapshot WHERE householderId = :householderId")
    suspend fun deleteVisitSnapshots(householderId: UUID)
}
