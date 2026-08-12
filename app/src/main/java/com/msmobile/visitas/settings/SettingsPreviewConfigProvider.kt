package com.msmobile.visitas.settings

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.msmobile.visitas.MainActivityViewModel
import com.msmobile.visitas.util.CalendarInfo
import com.msmobile.visitas.util.IntentState
import com.msmobile.visitas.util.identity
import com.msmobile.visitas.visit.VisitMapEngineOption

@VisibleForTesting
internal class SettingsPreviewConfigProvider : PreviewParameterProvider<SettingsPreviewConfig> {

    override val values: Sequence<SettingsPreviewConfig> = sequenceOf(
        SettingsPreviewConfig(
            configName = "Default",
            mainActivityUiState = previewMainActivityUiState,
            uiState = SettingsDetailViewModel.UiState(
                selectedMapEngine = VisitMapEngineOption.MapLibre,
                versionName = APP_VERSION
            )
        ),
        SettingsPreviewConfig(
            configName = "Leaflet Engine",
            mainActivityUiState = previewMainActivityUiState,
            uiState = SettingsDetailViewModel.UiState(
                selectedMapEngine = VisitMapEngineOption.Leaflet,
                versionName = APP_VERSION
            )
        ),
        SettingsPreviewConfig(
            configName = "Loading",
            mainActivityUiState = previewMainActivityUiState,
            uiState = SettingsDetailViewModel.UiState(
                isLoading = true,
                versionName = APP_VERSION
            )
        ),
        SettingsPreviewConfig(
            configName = "Restore Success",
            mainActivityUiState = previewMainActivityUiState,
            uiState = SettingsDetailViewModel.UiState(
                backupResult = SettingsDetailViewModel.BackupResult.RestoreSuccess("Backup restored successfully"),
                versionName = APP_VERSION
            )
        ),
        SettingsPreviewConfig(
            configName = "Restore Failure",
            mainActivityUiState = previewMainActivityUiState,
            uiState = SettingsDetailViewModel.UiState(
                backupResult = SettingsDetailViewModel.BackupResult.RestoreFailure("Failed to restore backup"),
                versionName = APP_VERSION
            )
        ),
        SettingsPreviewConfig(
            configName = "Add Visits To Calendar Enabled",
            mainActivityUiState = previewMainActivityUiState,
            uiState = SettingsDetailViewModel.UiState(
                selectedMapEngine = VisitMapEngineOption.MapLibre,
                addVisitsToCalendar = true,
                versionName = APP_VERSION
            )
        ),
        SettingsPreviewConfig(
            configName = "Calendar Selected",
            mainActivityUiState = previewMainActivityUiState,
            uiState = SettingsDetailViewModel.UiState(
                selectedMapEngine = VisitMapEngineOption.MapLibre,
                addVisitsToCalendar = true,
                availableCalendars = PREVIEW_CALENDARS,
                preferredCalendar = PREVIEW_MINISTRY_CALENDAR.identity,
                versionName = APP_VERSION
            )
        )
    )

    override fun getDisplayName(index: Int): String {
        return values.elementAt(index).configName
    }

    companion object {
        private const val APP_VERSION = "1.0.1#710"

        private val PREVIEW_PERSONAL_CALENDAR = CalendarInfo(
            id = 1L,
            displayName = "Personal",
            accountName = "user@gmail.com",
            ownerAccount = "user@gmail.com",
            accountType = "com.google",
            isPrimary = true,
            isVisible = true
        )

        private val PREVIEW_MINISTRY_CALENDAR = CalendarInfo(
            id = 2L,
            displayName = "Ministry",
            accountName = "user@gmail.com",
            ownerAccount = "ministry123@group.calendar.google.com",
            accountType = "com.google",
            isPrimary = false,
            isVisible = true
        )

        private val PREVIEW_CALENDARS =
            listOf(PREVIEW_PERSONAL_CALENDAR, PREVIEW_MINISTRY_CALENDAR)
    }
}

@VisibleForTesting
internal data class SettingsPreviewConfig(
    val configName: String,
    val mainActivityUiState: MainActivityViewModel.UiState,
    val uiState: SettingsDetailViewModel.UiState
)

private val previewMainActivityUiState = MainActivityViewModel.UiState(
    eventState = MainActivityViewModel.UiEventState.Idle,
    intentState = IntentState.None
)