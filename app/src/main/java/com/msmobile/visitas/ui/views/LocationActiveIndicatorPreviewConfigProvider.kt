package com.msmobile.visitas.ui.views

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.tooling.preview.PreviewParameterProvider

@VisibleForTesting
internal class LocationActiveIndicatorPreviewConfigProvider : PreviewParameterProvider<LocationActiveIndicatorPreviewConfig> {

    private val previewConfigLight = sequenceOf(
        LocationActiveIndicatorPreviewConfig(
            configName = "Expanded",
            isExpanded = true,
            isDarkMode = false,
        ),
        LocationActiveIndicatorPreviewConfig(
            configName = "Collapsed",
            isExpanded = false,
            isDarkMode = false,
        ),
    )

    private val previewConfigDark = previewConfigLight.map { config ->
        config.copy(
            configName = "${config.configName} - Dark Mode",
            isDarkMode = true,
        )
    }

    override val values: Sequence<LocationActiveIndicatorPreviewConfig> = previewConfigLight + previewConfigDark

    override fun getDisplayName(index: Int): String {
        return values.elementAt(index).configName
    }
}

@VisibleForTesting
internal data class LocationActiveIndicatorPreviewConfig(
    val configName: String,
    val isExpanded: Boolean,
    val isDarkMode: Boolean,
)
