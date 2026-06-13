package com.msmobile.visitas.ui.views

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.msmobile.visitas.R
import com.msmobile.visitas.ui.theme.PreviewFoldable
import com.msmobile.visitas.ui.theme.PreviewPhone
import com.msmobile.visitas.ui.theme.VisitasTheme
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
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dateTime.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = dateTime.hour,
        initialMinute = dateTime.minute,
        is24Hour = true
    )

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
        datePickerState.selectedDateMillis =
            presetDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    }

    val onTimePresetSelected = { presetTime: LocalDateTime ->
        timePickerState.hour = presetTime.hour
        timePickerState.minute = presetTime.minute
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
@VisibleForTesting
@PreviewPhone
@PreviewFoldable
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
            selectedTabIndex = config.selectedTabIndex,
            datePickerState = datePickerState,
            timePickerState = timePickerState,
            onTabSelected = {},
        )
    }
}

