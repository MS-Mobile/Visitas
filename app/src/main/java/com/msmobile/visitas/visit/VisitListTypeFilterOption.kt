package com.msmobile.visitas.visit

import androidx.annotation.Keep
import com.msmobile.visitas.R
import com.msmobile.visitas.util.StringResource

@Keep
enum class VisitListTypeFilterOption(val description: StringResource) {
    All(description = StringResource(R.string.all_visits)),
    FirstVisit(description = StringResource(R.string.first_visit)),
    ReturnVisit(description = StringResource(R.string.return_visit)),
    BibleStudy(description = StringResource(R.string.bible_study)),
}
