package com.msmobile.visitas.visit

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.msmobile.visitas.R
import com.msmobile.visitas.ui.views.WEB_VIEW_ASSET_BASE_URL
import com.msmobile.visitas.ui.views.WebView
import com.msmobile.visitas.ui.views.WebViewViewBridge
@Composable
fun VisitsMap(
    currentLocation: Pair<Double, Double>,
    visitMapState: VisitMapState.Visits,
    engine: VisitMapEngineOption,
    onMapError: (String) -> Unit,
    onMapReady: () -> Unit = {}
) {
    val currentLocationText = stringResource(R.string.current_location).replace("'", "\\'")
    val isDarkTheme = isSystemInDarkTheme()
    val webViewBridgeState = remember(engine) { mutableStateOf<WebViewViewBridge?>(null) }

    val (currentLatitude, currentLongitude) = currentLocation

    LaunchedEffect(visitMapState.serialized, currentLatitude, currentLongitude) {
        webViewBridgeState.value?.let { bridge ->
            val visitsJson = visitMapState.serialized
            bridge.executeScript("setMarkers($currentLatitude, $currentLongitude, $visitsJson);") { }
        }
    }

    WebView(
        url = assetPath(engine),
        javascriptInterface = VisitsMapJavascriptInterface(
            onMapError = onMapError,
            onMapReady = onMapReady
        ),
        isJavaScriptEnabled = true,
        isZoomEnabled = true,
        isDomStorageEnabled = true,
        onInitializationComplete = { webViewBridge ->
            webViewBridgeState.value = webViewBridge
            val initScript = "initializeMap('${currentLocationText}', $isDarkTheme);"
            webViewBridge.executeScript(initScript) { _ ->
                val visitsJson = visitMapState.serialized
                webViewBridge.executeScript("setMarkers($currentLatitude, $currentLongitude, $visitsJson);") { }
            }
        }
    )
}

sealed class VisitsMapEvent {
    data class ErrorLoadingMap(val errorMessage: String) : VisitsMapEvent()
}

/**
 * Both engines are vendored under `assets/map/<engine>/` and are interchangeable: each implements
 * the same bridge in both directions — Kotlin calls `initializeMap` and `setMarkers(lat, lng,
 * visitsJson)`, JS calls back on `window.Visits`. A change to either side of that contract has to
 * land in both HTML files, or switching engines in Settings breaks the map.
 *
 * They differ in coordinate order (Leaflet takes `[lat, lng]`, MapLibre GeoJSON `[lng, lat]`); each
 * file documents its own conversion.
 */
private fun assetPath(engine: VisitMapEngineOption) = when (engine) {
    VisitMapEngineOption.MapLibre -> "${WEB_VIEW_ASSET_BASE_URL}map/maplibre/visits-map.html"
    VisitMapEngineOption.Leaflet -> "${WEB_VIEW_ASSET_BASE_URL}map/leaflet/visits-map.html"
}
