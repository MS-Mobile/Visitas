package com.msmobile.visitas.householder

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.msmobile.visitas.VisitasDatabase
import com.msmobile.visitas.util.AddressProvider
import com.msmobile.visitas.util.AddressProvider.LocalityLookup
import com.msmobile.visitas.util.BackupHandler
import com.msmobile.visitas.util.LocalityBackfillFlag
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import javax.inject.Inject

/**
 * Runs the backfill against the real database and Google's real geocoder. The householders carry a
 * fixed point in central Criciúma, so the emulator's own location does not matter; without network
 * or a geocoder each test is skipped rather than failed.
 *
 * Uses [runBlocking], not runTest: runTest's virtual time would fire the geocoder timeout at once.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HouseholderLocalityBackfillTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    @ApplicationContext
    lateinit var context: Context

    @Inject
    lateinit var database: VisitasDatabase

    @Inject
    lateinit var addressProvider: AddressProvider

    @Inject
    lateinit var backfillFlag: LocalityBackfillFlag

    @Inject
    lateinit var backfill: HouseholderLocalityBackfill

    @Inject
    lateinit var backupHandler: BackupHandler

    @Before
    fun setUp() {
        hiltRule.inject()
        backfillFlag.clear()
    }

    @After
    fun tearDown() {
        backfillFlag.clear()
        database.close()
        File(context.cacheDir, "backups").deleteRecursively()
        context.deleteDatabase(VisitasDatabase.DATABASE_NAME)
    }

    @Test
    fun restart_withHouseholderMissingLocality_shouldFillNeighborhoodAndCity() = runBlocking {
        // Arrange
        assumeGeocoderAnswers()
        val householder = householderInCriciuma()
        database.householderDao().save(householder)

        // Act
        backfill.restart().join()

        // Assert
        val filled = database.householderDao().getById(householder.id)
        assertEquals("Criciúma", filled.city)
        assertNotNull(filled.neighborhood)
        assertTrue("A fully answered run must set the flag", backfillFlag.isDone())
    }

    @Test
    fun restart_withExistingNeighborhood_shouldKeepItAndFillTheCity() = runBlocking {
        // Arrange
        assumeGeocoderAnswers()
        val householder = householderInCriciuma().copy(neighborhood = "Manual")
        database.householderDao().save(householder)

        // Act
        backfill.restart().join()

        // Assert
        val filled = database.householderDao().getById(householder.id)
        assertEquals("Manual", filled.neighborhood)
        assertEquals("Criciúma", filled.city)
    }

    @Test
    fun restart_withHouseholderWithoutCoordinates_shouldLeaveItUntouched() = runBlocking {
        // Arrange
        assumeGeocoderAnswers()
        val withCoordinates = householderInCriciuma()
        val withoutCoordinates = householderInCriciuma()
            .copy(addressLatitude = null, addressLongitude = null)
        database.householderDao().save(withCoordinates)
        database.householderDao().save(withoutCoordinates)

        // Act
        backfill.restart().join()

        // Assert
        val untouched = database.householderDao().getById(withoutCoordinates.id)
        assertNull(untouched.neighborhood)
        assertNull(untouched.city)
        assertEquals("Criciúma", database.householderDao().getById(withCoordinates.id).city)
    }

    @Test
    fun restart_afterRestoringABackup_shouldFillTheRestoredHouseholder() = runBlocking {
        // Arrange
        assumeGeocoderAnswers()
        val householder = householderInCriciuma()
        database.householderDao().save(householder)
        val backupUri = backupHandler.createBackupFile().getOrThrow()
        database.clearAllTables()
        backupHandler.restoreBackup(backupUri).getOrThrow()

        // Act
        backfill.restart().join()

        // Assert
        val restored = database.householderDao().getById(householder.id)
        assertEquals("Criciúma", restored.city)
        assertNotNull(restored.neighborhood)
    }

    private suspend fun assumeGeocoderAnswers() {
        assumeTrue("No geocoder on this device", addressProvider.isGeocoderAvailable())
        val lookup = addressProvider.lookupLocality(CRICIUMA_LATITUDE, CRICIUMA_LONGITUDE)
        assumeTrue("Geocoder did not answer (offline?)", lookup is LocalityLookup.Found)
    }

    private fun householderInCriciuma() = Householder(
        id = UUID.randomUUID(),
        name = "Criciúma Householder",
        address = "Praça Nereu Ramos",
        notes = null,
        addressLatitude = CRICIUMA_LATITUDE,
        addressLongitude = CRICIUMA_LONGITUDE
    )

    private companion object {
        // Praça Nereu Ramos, Centro, Criciúma - SC.
        private const val CRICIUMA_LATITUDE = -28.6775
        private const val CRICIUMA_LONGITUDE = -49.3697
    }
}
