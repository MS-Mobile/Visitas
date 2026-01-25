package com.msmobile.visitas.serialization

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class LocalDateTimeAdapter : JsonAdapter<LocalDateTime>() {
    @FromJson
    override fun fromJson(reader: JsonReader): LocalDateTime? {
        val value = reader.readJsonValue() as? String
        return value?.let {
            ZonedDateTime.parse(value)
                .truncatedTo(ChronoUnit.MINUTES)
                .toLocalDateTime()
        }
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: LocalDateTime?) {
        writer.jsonValue(value?.toString())
    }
}