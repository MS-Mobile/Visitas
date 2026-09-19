package com.msmobile.visitas.visit

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

object VisitTimeValidator {

    private val MORNING_RANGE = LocalTime.of(6, 0)..LocalTime.of(11, 59)
    private val AFTERNOON_RANGE = LocalTime.of(12, 0)..LocalTime.of(17, 59)
    private val EVENING_RANGE = LocalTime.of(18, 0)..LocalTime.of(21, 59)

    private val WEEKDAYS = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    )

    private val WEEKENDS = setOf(
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY
    )

    fun isValidVisitTime(
        visitDateTime: LocalDateTime,
        preferredDay: VisitPreferredDay,
        preferredTime: VisitPreferredTime
    ): Boolean {
        val isDayValid = isDayValid(visitDateTime.dayOfWeek, preferredDay)
        val isTimeValid = isTimeValid(visitDateTime.toLocalTime(), preferredTime)
        return isDayValid && isTimeValid
    }

    /**
     * The period of the day [time] falls into, for grouping visits by time of day. Never returns
     * [VisitPreferredTime.ANY].
     *
     * Shares its boundaries with the preferred-time ranges, but unlike them it covers the whole
     * day: the hours before [MORNING_RANGE] count as morning, the hours after [EVENING_RANGE] as
     * evening, so every visit lands in a period.
     */
    fun periodOf(time: LocalTime): VisitPreferredTime {
        return when {
            time < AFTERNOON_RANGE.start -> VisitPreferredTime.MORNING
            time < EVENING_RANGE.start -> VisitPreferredTime.AFTERNOON
            else -> VisitPreferredTime.EVENING
        }
    }

    private fun isDayValid(dayOfWeek: DayOfWeek, preferredDay: VisitPreferredDay): Boolean {
        return when (preferredDay) {
            VisitPreferredDay.ANY -> true
            VisitPreferredDay.SUNDAY -> dayOfWeek == DayOfWeek.SUNDAY
            VisitPreferredDay.MONDAY -> dayOfWeek == DayOfWeek.MONDAY
            VisitPreferredDay.TUESDAY -> dayOfWeek == DayOfWeek.TUESDAY
            VisitPreferredDay.WEDNESDAY -> dayOfWeek == DayOfWeek.WEDNESDAY
            VisitPreferredDay.THURSDAY -> dayOfWeek == DayOfWeek.THURSDAY
            VisitPreferredDay.FRIDAY -> dayOfWeek == DayOfWeek.FRIDAY
            VisitPreferredDay.SATURDAY -> dayOfWeek == DayOfWeek.SATURDAY
            VisitPreferredDay.WEEKDAYS -> dayOfWeek in WEEKDAYS
            VisitPreferredDay.WEEKENDS -> dayOfWeek in WEEKENDS
        }
    }

    private fun isTimeValid(time: LocalTime, preferredTime: VisitPreferredTime): Boolean {
        return when (preferredTime) {
            VisitPreferredTime.ANY -> true
            VisitPreferredTime.MORNING -> time in MORNING_RANGE
            VisitPreferredTime.AFTERNOON -> time in AFTERNOON_RANGE
            VisitPreferredTime.EVENING -> time in EVENING_RANGE
        }
    }
}
