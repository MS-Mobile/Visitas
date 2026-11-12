package com.msmobile.visitas.appfunctions

import androidx.appfunctions.AppFunctionContext
import com.msmobile.visitas.util.DateTimeProvider
import com.msmobile.visitas.util.DispatcherProvider
import com.msmobile.visitas.util.MainDispatcherRule
import com.msmobile.visitas.visit.VisitHouseholder
import com.msmobile.visitas.visit.VisitHouseholderRepository
import com.msmobile.visitas.visit.VisitType
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

class VisitAppFunctionsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val now = LocalDateTime.of(2026, 11, 7, 9, 0)

    @Test
    fun `listVisits returns scheduledDate as ISO 8601 with the device zone offset`() = runTest {
        // Arrange
        val visit = createVisit(date = LocalDateTime.of(2026, 11, 7, 10, 30))
        val appFunctions = createAppFunctions(
            visits = listOf(visit),
            zoneId = ZoneId.of("America/Sao_Paulo")
        )

        // Act
        val result = appFunctions.listVisits(mock<AppFunctionContext>())

        // Assert
        assertEquals("2026-11-07T10:30:00-03:00", result.visits.single().scheduledDate)
    }

    @Test
    fun `listVisits keeps the local wall-clock time and only appends the offset`() = runTest {
        // Arrange
        val visit = createVisit(date = LocalDateTime.of(2026, 11, 7, 10, 30))
        val appFunctions = createAppFunctions(
            visits = listOf(visit),
            zoneId = ZoneId.of("Asia/Kolkata")
        )

        // Act
        val result = appFunctions.listVisits(mock<AppFunctionContext>())

        // Assert
        assertEquals("2026-11-07T10:30:00+05:30", result.visits.single().scheduledDate)
    }

    @Test
    fun `listVisits uses Z as the offset when the device zone is UTC`() = runTest {
        // Arrange
        val visit = createVisit(date = LocalDateTime.of(2026, 11, 7, 10, 30))
        val appFunctions = createAppFunctions(
            visits = listOf(visit),
            zoneId = ZoneOffset.UTC
        )

        // Act
        val result = appFunctions.listVisits(mock<AppFunctionContext>())

        // Assert
        assertEquals("2026-11-07T10:30:00Z", result.visits.single().scheduledDate)
    }

    @Test
    fun `listVisits uses the offset in effect on the visit date across DST changes`() = runTest {
        // Arrange
        val summerVisit = createVisit(date = LocalDateTime.of(2027, 7, 1, 10, 30))
        val winterVisit = createVisit(date = LocalDateTime.of(2027, 1, 1, 10, 30))
        val appFunctions = createAppFunctions(
            visits = listOf(winterVisit, summerVisit),
            zoneId = ZoneId.of("America/New_York")
        )

        // Act
        val result = appFunctions.listVisits(mock<AppFunctionContext>())

        // Assert
        assertEquals(
            listOf("2027-01-01T10:30:00-05:00", "2027-07-01T10:30:00-04:00"),
            result.visits.map { it.scheduledDate }
        )
    }

    @Test
    fun `listVisits maps the remaining visit fields`() = runTest {
        // Arrange
        val visit = createVisit(
            date = LocalDateTime.of(2026, 11, 7, 10, 30),
            subject = "Bible reading",
            householderName = "Maria Silva",
            householderAddress = "Rua A, 123",
            type = VisitType.BIBLE_STUDY
        )
        val appFunctions = createAppFunctions(visits = listOf(visit))

        // Act
        val result = appFunctions.listVisits(mock<AppFunctionContext>())

        // Assert
        val item = result.visits.single()
        assertEquals("Bible reading", item.subject)
        assertEquals("Maria Silva", item.householderName)
        assertEquals("Rua A, 123", item.address)
        assertEquals("BIBLE_STUDY", item.visitType)
        assertEquals(1, result.count)
    }

    @Test
    fun `listVisits without a filter returns upcoming pending visits`() = runTest {
        // Arrange
        val today = createVisit(date = now.plusHours(1), subject = "today")
        val nextWeek = createVisit(date = now.plusDays(7), subject = "next week")
        val pastDue = createVisit(date = now.minusDays(1), subject = "past due")
        val done = createVisit(date = now.plusDays(1), subject = "done", isDone = true)
        val appFunctions = createAppFunctions(visits = listOf(today, nextWeek, pastDue, done))

        // Act
        val result = appFunctions.listVisits(mock<AppFunctionContext>())

        // Assert
        assertEquals(listOf("today", "next week"), result.visits.map { it.subject })
        assertEquals(2, result.count)
    }

    @Test
    fun `listVisits with today filter returns only pending visits scheduled today`() = runTest {
        // Arrange
        val earlierToday = createVisit(date = now.toLocalDate().atStartOfDay(), subject = "earlier today")
        val laterToday = createVisit(date = now.withHour(23), subject = "later today")
        val tomorrow = createVisit(date = now.plusDays(1), subject = "tomorrow")
        val doneToday = createVisit(date = now.plusHours(1), subject = "done", isDone = true)
        val appFunctions = createAppFunctions(visits = listOf(earlierToday, laterToday, tomorrow, doneToday))

        // Act
        val result = appFunctions.listVisits(mock<AppFunctionContext>(), dateFilter = "today")

        // Assert
        assertEquals(listOf("earlier today", "later today"), result.visits.map { it.subject })
    }

    @Test
    fun `listVisits with tomorrow filter returns only pending visits scheduled tomorrow`() = runTest {
        // Arrange
        val today = createVisit(date = now.plusHours(1), subject = "today")
        val tomorrow = createVisit(date = now.plusDays(1), subject = "tomorrow")
        val dayAfterTomorrow = createVisit(
            date = now.toLocalDate().plusDays(2).atStartOfDay(),
            subject = "day after tomorrow"
        )
        val appFunctions = createAppFunctions(visits = listOf(today, tomorrow, dayAfterTomorrow))

        // Act
        val result = appFunctions.listVisits(mock<AppFunctionContext>(), dateFilter = "tomorrow")

        // Assert
        assertEquals(listOf("tomorrow"), result.visits.map { it.subject })
    }

    @Test
    fun `listVisits with past_due filter returns pending visits before today`() = runTest {
        // Arrange
        val yesterday = createVisit(date = now.minusDays(1), subject = "yesterday")
        val yesterdayDone = createVisit(date = now.minusDays(1), subject = "done", isDone = true)
        val earlierToday = createVisit(date = now.minusHours(1), subject = "earlier today")
        val appFunctions = createAppFunctions(visits = listOf(yesterday, yesterdayDone, earlierToday))

        // Act
        val result = appFunctions.listVisits(mock<AppFunctionContext>(), dateFilter = "past_due")

        // Assert
        assertEquals(listOf("yesterday"), result.visits.map { it.subject })
    }

    @Test
    fun `listVisits with done filter returns completed visits regardless of date`() = runTest {
        // Arrange
        val donePast = createVisit(date = now.minusDays(3), subject = "done past", isDone = true)
        val doneFuture = createVisit(date = now.plusDays(3), subject = "done future", isDone = true)
        val pending = createVisit(date = now.plusDays(1), subject = "pending")
        val appFunctions = createAppFunctions(visits = listOf(donePast, doneFuture, pending))

        // Act
        val result = appFunctions.listVisits(mock<AppFunctionContext>(), dateFilter = "done")

        // Assert
        assertEquals(listOf("done past", "done future"), result.visits.map { it.subject })
    }

    @Test
    fun `listVisits matches the filter case-insensitively`() = runTest {
        // Arrange
        val today = createVisit(date = now.plusHours(1), subject = "today")
        val appFunctions = createAppFunctions(visits = listOf(today))

        // Act
        val result = appFunctions.listVisits(mock<AppFunctionContext>(), dateFilter = "TODAY")

        // Assert
        assertEquals(listOf("today"), result.visits.map { it.subject })
    }

    private fun createAppFunctions(
        visits: List<VisitHouseholder>,
        zoneId: ZoneId = ZoneId.of("America/Sao_Paulo")
    ): VisitAppFunctions {
        val visitHouseholderRepository = mock<VisitHouseholderRepository> {
            onBlocking { getAll() } doReturn visits
        }
        val dateTimeProvider = mock<DateTimeProvider> {
            on { nowLocalDateTime() } doReturn now
            on { zoneId() } doReturn zoneId
        }
        return VisitAppFunctions(
            visitHouseholderRepository = visitHouseholderRepository,
            dateTimeProvider = dateTimeProvider,
            dispatcherProvider = DispatcherProvider(io = mainDispatcherRule.dispatcher)
        )
    }

    private fun createVisit(
        date: LocalDateTime,
        subject: String = "Subject",
        isDone: Boolean = false,
        householderName: String = "Householder",
        householderAddress: String = "Address",
        type: VisitType = VisitType.RETURN_VISIT
    ): VisitHouseholder {
        return VisitHouseholder(
            visitId = UUID.randomUUID(),
            subject = subject,
            date = date,
            isDone = isDone,
            householderId = UUID.randomUUID(),
            householderName = householderName,
            householderAddress = householderAddress,
            type = type,
            householderLatitude = null,
            householderLongitude = null
        )
    }
}
