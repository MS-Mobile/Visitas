package com.msmobile.visitas

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.android.tools.screenshot.PreviewTest
import com.msmobile.visitas.ui.theme.PreviewPhone

class AppScaffoldScreenshotTest {

    @PreviewTest
    @PreviewPhone
    @Composable
    internal fun AppScaffoldPreviewTest(
        @PreviewParameter(AppScaffoldPreviewConfigProvider::class) config: AppScaffoldPreviewConfig
    ) {
        AppScaffoldPreview(config)
    }
}
