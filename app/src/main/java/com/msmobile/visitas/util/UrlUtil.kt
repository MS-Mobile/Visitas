package com.msmobile.visitas.util

import android.webkit.URLUtil

object UrlUtil {
    fun isValidUrl(url: String): Boolean {
        return URLUtil.isValidUrl(url)
    }
}
