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
        ),
        DateTimePickerPreviewConfig(
            configName = "Calendar Colors",
            selectedTabIndex = 0,
            isDarkMode = false,
            initialSelectedDateMillis = 1705312800000,
            initialHour = 10,
            initialMinute = 12,
            is24Hour = true,
            now = LocalDateTime.now(),
            addToCalendarState = AddToCalendarState.EventColors(
                colors = previewCalendarColors,
                selectedKey = "2"
            )
        ),
        DateTimePickerPreviewConfig(
            configName = "No Event Colors",
            selectedTabIndex = 0,
            isDarkMode = false,
            initialSelectedDateMillis = 1705312800000,
            initialHour = 10,
            initialMinute = 12,
            is24Hour = true,
            now = LocalDateTime.now(),
            addToCalendarState = AddToCalendarState.NoEventColors
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
    val now: LocalDateTime,
    val addToCalendarState: AddToCalendarState = AddToCalendarState.PermissionMissing
)

// Google Calendar's synced event palette, as exposed through
// CalendarContract.Colors for a Google account.
private val previewCalendarColors = listOf(
    CalendarColor(key = "1", argb = 0xFF7986CB.toInt()),
    CalendarColor(key = "2", argb = 0xFF33B679.toInt()),
    CalendarColor(key = "3", argb = 0xFF8E24AA.toInt()),
    CalendarColor(key = "4", argb = 0xFFE67C73.toInt()),
    CalendarColor(key = "5", argb = 0xFFF6BF26.toInt()),
    CalendarColor(key = "6", argb = 0xFFF4511E.toInt()),
    CalendarColor(key = "7", argb = 0xFF039BE5.toInt()),
    CalendarColor(key = "8", argb = 0xFF616161.toInt()),
    CalendarColor(key = "9", argb = 0xFF3F51B5.toInt()),
    CalendarColor(key = "10", argb = 0xFF0B8043.toInt()),
    CalendarColor(key = "11", argb = 0xFFD50000.toInt())
)
