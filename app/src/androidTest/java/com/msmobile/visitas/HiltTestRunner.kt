package com.msmobile.visitas

import dagger.hilt.android.testing.HiltTestApplication

class HiltTestRunner : androidx.test.runner.AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        name: String?,
        context: android.content.Context?
    ): android.app.Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
