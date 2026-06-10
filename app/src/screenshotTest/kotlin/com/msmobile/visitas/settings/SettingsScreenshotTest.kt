package com.msmobile.visitas.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.android.tools.screenshot.PreviewTest
import com.msmobile.visitas.ui.theme.PreviewPhone

class SettingsScreenshotTest {

    @PreviewTest
    @PreviewPhone
    @Composable
    internal fun SettingsScreenPreviewTest(
        @PreviewParameter(SettingsPreviewConfigProvider::class) config: SettingsPreviewConfig
    ) {
        SettingsScreenPreview(config)
    }
}
