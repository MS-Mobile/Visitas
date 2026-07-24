package com.msmobile.visitas.ui.views

import androidx.annotation.ColorInt
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.getSelectedDate
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.msmobile.visitas.R
import com.msmobile.visitas.extension.toComposeColor
import com.msmobile.visitas.ui.theme.PreviewPhone
import com.msmobile.visitas.ui.theme.VisitasTheme
import com.msmobile.visitas.util.borderPadding
import com.msmobile.visitas.util.cardInnerPadding
import com.msmobile.visitas.util.horizontalFieldPadding
import com.msmobile.visitas.util.verticalFieldPadding
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

private const val DATE_PICKER_TAB = 0
private const val TIME_PICKER_TAB = 1

// Side of the square color indicator / palette swatches.
private val COLOR_SWATCH_SIZE = 28.dp

// Opacity applied to the color indicator when the "Add to calendar" checkbox is unchecked, so it
// reads as disabled. Matches Material's default disabled content alpha.
private const val DISABLED_ALPHA = 0.38f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePicker(
    dateTime: LocalDateTime,
    addToCalendarState: AddToCalendarState,
    onAddToCalendarChecked: (Boolean) -> Unit,
    onAddToCalendarColorSelected: (Int) -> Unit,
    onDateSelected: (LocalDateTime) -> Unit,
    onDismiss: () -> Unit,
    now: LocalDateTime = LocalDateTime.now()
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedDate by remember { mutableStateOf(dateTime) }
    var colorPickerExpanded by remember { mutableStateOf(false) }
    lateinit var datePickerState: DatePickerState
    lateinit var timePickerState: TimePickerState

    // Set the date and time picker states based on the selected date
    key(selectedDate) {
        datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atZone(ZoneId.of("UTC")).toInstant()
                .toEpochMilli()
        )
        timePickerState = rememberTimePickerState(
            initialHour = selectedDate.hour,
            initialMinute = selectedDate.minute,
            is24Hour = true
        )
    }
    // Update the selected date when the date picker state changes
    key(datePickerState.selectedDateMillis) {
        selectedDate = datePickerState.getSelectedDate()?.atTime(
            selectedDate.hour,
            selectedDate.minute
        ) ?: now
    }
    // Update the selected date when the time picker state changes
    key(timePickerState.hour, timePickerState.minute) {
        selectedDate = LocalDateTime.of(
            selectedDate.year,
            selectedDate.month,
            selectedDate.dayOfMonth,
            timePickerState.hour,
            timePickerState.minute
        )
    }

    val onConfirm = {
        val selectedDateMillis = datePickerState.selectedDateMillis
        if (selectedDateMillis != null) {
            val selectedDateOnly = Instant
                .ofEpochMilli(selectedDateMillis)
                .atZone(ZoneId.of("UTC"))
                .toLocalDate()
            val selectedDateTime = LocalDateTime.of(
                selectedDateOnly.year,
                selectedDateOnly.month,
                selectedDateOnly.dayOfMonth,
                timePickerState.hour,
                timePickerState.minute
            )
            onDateSelected(selectedDateTime)
        }
    }

    val onDatePresetSelected = { presetDate: LocalDate ->
        selectedDate = presetDate.atTime(
            selectedDate.hour,
            selectedDate.minute
        )
    }

    val onTimePresetSelected = { presetTime: LocalDateTime ->
        selectedDate = presetTime
    }

    val onTabSelected = { tabIndex: Int ->
        selectedTabIndex = tabIndex
    }
    DateTimePickerContent(
        now = now,
        selectedTabIndex = selectedTabIndex,
        datePickerState = datePickerState,
        timePickerState = timePickerState,
        onAddToCalendarChecked = onAddToCalendarChecked,
        onAddToCalendarColorSelected = { color ->
            colorPickerExpanded = false
            onAddToCalendarColorSelected(color)
        },
        colorPickerExpanded = colorPickerExpanded,
        onColorPickerExpandedChange = { colorPickerExpanded = it },
        onTabSelected = onTabSelected,
        onDatePresetSelected = onDatePresetSelected,
        onTimePresetSelected = onTimePresetSelected,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        addToCalendarState = addToCalendarState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerContent(
    now: LocalDateTime,
    selectedTabIndex: Int,
    datePickerState: DatePickerState,
    timePickerState: TimePickerState,
    addToCalendarState: AddToCalendarState,
    onAddToCalendarChecked: (Boolean) -> Unit,
    onAddToCalendarColorSelected: (Int) -> Unit,
    colorPickerExpanded: Boolean,
    onColorPickerExpandedChange: (Boolean) -> Unit,
    onTabSelected: (Int) -> Unit,
    onDatePresetSelected: (LocalDate) -> Unit,
    onTimePresetSelected: (LocalDateTime) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(horizontalFieldPadding)
            ) {
                if (selectedTabIndex == DATE_PICKER_TAB) {
                    SelectTodayButton(
                        today = now.toLocalDate(),
                        onPresetSelected = onDatePresetSelected
                    )
                } else {
                    SelectNowButton(now = now, onPresetSelected = onTimePresetSelected)
                }

                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
        },
        content = {
            Column(modifier = Modifier.background(color = DatePickerDefaults.colors().containerColor)) {
                Column(
                    // Yield height to the AddToCalendarRow below when the dialog runs out of
                    // vertical space (the taller date tab, or when the color palette is open):
                    // without this the row is clipped and only the checkbox shows. fill = false
                    // keeps the picker at its natural height when there is room to spare.
                    modifier = Modifier.weight(weight = 1f, fill = false),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    PrimaryTabRow(
                        containerColor = DatePickerDefaults.colors().containerColor,
                        selectedTabIndex = selectedTabIndex
                    ) {
                        Tab(
                            selected = selectedTabIndex == DATE_PICKER_TAB,
                            onClick = { onTabSelected(DATE_PICKER_TAB) },
                            text = {
                                Text(text = stringResource(id = R.string.date))
                            }
                        )
                        Tab(
                            selected = selectedTabIndex == TIME_PICKER_TAB,
                            onClick = { onTabSelected(TIME_PICKER_TAB) },
                            text = {
                                Text(text = stringResource(id = R.string.time))
                            }
                        )
                    }
                    when (selectedTabIndex) {
                        DATE_PICKER_TAB -> {
                            DatePicker(
                                modifier = Modifier.padding(borderPadding),
                                state = datePickerState,
                                // No title/headline: keeps the dialog compact enough to always fit
                                // the AddToCalendarRow (and its color palette) without clipping.
                                title = null,
                                headline = null,
                                showModeToggle = false
                            )
                        }

                        TIME_PICKER_TAB -> {
                            TimePicker(
                                modifier = Modifier.padding(borderPadding),
                                state = timePickerState
                            )
                        }
                    }
                }
                AddToCalendarRow(
                    modifier = Modifier.padding(horizontal = borderPadding + cardInnerPadding),
                    addToCalendarState = addToCalendarState,
                    addToCalendarChecked = onAddToCalendarChecked,
                    colorPickerExpanded = colorPickerExpanded,
                    onColorPickerExpandedChange = onColorPickerExpandedChange,
                    onColorSelected = onAddToCalendarColorSelected
                )
                Spacer(modifier = Modifier.padding(verticalFieldPadding))
            }
        }
    )
}

