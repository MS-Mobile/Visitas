package com.msmobile.visitas

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.sentry.android.core.SentryAndroid

@HiltAndroidApp
class VisitasApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initSentry()
    }

    private fun initSentry() {
        val dsn = BuildConfig.SENTRY_DSN
        if (dsn.isBlank()) return

        SentryAndroid.init(this) { options ->
            options.dsn = dsn
            options.environment = BuildConfig.BUILD_TYPE
            options.tracesSampleRate = if (BuildConfig.DEBUG) 1.0 else 0.2
            options.profileSessionSampleRate = if (BuildConfig.DEBUG) 1.0 else 0.1
            options.sessionReplay.sessionSampleRate = 0.1
            options.sessionReplay.onErrorSampleRate = 1.0
            options.logs.isEnabled = true
            options.isAttachScreenshot = true
            options.isAttachViewHierarchy = true
            options.isDebug = BuildConfig.DEBUG
        }
    }
}

