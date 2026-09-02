package com.msmobile.visitas.ui.views

import android.graphics.Color
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import java.lang.ref.WeakReference

/**
 * Base URL for anything vendored under `app/src/main/assets/`.
 *
 * Assets are served over this synthetic HTTPS origin instead of `file:///android_asset/` because
 * ES modules — which MapLibre ships exclusively as of v6 — are blocked on a `file://` opaque
 * origin: Chromium fetches module scripts in CORS mode, which a file URL can never satisfy, and
 * the `allowFileAccessFromFileURLs` / `allowUniversalAccessFromFileURLs` settings only relax
 * XHR, not module resolution. A real origin also means no mixed content and a proper `Origin`
 * header on tile requests.
 */
const val WEB_VIEW_ASSET_BASE_URL = "https://appassets.androidplatform.net/assets/"

private const val ASSET_PATH_PREFIX = "/assets/"

@Composable
fun WebView(
    url: String,
    isJavaScriptEnabled: Boolean,
    isZoomEnabled: Boolean,
    isDomStorageEnabled: Boolean,
    javascriptInterface: WebViewJavascriptInterface? = null,
    onInitializationComplete: (WebViewViewBridge) -> Unit = {}
) {
    AndroidView(factory = { context ->
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler(ASSET_PATH_PREFIX, WebViewAssetLoader.AssetsPathHandler(context))
            .build()
        val webView = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.TRANSPARENT)
            settings.apply {
                javaScriptEnabled = isJavaScriptEnabled
                domStorageEnabled = isDomStorageEnabled
                allowContentAccess = true
                cacheMode = WebSettings.LOAD_DEFAULT
                databaseEnabled = true
                setGeolocationEnabled(true)
                mediaPlaybackRequiresUserGesture = false
                loadWithOverviewMode = true
                useWideViewPort = true

                // Pinch-to-zoom without zoom buttons
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
            }
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? =
                    assetLoader.shouldInterceptRequest(request.url)
                        ?.withJavaScriptMimeTypeIfModule(request)

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    onInitializationComplete(createWebViewBridge(view))
                }
            }
            webChromeClient = WebChromeClient()

            if (javascriptInterface != null) {
                addJavascriptInterface(javascriptInterface, javascriptInterface.name)
            }
            // Initial load
            loadUrl(url)
        }
        webView
    }, update = { webView ->
        // Avoid forcing a reload on every recomposition; only reload if changed
        if (webView.url != url) {
            webView.loadUrl(url)
        }
    })
}

/**
 * [WebViewAssetLoader] derives a MIME type from the file extension, and Android's table has no
 * entry for `.mjs`, so it falls back to `text/plain`. Chromium refuses to execute a module script
 * that is not served as a JavaScript type, which would sink both the MapLibre ESM bundle and the
 * module worker it spawns. Correcting the type here is preferable to renaming the files, whose
 * import specifiers are baked into the published bundle.
 */
private fun WebResourceResponse.withJavaScriptMimeTypeIfModule(
    request: WebResourceRequest
): WebResourceResponse = apply {
    if (request.url.path?.endsWith(".mjs") == true) {
        mimeType = "text/javascript"
    }
}

interface WebViewViewBridge {
    fun executeScript(script: String, callback: ValueCallback<String>)
    fun loadUrl(url: String)
}

interface WebViewJavascriptInterface {
    val name: String
}

private fun createWebViewBridge(webView: WebView): WebViewViewBridge {
    return object : WebViewViewBridge {
        private val webViewRef = WeakReference(webView)

        override fun executeScript(script: String, callback: ValueCallback<String>) {
            webViewRef.get()?.evaluateJavascript(script, callback)
        }

        override fun loadUrl(url: String) {
            webViewRef.get()?.loadUrl(url)
        }
    }
}
