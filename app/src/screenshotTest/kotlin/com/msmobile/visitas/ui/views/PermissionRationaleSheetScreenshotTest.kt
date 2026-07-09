package com.msmobile.visitas.ui.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.android.tools.screenshot.PreviewTest
import com.msmobile.visitas.ui.theme.PreviewPhone
import com.msmobile.visitas.visit.VisitDetailPreviewConfig
import com.msmobile.visitas.visit.VisitDetailPreviewConfigProvider
import com.msmobile.visitas.visit.VisitDetailScreenPreview

class PermissionRationaleSheetScreenshotTest {

    @PreviewTest
    @PreviewPhone
    @Composable
    internal fun PermissionRationaleSheetPreviewTest(@PreviewParameter(PermissionRationaleSheetPreviewConfigProvider::class) config: PermissionRationaleSheetPreviewConfig) {
        PermissionRationaleSheetPreview(config)
    }
}
