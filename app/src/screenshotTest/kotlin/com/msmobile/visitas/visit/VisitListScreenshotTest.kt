package com.msmobile.visitas.visit

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.android.tools.screenshot.PreviewTest
import com.msmobile.visitas.ui.theme.PreviewPhone

class VisitListScreenshotTest {

    @PreviewTest
    @PreviewPhone
    @Composable
    internal fun VisitListScreenPreviewTest(
        @PreviewParameter(VisitListPreviewConfigProvider::class) config: VisitListPreviewConfig
    ) {
        VisitListScreenPreview(config)
    }

    @PreviewTest
    @PreviewPhone
    @Composable
    internal fun VisitListFilterDropdownContentPreviewTest() {
        VisitListFilterDropdownContentPreview()
    }
}
