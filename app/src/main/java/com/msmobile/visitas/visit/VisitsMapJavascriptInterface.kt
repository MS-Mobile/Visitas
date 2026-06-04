package com.msmobile.visitas.visit

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import com.msmobile.visitas.ui.views.WebViewJavascriptInterface

class VisitsMapJavascriptInterface(
    private val onMapError: (String) -> Unit,
    private val onMapReady: () -> Unit = {}
) : WebViewJavascriptInterface {
    override val name: String
        get() = "Visits"

    @JavascriptInterface
    fun onMapReady() {
        Log.d("VisitsMap", "Map is ready")
        Handler(Looper.getMainLooper()).post(onMapReady)
    }

    @JavascriptInterface
    fun onMarkerClicked(marker: String) {
        Log.d("VisitsMap", "Marker clicked: $marker")
    }

    @JavascriptInterface
    fun onMapInitializationError(error: String) {
        Log.e("VisitsMap", "Map initialization error: $error")
        onMapError(error)
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
