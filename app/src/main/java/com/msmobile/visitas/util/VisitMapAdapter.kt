package com.msmobile.visitas.util

import com.msmobile.visitas.visit.VisitMapData
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi

class VisitMapAdapter(private val moshi: Moshi) : JsonAdapter<List<VisitMapData>?>() {
    private val adapter: JsonAdapter<List<VisitMapData>?> by lazy(::createAdapter)

    override fun fromJson(p0: JsonReader?): List<VisitMapData>? {
        return adapter.fromJson(p0)
    }

    override fun toJson(
        p0: JsonWriter?,
        p1: List<VisitMapData>?
    ) {
        return adapter.toJson(p0, p1)
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