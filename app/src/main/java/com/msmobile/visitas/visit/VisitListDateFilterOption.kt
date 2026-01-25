package com.msmobile.visitas.visit

import androidx.annotation.Keep
import com.msmobile.visitas.R
import com.msmobile.visitas.util.StringResource

@Keep
enum class VisitListDateFilterOption(val description: StringResource) {
    All(description = StringResource(R.string.all_visits)),
    PastDue(description = StringResource(R.string.past_due_visits)),
    ScheduledForToday(description = StringResource(R.string.visits_scheduled_for_today)),
    ScheduledForTomorrow(description = StringResource(R.string.visits_scheduled_for_tomorrow)),
    ScheduledForNextDays(description = StringResource(R.string.visits_scheduled_for_next_days)),
    Done(description = StringResource(R.string.done_visits)),
}