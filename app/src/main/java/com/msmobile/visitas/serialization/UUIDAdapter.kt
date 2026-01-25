package com.msmobile.visitas.serialization

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import java.util.UUID

class UUIDAdapter : JsonAdapter<UUID>() {
    @FromJson
    override fun fromJson(reader: JsonReader): UUID? {
        val value = reader.readJsonValue() as? String
        return value?.let { UUID.fromString(it) }
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: UUID?) {
        writer.jsonValue(value?.toString())
    }
}