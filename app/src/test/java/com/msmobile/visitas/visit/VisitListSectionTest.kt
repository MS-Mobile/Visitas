package com.msmobile.visitas.visit

import com.msmobile.visitas.util.AddressProvider
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class VisitListSectionTest {

    @Test
    fun `periodOf splits the day at noon and six pm`() {
        assertEquals(VisitPreferredTime.MORNING, VisitTimeValidator.periodOf(LocalTime.of(11, 59)))
        assertEquals(VisitPreferredTime.AFTERNOON, VisitTimeValidator.periodOf(LocalTime.of(12, 0)))
        assertEquals(VisitPreferredTime.AFTERNOON, VisitTimeValidator.periodOf(LocalTime.of(17, 59)))
        assertEquals(VisitPreferredTime.EVENING, VisitTimeValidator.periodOf(LocalTime.of(18, 0)))
    }

    @Test
    fun `periodOf folds late night into evening and early hours into morning`() {
        assertEquals(VisitPreferredTime.MORNING, VisitTimeValidator.periodOf(LocalTime.MIDNIGHT))
        assertEquals(VisitPreferredTime.MORNING, VisitTimeValidator.periodOf(LocalTime.of(5, 59)))
        assertEquals(VisitPreferredTime.EVENING, VisitTimeValidator.periodOf(LocalTime.of(22, 0)))
        assertEquals(VisitPreferredTime.EVENING, VisitTimeValidator.periodOf(LocalTime.of(23, 59)))
    }

    @Test
    fun `toSections by period orders morning, afternoon, evening and keeps incoming order within`() {
        // Arrange — incoming order is the view model's (nearby first), not chronological
        val evening = visit(at(19, 0))
        val lateMorning = visit(at(11, 0))
        val afternoon = visit(at(14, 0))
        val earlyMorning = visit(at(9, 0))

        // Act
        val sections = listOf(evening, lateMorning, afternoon, earlyMorning)
            .toSections(groupByPeriod = true)

        // Assert
        assertEquals(
            listOf(
                VisitListSection.Header.Period(VisitPreferredTime.MORNING),
                VisitListSection.Header.Period(VisitPreferredTime.AFTERNOON),
                VisitListSection.Header.Period(VisitPreferredTime.EVENING)
            ),
            sections.map { it.header }
        )
        assertEquals(listOf(lateMorning, earlyMorning), sections[0].visits)
    }

    @Test
    fun `toSections leaves out periods with no visits`() {
        val sections = listOf(visit(at(9, 0)), visit(at(20, 0))).toSections(groupByPeriod = true)

        assertEquals(
            listOf(
                VisitListSection.Header.Period(VisitPreferredTime.MORNING),
                VisitListSection.Header.Period(VisitPreferredTime.EVENING)
            ),
            sections.map { it.header }
        )
    }

    @Test
    fun `toSections puts drafts first in their own section in both modes`() {
        val draft = visit(at(20, 0), hasDrafts = true)
        val regular = visit(at(9, 0))

        listOf(true, false).forEach { groupByPeriod ->
            val sections = listOf(regular, draft).toSections(groupByPeriod = groupByPeriod)

            assertEquals(VisitListSection.Header.Drafts, sections.first().header)
            assertEquals(listOf(draft), sections.first().visits)
            assertTrue(sections.drop(1).none { draft in it.visits })
        }
    }

    @Test
    fun `toSections by date gives one chronological section per day`() {
        val dayTwo = visit(LocalDateTime.of(2026, 9, 21, 9, 0))
        val dayOneLate = visit(LocalDateTime.of(2026, 9, 20, 19, 0))
        val dayOneEarly = visit(LocalDateTime.of(2026, 9, 20, 8, 0))

        val sections = listOf(dayTwo, dayOneLate, dayOneEarly).toSections(groupByPeriod = false)

        assertEquals(
            listOf(
                VisitListSection.Header.Day(LocalDate.of(2026, 9, 20)),
                VisitListSection.Header.Day(LocalDate.of(2026, 9, 21))
            ),
            sections.map { it.header }
        )
        assertEquals(listOf(dayOneLate, dayOneEarly), sections[0].visits)
    }

    @Test
    fun `toSections of an empty list is empty`() {
        assertTrue(emptyList<VisitListViewModel.VisitHouseholderState>().toSections(true).isEmpty())
    }

    private fun at(hour: Int, minute: Int) = LocalDateTime.of(2026, 9, 19, hour, minute)

    private fun visit(date: LocalDateTime, hasDrafts: Boolean = false) =
        VisitListViewModel.VisitHouseholderState(
            visitId = UUID.randomUUID(),
            subject = "",
            subjectPreview = "",
            date = date,
            isDone = false,
            hasDrafts = hasDrafts,
            householderId = UUID.randomUUID(),
            householderName = "",
            householderAddressDistance = AddressProvider.AddressDistance.NoData,
            householderAddressState = VisitListViewModel.HouseholderAddressState.NoData,
            hide = false,
            isPendingVisitMenuExpanded = false,
            hasToBeRescheduled = false,
            type = VisitType.FIRST_VISIT
        )
}
