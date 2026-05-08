package com.msmobile.visitas.conversation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.android.tools.screenshot.PreviewTest
import com.msmobile.visitas.ui.theme.PreviewPhone

class ConversationDetailScreenshotTest {

    @PreviewTest
    @PreviewPhone
    @Composable
    internal fun ConversationDetailScreenPreviewTest(
        @PreviewParameter(ConversationDetailPreviewConfigProvider::class) config: ConversationDetailPreviewConfig
    ) {
        ConversationDetailScreenPreview(config)
    }
}
