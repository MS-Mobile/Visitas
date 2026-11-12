package com.msmobile.visitas.visit

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalDensity
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

    // The map is drawn behind the system bars, so the page is told how tall they are. dp and CSS
    // px are the same unit here because the page's viewport is `width=device-width`.
    val density = LocalDensity.current
    val safeDrawing = WindowInsets.safeDrawing
    val safeAreaScript = with(density) {
        val top = safeDrawing.getTop(this).toDp().value
        val bottom = safeDrawing.getBottom(this).toDp().value
        "setSafeAreaInsets($top, $bottom);"
    }
    // The WebView is created once, so its page-finished callback must not capture a stale script.
    val currentSafeAreaScript by rememberUpdatedState(safeAreaScript)

    LaunchedEffect(safeAreaScript) {
        webViewBridgeState.value?.executeScript(safeAreaScript) { }
    }

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
            val initScript =
                "$currentSafeAreaScript initializeMap('${currentLocationText}', $isDarkTheme);"
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
 * the same bridge in both directions — Kotlin calls `setSafeAreaInsets(top, bottom)`,
 * `initializeMap` and `setMarkers(lat, lng, visitsJson)`, JS calls back on `window.Visits`. A
 * change to either side of that contract has to land in both HTML files, or switching engines in
 * Settings breaks the map.
 *
 * They differ in coordinate order (Leaflet takes `[lat, lng]`, MapLibre GeoJSON `[lng, lat]`); each
 * file documents its own conversion.
 */
private fun assetPath(engine: VisitMapEngineOption) = when (engine) {
    VisitMapEngineOption.MapLibre -> "${WEB_VIEW_ASSET_BASE_URL}map/maplibre/visits-map.html"
    VisitMapEngineOption.Leaflet -> "${WEB_VIEW_ASSET_BASE_URL}map/leaflet/visits-map.html"
}
