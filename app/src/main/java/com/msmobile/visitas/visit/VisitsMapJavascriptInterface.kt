package com.msmobile.visitas.visit

import android.util.Log
import android.webkit.JavascriptInterface
import com.msmobile.visitas.ui.views.WebViewJavascriptInterface

class VisitsMapJavascriptInterface(
    private val onMapEvent: (VisitsMapEvent) -> Unit
) : WebViewJavascriptInterface {
    override val name: String
        get() = "Visits"

    @JavascriptInterface
    fun onMapReady() {
        Log.d("VisitsMap", "Map is ready")
    }

    @JavascriptInterface
    fun onMarkerClicked(marker: String) {
        Log.d("VisitsMap", "Marker clicked: $marker")
    }

    @JavascriptInterface
    fun onMapInitializationError(error: String) {
        Log.e("VisitsMap", "Map initialization error: $error")
        onMapEvent(VisitsMapEvent.ErrorLoadingMap(error))
    }

    @JavascriptInterface
    fun log(message: String) {
        Log.d("VisitsMapJS", message)
    }

    @JavascriptInterface
    fun error(message: String) {
        Log.e("VisitsMapJS", message)
    }
}
