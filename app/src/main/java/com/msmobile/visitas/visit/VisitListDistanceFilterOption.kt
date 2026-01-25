package com.msmobile.visitas.visit

import androidx.annotation.Keep
import com.msmobile.visitas.R
import com.msmobile.visitas.util.StringResource

@Keep
enum class VisitListDistanceFilterOption(val description: StringResource) {
    All(description = StringResource(R.string.all_visits)),
    Nearby(description = StringResource(R.string.nearby_visits)),
}