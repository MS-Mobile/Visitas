package com.msmobile.visitas.visit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.msmobile.visitas.R
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
        isFileAccessAllowed = true,
        onInitializationComplete = { webViewBridge ->
            webViewBridgeState.value = webViewBridge
            val initScript = "initializeMap('${currentLocationText}');"
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

private fun assetPath(engine: VisitMapEngineOption) = when (engine) {
    VisitMapEngineOption.MapLibre -> "file:///android_asset/map/maplibre/visits-map.html"
    VisitMapEngineOption.Leaflet -> "file:///android_asset/map/leaflet/visits-map.html"
}
