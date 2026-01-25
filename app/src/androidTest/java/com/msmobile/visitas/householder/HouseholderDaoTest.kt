package com.msmobile.visitas.householder

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.msmobile.visitas.VisitasDatabase
import com.msmobile.visitas.visit.Visit
import com.msmobile.visitas.visit.VisitType
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HouseholderDaoTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    @ApplicationContext
    lateinit var context: Context

    @Inject
    lateinit var database: VisitasDatabase

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(VisitasDatabase.DATABASE_NAME)
    }

    @Test
    fun saveHouseholder_whenUpdatingExistingHouseholder_shouldNotDeleteVisits() = runTest {
        // Arrange
        val householderId = UUID.randomUUID()
        val householder = Householder(
            id = householderId,
            name = "John Doe",
            address = "123 Test Street",
            notes = "Test notes",
            addressLatitude = 40.7128,
            addressLongitude = -74.0060
        )

        val visit1 = Visit(
            id = UUID.randomUUID(),
            subject = "First visit",
            date = LocalDateTime.now(),
            isDone = false,
            householderId = householderId,
            orderIndex = 0,
            visitType = VisitType.FIRST_VISIT,
            nextConversationId = null
        )

        val visit2 = Visit(
            id = UUID.randomUUID(),
            subject = "Second visit",
            date = LocalDateTime.now().plusDays(1),
            isDone = false,
            householderId = householderId,
            orderIndex = 1,
            visitType = VisitType.RETURN_VISIT,
            nextConversationId = null
        )

        val visit3 = Visit(
            id = UUID.randomUUID(),
            subject = "Third visit",
            date = LocalDateTime.now().plusDays(2),
            isDone = true,
            householderId = householderId,
            orderIndex = 2,
            visitType = VisitType.BIBLE_STUDY,
            nextConversationId = null
        )

        // Insert householder and visits
        database.householderDao().save(householder)
        database.visitDao().save(visit1)
        database.visitDao().save(visit2)
        database.visitDao().save(visit3)

        // Verify initial state
        val visitsBefore = database.visitDao().getByHouseholderId(householderId)
        assertEquals("Should have 3 visits before update", 3, visitsBefore.size)

        // Act - Update the householder (change address)
        val updatedHouseholder = householder.copy(address = "456 Updated Street")
        database.householderDao().save(updatedHouseholder)

        // Assert
        val visitsAfter = database.visitDao().getByHouseholderId(householderId)
        assertEquals(
            "Should still have 3 visits after updating householder",
            3,
            visitsAfter.size
        )

        // Verify the householder was actually updated
        val savedHouseholder = database.householderDao().getById(householderId)
        assertNotNull(savedHouseholder)
        assertEquals("456 Updated Street", savedHouseholder.address)

        // Verify visits data integrity
        val visitIds = visitsAfter.map { it.id }.toSet()
        assertEquals(true, visitIds.contains(visit1.id))
        assertEquals(true, visitIds.contains(visit2.id))
        assertEquals(true, visitIds.contains(visit3.id))
    }

    @Test
    fun saveHouseholder_whenUpdatingMultipleFields_shouldNotDeleteVisits() = runTest {
        // Arrange
        val householderId = UUID.randomUUID()
        val householder = Householder(
            id = householderId,
            name = "Jane Doe",
            address = "456 Original Street",
            notes = "Original notes",
            addressLatitude = null,
            addressLongitude = null
        )

        val visit = Visit(
            id = UUID.randomUUID(),
            subject = "Test visit",
            date = LocalDateTime.now(),
            isDone = false,
            householderId = householderId,
            orderIndex = 0,
            visitType = VisitType.FIRST_VISIT,
            nextConversationId = null
        )

        // Insert householder and visit
        database.householderDao().save(householder)
        database.visitDao().save(visit)

        // Verify initial state
        val visitsBefore = database.visitDao().getByHouseholderId(householderId)
        assertEquals(1, visitsBefore.size)

        // Act - Update multiple fields of the householder
        val updatedHouseholder = householder.copy(
            name = "Jane Smith",
            address = "789 New Street",
            notes = "Updated notes",
            addressLatitude = 51.5074,
            addressLongitude = -0.1278
        )
        database.householderDao().save(updatedHouseholder)

        // Assert
        val visitsAfter = database.visitDao().getByHouseholderId(householderId)
        assertEquals(
            "Visit should not be deleted when updating householder",
            1,
            visitsAfter.size
        )
        assertEquals(visit.id, visitsAfter.first().id)

        // Verify householder was updated
        val savedHouseholder = database.householderDao().getById(householderId)
        assertEquals("Jane Smith", savedHouseholder.name)
        assertEquals("789 New Street", savedHouseholder.address)
        assertEquals("Updated notes", savedHouseholder.notes)
        assertEquals(51.5074, savedHouseholder.addressLatitude)
        assertEquals(-0.1278, savedHouseholder.addressLongitude)
    }

    @Test
    fun saveHouseholder_consecutiveUpdates_shouldNotDeleteVisits() = runTest {
        // Arrange
        val householderId = UUID.randomUUID()
        val householder = Householder(
            id = householderId,
            name = "Test User",
            address = "Test Address",
            notes = null,
            addressLatitude = null,
            addressLongitude = null
        )

        val visit = Visit(
            id = UUID.randomUUID(),
            subject = "Test visit",
            date = LocalDateTime.now(),
            isDone = false,
            householderId = householderId,
            orderIndex = 0,
            visitType = VisitType.RETURN_VISIT,
            nextConversationId = null
        )

        // Insert householder and visit
        database.householderDao().save(householder)
        database.visitDao().save(visit)

        // Act - Perform multiple consecutive updates
        repeat(5) { iteration ->
            val updated = database.householderDao().getById(householderId).copy(
                address = "Updated Address $iteration"
            )
            database.householderDao().save(updated)

            // Assert after each update
            val visits = database.visitDao().getByHouseholderId(householderId)
            assertEquals(
                "Visit should exist after update iteration $iteration",
                1,
                visits.size
            )
        }

        // Final verification
        val finalVisits = database.visitDao().getByHouseholderId(householderId)
        assertEquals(1, finalVisits.size)
        assertEquals(visit.id, finalVisits.first().id)
    }
}

