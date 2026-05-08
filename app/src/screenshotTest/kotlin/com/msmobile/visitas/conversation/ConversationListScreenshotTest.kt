package com.msmobile.visitas.conversation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.android.tools.screenshot.PreviewTest
import com.msmobile.visitas.ui.theme.PreviewPhone

class ConversationListScreenshotTest {
    @PreviewTest
    @PreviewPhone
    @Composable
    internal fun ConversationListScreenPreviewTest(
        @PreviewParameter(PreviewConfigProvider::class) config: PreviewConfig
    ) {
        ConversationListScreenPreview(config)
    }
}