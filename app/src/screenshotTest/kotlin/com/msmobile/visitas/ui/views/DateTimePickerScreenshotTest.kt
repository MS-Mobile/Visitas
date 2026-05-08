package com.msmobile.visitas.ui.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.android.tools.screenshot.PreviewTest
import com.msmobile.visitas.ui.theme.PreviewPhone

class DateTimePickerScreenshotTest {

    @PreviewTest
    @PreviewPhone
    @Composable
    internal fun DateTimePickerPreviewTest(
        @PreviewParameter(DateTimePickerPreviewConfigProvider::class) config: DateTimePickerPreviewConfig
    ) {
        DateTimePickerPreview(config)
    }
}
