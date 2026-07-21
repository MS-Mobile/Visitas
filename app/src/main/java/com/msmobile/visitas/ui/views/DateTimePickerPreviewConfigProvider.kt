package com.msmobile.visitas.ui.views

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import java.time.LocalDateTime

@VisibleForTesting
internal class DateTimePickerPreviewConfigProvider : PreviewParameterProvider<DateTimePickerPreviewConfig> {

    private val previewConfigLight = sequenceOf(
        DateTimePickerPreviewConfig(
            configName = "Date Tab",
            selectedTabIndex = 0,
            isDarkMode = false,
            initialSelectedDateMillis = 1705312800000,
            initialHour = 10,
            initialMinute = 12,
            is24Hour = true,
            now = LocalDateTime.now()
        ),
        DateTimePickerPreviewConfig(
            configName = "Time Tab",
            selectedTabIndex = 1,
            isDarkMode = false,
            initialSelectedDateMillis = 1705312800000,
            initialHour = 10,
            initialMinute = 12,
            is24Hour = true,
            now = LocalDateTime.now()
        )
    )

    private val previewConfigDark = previewConfigLight.map { config ->
        config.copy(
            configName = "${config.configName} - Dark Mode",
            isDarkMode = true
        )
    }

    override val values: Sequence<DateTimePickerPreviewConfig> = previewConfigLight + previewConfigDark

    override fun getDisplayName(index: Int): String {
        return values.elementAt(index).configName
    }
}

@VisibleForTesting
internal data class DateTimePickerPreviewConfig(
    val configName: String,
    val selectedTabIndex: Int,
    val isDarkMode: Boolean,
    val initialSelectedDateMillis: Long,
    val initialHour: Int,
    val initialMinute: Int,
    val is24Hour: Boolean,
    val now: LocalDateTime
)

