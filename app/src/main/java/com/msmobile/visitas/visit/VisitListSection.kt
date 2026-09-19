package com.msmobile.visitas.visit

import java.time.LocalDate

/**
 * A titled run of cards in the visit list. The list is split into sections so it can be scanned
 * at a glance: by period of the day when it shows a single day, by date otherwise.
 */
data class VisitListSection(
    val header: Header,
    val visits: List<VisitListViewModel.VisitHouseholderState>
) {
    sealed interface Header {
        data object Drafts : Header
        data class Period(val period: VisitPreferredTime) : Header
        data class Day(val date: LocalDate) : Header
    }
}

/**
 * Splits already-sorted, visible visits into sections.
 *
 * Drafts always come first in a section of their own, keeping the list's guarantee that drafts
 * lead the list (see the sort in `VisitListViewModel.filterBy`). The remaining visits go into
 * morning / afternoon / evening sections when [groupByPeriod] is set, or into one section per day
 * otherwise, in chronological order. Within a section the incoming order is kept, so nearby visits
 * still sort first. Empty sections are left out.
 */
fun List<VisitListViewModel.VisitHouseholderState>.toSections(
    groupByPeriod: Boolean
): List<VisitListSection> {
    val (drafts, others) = partition { visit -> visit.hasDrafts }
    val draftSection = if (drafts.isEmpty()) {
        emptyList()
    } else {
        listOf(VisitListSection(VisitListSection.Header.Drafts, drafts))
    }
    val otherSections = if (groupByPeriod) {
        others.groupBy { visit -> VisitTimeValidator.periodOf(visit.date.toLocalTime()) }
            .toSortedMap()
            .map { (period, visits) -> VisitListSection(VisitListSection.Header.Period(period), visits) }
    } else {
        others.groupBy { visit -> visit.date.toLocalDate() }
            .toSortedMap()
            .map { (date, visits) -> VisitListSection(VisitListSection.Header.Day(date), visits) }
    }
    return draftSection + otherSections
}
