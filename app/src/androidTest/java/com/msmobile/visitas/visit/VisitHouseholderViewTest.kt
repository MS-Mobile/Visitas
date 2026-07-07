package com.msmobile.visitas.visit

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.msmobile.visitas.VisitasDatabase
import com.msmobile.visitas.householder.Householder
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

/**
 * Verifies the aggregate `hasDrafts` column on the `visit_householder` view. The row for a
 * householder is its latest visit, but `hasDrafts` must reflect the whole householder: it is true
 * when the householder row is a draft OR any of its visits is a draft (not just the latest one).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class VisitHouseholderViewTest {
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
    fun hasDrafts_isFalse_whenNeitherHouseholderNorAnyVisitIsDraft() = runTest {
        val householderId = UUID.randomUUID()
        database.householderDao().save(householder(householderId, isDraft = false))
        database.visitDao().save(visit(householderId, date = NOW, isDraft = false))

        val row = database.visitHouseholderDao().getAll().first { it.householderId == householderId }

        assertFalse("No drafts anywhere -> hasDrafts should be false", row.hasDrafts)
    }

    @Test
    fun hasDrafts_isTrue_whenANonLatestVisitIsADraft() = runTest {
        val householderId = UUID.randomUUID()
        database.householderDao().save(householder(householderId, isDraft = false))
        // Latest visit is not a draft; an older visit is. The view row is the latest visit.
        val latestVisit = visit(householderId, date = NOW, isDraft = false)
        val olderDraftVisit = visit(householderId, date = NOW.minusDays(1), isDraft = true)
        database.visitDao().save(latestVisit)
        database.visitDao().save(olderDraftVisit)

        val row = database.visitHouseholderDao().getAll().first { it.householderId == householderId }

        assertEquals("View row should be the latest visit", latestVisit.id, row.visitId)
        assertTrue("A draft on a non-latest visit must set hasDrafts", row.hasDrafts)
    }

    @Test
    fun hasDrafts_isTrue_whenOnlyTheHouseholderIsADraft() = runTest {
        val householderId = UUID.randomUUID()
        database.householderDao().save(householder(householderId, isDraft = true))
        database.visitDao().save(visit(householderId, date = NOW, isDraft = false))

        val row = database.visitHouseholderDao().getAll().first { it.householderId == householderId }

        assertTrue("A householder-only draft must set hasDrafts", row.hasDrafts)
    }

    private fun householder(id: UUID, isDraft: Boolean) = Householder(
        id = id,
        name = "Test Householder",
        address = "Test Address",
        notes = null,
        isDraft = isDraft
    )

    private fun visit(householderId: UUID, date: LocalDateTime, isDraft: Boolean) = Visit(
        id = UUID.randomUUID(),
        subject = "Subject",
        date = date,
        isDone = false,
        householderId = householderId,
        orderIndex = 0,
        visitType = VisitType.FIRST_VISIT,
        nextConversationId = null,
        isDraft = isDraft
    )

    companion object {
        private val NOW: LocalDateTime = LocalDateTime.of(2026, 1, 15, 10, 30)
    }
}
