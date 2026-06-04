package com.msmobile.visitas.visit

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.msmobile.visitas.R
import com.msmobile.visitas.ui.views.WebView
import com.msmobile.visitas.ui.views.WebViewViewBridge
import kotlin.math.roundToInt

@Composable
fun VisitsMap(
    currentLocation: Pair<Double, Double>,
    visitMapState: VisitMapState.Visits,
    engine: VisitMapEngineOption,
    onMapEvent: (VisitsMapEvent) -> Unit
) {
    val currentLocationText = stringResource(R.string.current_location).replace("'", "\\'")
    val webViewBridgeState = remember { mutableStateOf<WebViewViewBridge?>(null) }
    val darkLandColor = darkLandColor(engine)

    val (currentLatitude, currentLongitude) = currentLocation

    LaunchedEffect(visitMapState.serialized, currentLatitude, currentLongitude) {
        webViewBridgeState.value?.let { bridge ->
            val visitsJson = visitMapState.serialized
            bridge.executeScript("setMarkers($currentLatitude, $currentLongitude, $visitsJson);") { }
        }
    }

    WebView(
        url = assetPath(engine),
        javascriptInterface = VisitsMapJavascriptInterface(onMapEvent),
        isJavaScriptEnabled = true,
        isZoomEnabled = true,
        isDomStorageEnabled = true,
        isFileAccessAllowed = true,
        onInitializationComplete = { webViewBridge ->
            webViewBridgeState.value = webViewBridge
            val initScript = if (darkLandColor != null)
                "initializeMap('${currentLocationText}', '${darkLandColor}');"
            else
                "initializeMap('${currentLocationText}');"
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

@Composable
private fun darkLandColor(engine: VisitMapEngineOption): String? {
    if (engine != VisitMapEngineOption.MapLibre || !isSystemInDarkTheme()) return null
    val c = MaterialTheme.colorScheme.surfaceContainerHigh
    return "#%02x%02x%02x".format(
        (c.red * 255).roundToInt().coerceIn(0, 255),
        (c.green * 255).roundToInt().coerceIn(0, 255),
        (c.blue * 255).roundToInt().coerceIn(0, 255)
    )
}

private fun assetPath(engine: VisitMapEngineOption) = when (engine) {
    VisitMapEngineOption.MapLibre -> "file:///android_asset/map/maplibre/visits-map.html"
    VisitMapEngineOption.Leaflet -> "file:///android_asset/map/leaflet/visits-map.html"
}
