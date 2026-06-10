package com.msmobile.visitas.util

import com.msmobile.visitas.BuildConfig

object AppVersionProvider {
    fun getVersionName(): String {
        return BuildConfig.VERSION_NAME
    }
}