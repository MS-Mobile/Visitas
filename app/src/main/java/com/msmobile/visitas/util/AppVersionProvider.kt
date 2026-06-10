package com.msmobile.visitas.util

import com.msmobile.visitas.BuildConfig

object AppVersionProvider {
    fun getVersion(): String {
        return "${BuildConfig.VERSION_NAME}#${BuildConfig.VERSION_CODE}"
    }
}