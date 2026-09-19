package com.msmobile.visitas.visit

import com.msmobile.visitas.util.AddressProvider
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
        data object Nearby : Header
        data class Period(val period: VisitPreferredTime) : Header
        data class Day(val date: LocalDate) : Header
    }
}

/**
 * Splits already-sorted, visible visits into sections.
 *
 * Drafts always come first in a section of their own, keeping the list's guarantee that drafts
 * lead the list (see the sort in `VisitListViewModel.filterBy`). When [showNearby] is set, nearby
 * visits follow in their own section: that filter shows them whatever their date, so they must not
 * be filed under a period or a day they may not belong to, and it sorts them to the top of the
 * list. The remaining visits go into morning / afternoon / evening sections when [groupByPeriod]
 * is set, or into one section per day otherwise, in chronological order. Within a section the
 * incoming order is kept. Empty sections are left out.
 */
fun List<VisitListViewModel.VisitHouseholderState>.toSections(
    groupByPeriod: Boolean,
    showNearby: Boolean
): List<VisitListSection> {
    val (drafts, notDrafts) = partition { visit -> visit.hasDrafts }
    val (nearby, others) = notDrafts.partition { visit ->
        showNearby && visit.householderAddressDistance is AddressProvider.AddressDistance.Nearby
    }
    val leadingSections = listOf(
        VisitListSection(VisitListSection.Header.Drafts, drafts),
        VisitListSection(VisitListSection.Header.Nearby, nearby)
    ).filter { section -> section.visits.isNotEmpty() }
    val otherSections = if (groupByPeriod) {
        others.groupBy { visit -> VisitTimeValidator.periodOf(visit.date.toLocalTime()) }
            .toSortedMap()
            .map { (period, visits) -> VisitListSection(VisitListSection.Header.Period(period), visits) }
    } else {
        others.groupBy { visit -> visit.date.toLocalDate() }
            .toSortedMap()
            .map { (date, visits) -> VisitListSection(VisitListSection.Header.Day(date), visits) }
    }
    return leadingSections + otherSections
}
