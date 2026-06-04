package com.msmobile.visitas.appfunctions

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.service.AppFunction
import com.msmobile.visitas.util.DateTimeProvider
import com.msmobile.visitas.util.DispatcherProvider
import com.msmobile.visitas.util.LocaleProvider
import com.msmobile.visitas.visit.VisitHouseholderRepository
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

class VisitAppFunctions @Inject constructor(
    private val visitHouseholderRepository: VisitHouseholderRepository,
    private val dateTimeProvider: DateTimeProvider,
    private val dispatcherProvider: DispatcherProvider,
    private val localeProvider: LocaleProvider,
) {
    @AppFunctionSerializable(isDescribedByKDoc = true)
    data class VisitItem(
        /** Subject or topic discussed during this visit. */
        val subject: String,
        /** Full name of the person to visit. */
        val householderName: String,
        /** Street address of the person to visit. */
        val address: String,
        /** Scheduled date and time as a human-readable string. */
        val scheduledDate: String,
        /** Type of visit: FIRST_VISIT, RETURN_VISIT, or BIBLE_STUDY. */
        val visitType: String
    )

    @AppFunctionSerializable(isDescribedByKDoc = true)
    data class VisitListResult(
        /** Visits matching the requested filter. */
        val visits: List<VisitItem>,
        /** Total number of matching visits. */
        val count: Int
    )

    /**
     * Lists scheduled visits filtered by date.
     * Use this to answer questions like "What visits do I have today?",
     * "Show me tomorrow's visits", or "What visits are past due?"
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun listVisits(
        appFunctionContext: AppFunctionContext,
        /**
         * Optional date filter. Accepted values:
         * "today" – visits scheduled for today,
         * "tomorrow" – visits scheduled for tomorrow,
         * "past_due" – overdue visits not yet completed,
         * "done" – completed visits.
         * Omit to list all upcoming pending visits.
         */
        dateFilter: String? = null
    ): VisitListResult = withContext(dispatcherProvider.io) {
        val allVisits = visitHouseholderRepository.getAll()
        val todayStart = dateTimeProvider.nowLocalDateTime().toLocalDate().atStartOfDay()
        val tomorrowStart = todayStart.plusDays(1)
        val dayAfterTomorrowStart = todayStart.plusDays(2)

        val filtered = when (dateFilter?.lowercase(Locale.ROOT)) {
            // Omitted: default to all upcoming pending visits.
            null -> allVisits.filter { !it.isDone && it.date >= todayStart }
            "today" -> allVisits.filter {
                !it.isDone && it.date >= todayStart && it.date < tomorrowStart
            }
            "tomorrow" -> allVisits.filter {
                !it.isDone && it.date >= tomorrowStart && it.date < dayAfterTomorrowStart
            }
            "past_due" -> allVisits.filter {
                !it.isDone && it.date < todayStart
            }
            "done" -> allVisits.filter { it.isDone }
            // Provided but unrecognized: surface an error so the agent retries with a
            // documented value instead of silently returning a wrong result set.
            else -> throw AppFunctionInvalidArgumentException(
                "Unknown dateFilter '$dateFilter'. Accepted values: today, tomorrow, past_due, done."
            )
        }

        val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d 'at' h:mm a", localeProvider.getLocale())
        VisitListResult(
            visits = filtered.map { visit ->
                VisitItem(
                    subject = visit.subject,
                    householderName = visit.householderName,
                    address = visit.householderAddress,
                    scheduledDate = visit.date.format(formatter),
                    visitType = visit.type.name
                )
            },
            count = filtered.size
        )
    }
}
