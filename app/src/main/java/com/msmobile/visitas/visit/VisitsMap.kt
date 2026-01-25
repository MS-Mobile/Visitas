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
    onMapEvent: (VisitsMapEvent) -> Unit
) {
    val currentLocationText = stringResource(R.string.current_location).replace("'", "\\'")
    // Hold the bridge so we can push JS updates when visitMapState changes
    val webViewBridgeState = remember { mutableStateOf<WebViewViewBridge?>(null) }

    val (currentLatitude, currentLongitude) = currentLocation

    // Push updated markers whenever the serialized visits change (after initialization)
    LaunchedEffect(visitMapState.serialized, currentLatitude, currentLongitude) {
        webViewBridgeState.value?.let { bridge ->
            val visitsJson = visitMapState.serialized
            // Updated signature: setMarkers(latitude, longitude, visits)
            bridge.executeScript("setMarkers($currentLatitude, $currentLongitude, $visitsJson);") { }
        }
    }

    WebView(
        url = VISITS_MAP_HTML_ASSET_PATH,
        javascriptInterface = VisitsMapJavascriptInterface(onMapEvent),
        isJavaScriptEnabled = true,
        isZoomEnabled = true,
        isDomStorageEnabled = true,
        isFileAccessAllowed = true,
        onInitializationComplete = { webViewBridge ->
            webViewBridgeState.value = webViewBridge
            // Updated signature: initializeMap(currentLocationText)
            val initScript = "initializeMap('${currentLocationText}');"
            webViewBridge.executeScript(initScript) { _ ->
                // Initial marker load after map is initialized
                val visitsJson = visitMapState.serialized
                webViewBridge.executeScript("setMarkers($currentLatitude, $currentLongitude, $visitsJson);") { }
            }
        }
    )
}

sealed class VisitsMapEvent {
    data class ErrorLoadingMap(val errorMessage: String) : VisitsMapEvent()
}

private const val VISITS_MAP_HTML_ASSET_PATH = "file:///android_asset/visits-map.html"