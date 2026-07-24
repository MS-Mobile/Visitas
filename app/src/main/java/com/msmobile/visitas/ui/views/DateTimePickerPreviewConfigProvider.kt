package com.msmobile.visitas.ui.views

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import java.time.LocalDateTime

@VisibleForTesting
internal class DateTimePickerPreviewConfigProvider : PreviewParameterProvider<DateTimePickerPreviewConfig> {

    // Stand-in for the account's synced event color palette (CalendarContract.Colors).
    private val availableColors = listOf(
        Color(0xFFD50000).toArgb(),
        Color(0xFFF4511E).toArgb(),
        Color(0xFFF6BF26).toArgb(),
        Color(0xFF33B679).toArgb(),
        Color(0xFF039BE5).toArgb(),
        Color(0xFF7986CB).toArgb()
    )

    private val previewConfigLight = sequenceOf(
        DateTimePickerPreviewConfig(
            configName = "Date Tab",
            selectedTabIndex = 0,
            isDarkMode = false,
            initialSelectedDateMillis = 1705312800000,
            initialHour = 10,
            initialMinute = 12,
            is24Hour = true,
            now = LocalDateTime.now(),
            addToCalendarState = AddToCalendarState.Visible(
                checked = false,
                selectedColor = -1,
                availableColors = availableColors
            )
        ),
        DateTimePickerPreviewConfig(
            configName = "Time Tab",
            selectedTabIndex = 1,
            isDarkMode = false,
            initialSelectedDateMillis = 1705312800000,
            initialHour = 10,
            initialMinute = 12,
            is24Hour = true,
            now = LocalDateTime.now(),
            addToCalendarState = AddToCalendarState.Visible(
                checked = false,
                selectedColor = -1,
                availableColors = availableColors
            )
        ),
        DateTimePickerPreviewConfig(
            configName = "Date Tab - Add to Calendar Checked",
            selectedTabIndex = 0,
            isDarkMode = false,
            initialSelectedDateMillis = 1705312800000,
            initialHour = 10,
            initialMinute = 12,
            is24Hour = true,
            now = LocalDateTime.now(),
            addToCalendarState = AddToCalendarState.Visible(
                checked = true,
                selectedColor = Color(0xFF33B679).toArgb(),
                availableColors = availableColors
            )
        ),
        DateTimePickerPreviewConfig(
            configName = "Time Tab - Add to Calendar Checked",
            selectedTabIndex = 1,
            isDarkMode = false,
            initialSelectedDateMillis = 1705312800000,
            initialHour = 10,
            initialMinute = 12,
            is24Hour = true,
            now = LocalDateTime.now(),
            addToCalendarState = AddToCalendarState.Visible(
                checked = true,
                selectedColor = Color(0xFF039BE5).toArgb(),
                availableColors = availableColors
            )
        ),
        // The color palette open state is exercised on the (shorter) time tab: on the date tab the
        // tall calendar leaves no room for the palette without visibly compressing the month grid.
        DateTimePickerPreviewConfig(
            configName = "Time Tab - Color Picker Open",
            selectedTabIndex = 1,
            isDarkMode = false,
            initialSelectedDateMillis = 1705312800000,
            initialHour = 10,
            initialMinute = 12,
            is24Hour = true,
            now = LocalDateTime.now(),
            addToCalendarState = AddToCalendarState.Visible(
                checked = true,
                selectedColor = Color(0xFF039BE5).toArgb(),
                availableColors = availableColors
            ),
            colorPickerExpanded = true
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
    val addToCalendarState: AddToCalendarState,
    val colorPickerExpanded: Boolean = false
)
