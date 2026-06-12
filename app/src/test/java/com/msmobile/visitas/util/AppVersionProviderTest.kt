package com.msmobile.visitas.util

import com.msmobile.visitas.BuildConfig
import junit.framework.TestCase.assertEquals
import org.junit.Test

class AppVersionProviderTest {

    @Test
    fun `getVersionName returns BuildConfig VERSION_NAME`() {
        assertEquals(BuildConfig.VERSION_NAME, AppVersionProvider.getVersionName())
    }

    @Test
    fun `getVersionCode returns BuildConfig VERSION_CODE`() {
        assertEquals(BuildConfig.VERSION_CODE, AppVersionProvider.getVersionCode())
    }
}
