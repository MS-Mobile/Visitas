package com.msmobile.visitas.householder

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import java.util.UUID

@Dao
interface HouseholderDao {
    @Upsert
    suspend fun save(householder: Householder)

    @Query("SELECT * FROM Householder WHERE id = :id")
    suspend fun getById(id: UUID): Householder

    @Query("SELECT * FROM Householder WHERE id = :id")
    suspend fun getByIdOrNull(id: UUID): Householder?

    @Query("DELETE FROM Householder WHERE id = :id")
    suspend fun deleteById(id: UUID)

    @Query("SELECT * FROM Householder")
    suspend fun listAll(): List<Householder>

    @Query(
        "SELECT * FROM Householder WHERE addressLatitude IS NOT NULL AND addressLongitude IS NOT NULL " +
            "AND (neighborhood IS NULL OR city IS NULL)"
    )
    suspend fun listMissingLocality(): List<Householder>

    /**
     * Fills only the null locality fields, and only while the householder still sits at the looked
     * up coordinates, so a pin moved during a backfill never receives the old point's locality.
     */
    @Query(
        "UPDATE Householder SET neighborhood = COALESCE(neighborhood, :neighborhood), " +
            "city = COALESCE(city, :city) WHERE id = :id AND addressLatitude = :latitude " +
            "AND addressLongitude = :longitude"
    )
    suspend fun fillLocality(
        id: UUID,
        latitude: Double,
        longitude: Double,
        neighborhood: String?,
        city: String?
    )
}