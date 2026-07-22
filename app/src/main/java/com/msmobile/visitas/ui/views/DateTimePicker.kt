package com.msmobile.visitas.ui.views

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.msmobile.visitas.R
import com.msmobile.visitas.ui.theme.PreviewFoldable
import com.msmobile.visitas.ui.theme.PreviewPhone
import com.msmobile.visitas.ui.theme.VisitasTheme
import com.msmobile.visitas.util.borderPadding
import com.msmobile.visitas.util.horizontalFieldPadding
import com.msmobile.visitas.util.verticalFieldPadding
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

private const val DATE_PICKER_TAB = 0
private const val TIME_PICKER_TAB = 1

/**
 * Discloses that confirming the picker also puts the visit on the user's
 * calendar, and hosts the event color selection. Requesting calendar access
 * from this row's affordance keeps the permission prompt tied to an action
 * the user can attribute it to.
 */
sealed interface AddToCalendarState {
    /** Calendar permission not granted yet: show the affordance that requests it. */
    data object PermissionMissing : AddToCalendarState

    /** Permission granted but the account syncs no event palette (e.g. local calendar). */
    data object NoEventColors : AddToCalendarState

    /** Permission granted: show the account's event palette. */
    data class EventColors(
        val colors: List<CalendarColor>,
        val selectedKey: String?
    ) : AddToCalendarState
}

data class CalendarColor(val key: String, val argb: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePicker(
    dateTime: LocalDateTime,
    addToCalendarState: AddToCalendarState,
    onDateSelected: (LocalDateTime, String?) -> Unit,
    onCalendarAccessRequested: () -> Unit,
    onDismiss: () -> Unit,
    now: LocalDateTime = LocalDateTime.now()
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedDate by remember { mutableStateOf(dateTime) }
    // Null until the user taps a swatch, so an untouched row never overwrites
    // the visit's stored color choice.
    var pickedColorKey by remember { mutableStateOf<String?>(null) }
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
            onDateSelected(selectedDateTime, pickedColorKey)
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

    val addToCalendarStateWithSelection = when (addToCalendarState) {
        is AddToCalendarState.EventColors -> addToCalendarState.copy(
            selectedKey = pickedColorKey ?: addToCalendarState.selectedKey
        )

        else -> addToCalendarState
    }
    DateTimePickerContent(
        now = now,
        selectedTabIndex = selectedTabIndex,
        datePickerState = datePickerState,
        timePickerState = timePickerState,
        addToCalendarState = addToCalendarStateWithSelection,
        onTabSelected = onTabSelected,
        onDatePresetSelected = onDatePresetSelected,
        onTimePresetSelected = onTimePresetSelected,
        onColorSelected = { colorKey -> pickedColorKey = colorKey },
        onCalendarAccessRequested = onCalendarAccessRequested,
        onConfirm = onConfirm,
        onDismiss = onDismiss
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
    onTabSelected: (Int) -> Unit,
    onDatePresetSelected: (LocalDate) -> Unit,
    onTimePresetSelected: (LocalDateTime) -> Unit,
    onColorSelected: (String) -> Unit,
    onCalendarAccessRequested: () -> Unit,
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
        }
    ) {
        Column(modifier = Modifier.background(color = DatePickerDefaults.colors().containerColor)) {
            Column(
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
                            title = null
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
                state = addToCalendarState,
                onColorSelected = onColorSelected,
                onCalendarAccessRequested = onCalendarAccessRequested
            )
        }
    }
}

@Composable
private fun AddToCalendarRow(
    state: AddToCalendarState,
    onColorSelected: (String) -> Unit,
    onCalendarAccessRequested: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = borderPadding),
        verticalArrangement = Arrangement.spacedBy(verticalFieldPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(horizontalFieldPadding)
        ) {
            Icon(
                imageVector = Icons.Rounded.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.add_to_calendar),
                style = MaterialTheme.typography.bodyMedium
            )
            if (state is AddToCalendarState.PermissionMissing) {
                OutlinedButton(onClick = onCalendarAccessRequested) {
                    Text(text = stringResource(id = R.string.date_time_picker_choose_color))
                }
            }
        }
        if (state is AddToCalendarState.EventColors) {
            CalendarColorSwatches(
                colors = state.colors,
                selectedKey = state.selectedKey,
                onColorSelected = onColorSelected
            )
        }
    }
}

@Composable
private fun CalendarColorSwatches(
    colors: List<CalendarColor>,
    selectedKey: String?,
    onColorSelected: (String) -> Unit
) {
    val colorContentDescription = stringResource(id = R.string.calendar_color_content_description)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(horizontalFieldPadding)
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(colorSwatchSize)
                    .clip(CircleShape)
                    // Provider colors may carry no alpha channel; force opaque.
                    .background(Color(color.argb).copy(alpha = 1f))
                    .clickable { onColorSelected(color.key) }
                    .semantics { contentDescription = colorContentDescription },
                contentAlignment = Alignment.Center
            ) {
                if (color.key == selectedKey) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }
}

private val colorSwatchSize = 32.dp

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
        // Preview-only: hosts the expanded overlays (filter menu) above the app bar, which would
        // otherwise clip them in screenshots. No-op / absent in production (see PreviewOverlayHost).
        PreviewOverlayHost {
            DateTimePickerContent(
                now = config.now,
                selectedTabIndex = config.selectedTabIndex,
                datePickerState = datePickerState,
                timePickerState = timePickerState,
                addToCalendarState = config.addToCalendarState,
                onTabSelected = {},
                onDatePresetSelected = {},
                onTimePresetSelected = {},
                onColorSelected = {},
                onCalendarAccessRequested = {},
                onConfirm = {},
                onDismiss = {}
            )
        }
    }
}

