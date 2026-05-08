package com.msmobile.visitas.backup

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.android.tools.screenshot.PreviewTest
import com.msmobile.visitas.ui.theme.PreviewPhone

class BackupSheetScreenshotTest {

    @PreviewTest
    @PreviewPhone
    @Composable
    internal fun BackupSheetPreviewTest(
        @PreviewParameter(BackupSheetPreviewConfigProvider::class) config: BackupSheetPreviewConfig
    ) {
        BackupScreenPreview(config)
    }
}
