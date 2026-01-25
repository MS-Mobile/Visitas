package com.msmobile.visitas.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.msmobile.visitas.R
import com.msmobile.visitas.util.borderPadding
import com.msmobile.visitas.util.horizontalFieldPadding
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

private const val DATE_PICKER_TAB = 0
private const val TIME_PICKER_TAB = 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePicker(
    dateTime: LocalDateTime,
    onDateSelected: (LocalDateTime) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedDate by remember { mutableStateOf(dateTime) }
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
        ) ?: LocalDateTime.now()
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
                    SelectTodayButton(onPresetSelected = onDatePresetSelected)
                } else {
                    SelectNowButton(onPresetSelected = onTimePresetSelected)
                }

                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
        }
    ) {
        DateTimePickerContent(
            selectedTabIndex = selectedTabIndex,
            datePickerState = datePickerState,
            timePickerState = timePickerState,
            onTabSelected = onTabSelected
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerContent(
    selectedTabIndex: Int,
    datePickerState: DatePickerState,
    timePickerState: TimePickerState,
    onTabSelected: (Int) -> Unit
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
    }
}

@Composable
private fun SelectTodayButton(onPresetSelected: (LocalDate) -> Unit) {
    OutlinedButton(
        onClick = {
            val today = LocalDate.now()
            onPresetSelected(today)
        }
    ) {
        Text(text = stringResource(id = R.string.date_time_picker_today))
    }
}

@Composable
private fun SelectNowButton(onPresetSelected: (LocalDateTime) -> Unit) {
    OutlinedButton(
        onClick = {
            val now = LocalDateTime.now()
            onPresetSelected(now)
        }
    ) {
        Text(text = stringResource(id = R.string.date_time_picker_now))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun DateTimePickerPreview(@PreviewParameter(PreviewConfigProvider::class) config: PreviewConfig) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = null
    )
    val timePickerState = rememberTimePickerState(
        initialHour = 10,
        initialMinute = 12,
        is24Hour = true
    )

    DateTimePickerContent(
        selectedTabIndex = config.selectedTabIndex,
        datePickerState = datePickerState,
        timePickerState = timePickerState,
        onTabSelected = {},
    )
}

private class PreviewConfigProvider : PreviewParameterProvider<PreviewConfig> {
    override val values: Sequence<PreviewConfig> = sequenceOf(
        PreviewConfig(selectedTabIndex = 0),
        PreviewConfig(selectedTabIndex = 1)
    )
}

private data class PreviewConfig(
    val selectedTabIndex: Int
)
