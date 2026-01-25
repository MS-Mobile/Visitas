package com.msmobile.visitas.ui.views

import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import java.lang.ref.WeakReference

@Composable
fun WebView(
    url: String,
    isJavaScriptEnabled: Boolean,
    isZoomEnabled: Boolean,
    isDomStorageEnabled: Boolean,
    isFileAccessAllowed: Boolean,
    javascriptInterface: WebViewJavascriptInterface? = null,
    onInitializationComplete: (WebViewViewBridge) -> Unit = {}
) {
    AndroidView(factory = { context ->
        val webView = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                javaScriptEnabled = isJavaScriptEnabled
                domStorageEnabled = isDomStorageEnabled
                allowFileAccess = isFileAccessAllowed
                allowContentAccess = true
                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
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