@Composable
private fun SelectTodayButton(today: LocalDate, onPresetSelected: (LocalDate) -> Unit) {
    OutlinedButton(onClick = { onPresetSelected(today) }) {
        Text(text = stringResource(id = R.string.date_time_picker_today))
    }
}

@Composable
private fun SelectNowButton(now: LocalDateTime, onPresetSelected: (LocalDateTime) -> Unit) {
    OutlinedButton(onClick = { onPresetSelected(now) }) {
        Text(text = stringResource(id = R.string.date_time_picker_now))
    }
}

@Composable
private fun AddToCalendarRow(
    addToCalendarState: AddToCalendarState,
    addToCalendarChecked: (Boolean) -> Unit,
    colorPickerExpanded: Boolean,
    onColorPickerExpandedChange: (Boolean) -> Unit,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    when (addToCalendarState) {
        is AddToCalendarState.Visible -> {
            val enabled = addToCalendarState.checked
            Column(modifier = modifier) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = addToCalendarState.checked,
                        onCheckedChange = addToCalendarChecked
                    )
                    Text(text = stringResource(id = R.string.date_time_picker_add_to_calendar))

                    ColorSwatch(
                        color = addToCalendarState.selectedColor,
                        enabled = enabled,
                        onClick = { onColorPickerExpandedChange(!colorPickerExpanded) },
                        modifier = Modifier.padding(start = horizontalFieldPadding)
                    )
                }

                // Tapping the indicator reveals the account's available colors to choose from.
                AnimatedVisibility(visible = enabled && colorPickerExpanded) {
                    CalendarColorPalette(
                        availableColors = addToCalendarState.availableColors,
                        onColorSelected = onColorSelected,
                        modifier = Modifier.padding(top = verticalFieldPadding)
                    )
                }
            }
        }

        AddToCalendarState.Gone -> {}
    }
}

/**
 * The account's available event colors, shown when the color indicator is tapped. Rendered inline
 * (rather than as an anchored popup) so it appears identically in production and in the screenshot
 * renderer, which cannot paint [androidx.compose.ui.window.Popup] content.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarColorPalette(
    availableColors: List<Int>,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        FlowRow(
            modifier = Modifier.padding(cardInnerPadding),
            horizontalArrangement = Arrangement.spacedBy(horizontalFieldPadding),
            verticalArrangement = Arrangement.spacedBy(verticalFieldPadding)
        ) {
            availableColors.forEach { color ->
                ColorSwatch(
                    color = color,
                    enabled = true,
                    onClick = { onColorSelected(color) }
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    @ColorInt color: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(COLOR_SWATCH_SIZE)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(color = color.toComposeColor())
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.extraSmall
            )
            .clickable(enabled = enabled, onClick = onClick)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@VisibleForTesting
@PreviewPhone
@Composable
internal fun DateTimePickerPreview(@PreviewParameter(DateTimePickerPreviewConfigProvider::class) config: DateTimePickerPreviewConfig) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = config.initialSelectedDateMillis
    )
    val timePickerState = rememberTimePickerState(
        initialHour = config.initialHour,
        initialMinute = config.initialMinute,
        is24Hour = config.is24Hour
    )

    VisitasTheme(config.isDarkMode) {
        DateTimePickerContent(
            now = config.now,
            selectedTabIndex = config.selectedTabIndex,
            datePickerState = datePickerState,
            timePickerState = timePickerState,
            addToCalendarState = config.addToCalendarState,
            onAddToCalendarChecked = {},
            onAddToCalendarColorSelected = {},
            colorPickerExpanded = config.colorPickerExpanded,
            onColorPickerExpandedChange = {},
            onTabSelected = {},
            onDatePresetSelected = {},
            onTimePresetSelected = {},
            onConfirm = {},
            onDismiss = {}
        )
    }
}

sealed class AddToCalendarState {
    object Gone : AddToCalendarState()

    data class Visible(
        val checked: Boolean,
        @ColorInt val selectedColor: Int,
        val availableColors: List<Int> = emptyList()
    ) : AddToCalendarState()
}
