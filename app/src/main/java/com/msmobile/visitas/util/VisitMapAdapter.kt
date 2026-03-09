package com.msmobile.visitas.util

import com.msmobile.visitas.visit.VisitMapData
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi

class VisitMapAdapter(private val moshi: Moshi) {
    private val adapter: JsonAdapter<List<VisitMapData>?> by lazy(::createAdapter)

    fun toJson(data: List<VisitMapData>): String? {
        return adapter.toJson(data)
    }

    private fun createAdapter(): JsonAdapter<List<VisitMapData>?> {
        return moshi.adapter(
            com.squareup.moshi.Types.newParameterizedType(
                List::class.java,
                VisitMapData::class.java
            )
        )
    }
}