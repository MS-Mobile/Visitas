package com.msmobile.visitas.util

import com.msmobile.visitas.BuildConfig

object AppVersionProvider {
    fun getVersionName(): String = BuildConfig.VERSION_NAME
    fun getVersionCode(): Int = BuildConfig.VERSION_CODE
}