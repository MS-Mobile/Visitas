package com.msmobile.visitas.ui.views

import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.msmobile.visitas.R

@VisibleForTesting
internal class PermissionRationaleSheetPreviewConfigProvider : PreviewParameterProvider<PermissionRationaleSheetPreviewConfig> {

    private val previewConfigLight = sequenceOf(
        PermissionRationaleSheetPreviewConfig(
            configName = "Location",
            icon = Icons.Rounded.LocationOn,
            messageRes = R.string.location_permission_message,
            isDarkMode = false,
        ),
        PermissionRationaleSheetPreviewConfig(
            configName = "Calendar",
            icon = Icons.Rounded.DateRange,
            messageRes = R.string.calendar_permission_message,
            isDarkMode = false,
        ),
    )

    private val previewConfigDark = previewConfigLight.map { config ->
        config.copy(
            configName = "${config.configName} - Dark Mode",
            isDarkMode = true,
        )
    }

    override val values: Sequence<PermissionRationaleSheetPreviewConfig> = previewConfigLight + previewConfigDark

    override fun getDisplayName(index: Int): String {
        return values.elementAt(index).configName
    }
}

@VisibleForTesting
internal data class PermissionRationaleSheetPreviewConfig(
    val configName: String,
    val icon: ImageVector,
    @param:StringRes val messageRes: Int,
    val isDarkMode: Boolean,
)
