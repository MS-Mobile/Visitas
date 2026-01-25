package com.msmobile.visitas.util

interface Logger {
    fun error(tag: String, message: String, throwable: Throwable? = null)
}

object DefaultLogger : Logger {
    override fun error(tag: String, message: String, throwable: Throwable?) {
        android.util.Log.e(tag, message, throwable)
    }
}