package com.msmobile.visitas.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.msmobile.visitas.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun MonthNavigator(
    modifier: Modifier = Modifier,
    dateTime: LocalDateTime,
    onEvent: (MonthNavigatorEvent) -> Unit
) {
    val dateFormat = DateTimeFormatter.ofPattern("MMM, yyyy")
    val month = dateFormat.format(dateTime)
    val isNextMonthFutureDate = dateTime
        .plusMonths(1)
        .truncatedTo(ChronoUnit.DAYS) > LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
    val enableFutureDates = !isNextMonthFutureDate
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = { onEvent(MonthNavigatorEvent.PreviousMonthClicked) }) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(
                    id = R.string.previous_month
                )
            )
        }
        Text(text = month, style = MaterialTheme.typography.titleMedium)
        IconButton(
            enabled = enableFutureDates,
            onClick = { onEvent(MonthNavigatorEvent.NextMonthClicked) }) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = stringResource(
                    id = R.string.next_month
                )
            )
        }
    }
}

sealed class MonthNavigatorEvent {
    data object NextMonthClicked : MonthNavigatorEvent()
    data object PreviousMonthClicked : MonthNavigatorEvent()
}