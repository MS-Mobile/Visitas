package com.msmobile.visitas.serialization

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.rawType
import java.lang.reflect.Type

object SerializationFactory : JsonAdapter.Factory {
    override fun create(
        type: Type,
        annotations: MutableSet<out Annotation>,
        moshi: Moshi
    ): JsonAdapter<*>? {
        val serializeNull = Types.nextAnnotations(annotations, SerializeNull::class.java)
        return if (serializeNull == null) {
            null
        } else {
            moshi.adapter(type.rawType).serializeNulls()
        }
    }
}
