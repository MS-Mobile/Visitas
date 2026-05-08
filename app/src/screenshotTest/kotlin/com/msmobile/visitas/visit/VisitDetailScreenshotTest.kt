package com.msmobile.visitas.visit

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.android.tools.screenshot.PreviewTest
import com.msmobile.visitas.ui.theme.PreviewPhone

class VisitDetailScreenshotTest {

    @PreviewTest
    @PreviewPhone
    @Composable
    internal fun VisitDetailScreenPreviewTest(
        @PreviewParameter(VisitDetailPreviewConfigProvider::class) config: VisitDetailPreviewConfig
    ) {
        VisitDetailScreenPreview(config)
    }
}
