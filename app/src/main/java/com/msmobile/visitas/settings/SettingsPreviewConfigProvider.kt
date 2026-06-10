package com.msmobile.visitas.settings

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.msmobile.visitas.MainActivityViewModel
import com.msmobile.visitas.util.IntentState
import com.msmobile.visitas.visit.VisitMapEngineOption

@VisibleForTesting
internal class SettingsPreviewConfigProvider : PreviewParameterProvider<SettingsPreviewConfig> {

    override val values: Sequence<SettingsPreviewConfig> = sequenceOf(
        SettingsPreviewConfig(
            configName = "Default",
            mainActivityUiState = previewMainActivityUiState,
            uiState = SettingsDetailViewModel.UiState(
                selectedMapEngine = VisitMapEngineOption.MapLibre
            )
        ),
        SettingsPreviewConfig(
            configName = "Leaflet Engine",
            mainActivityUiState = previewMainActivityUiState,
            uiState = SettingsDetailViewModel.UiState(
                selectedMapEngine = VisitMapEngineOption.Leaflet
            )
        ),
        SettingsPreviewConfig(
            configName = "Loading",
            mainActivityUiState = previewMainActivityUiState,
            uiState = SettingsDetailViewModel.UiState(
                isLoading = true
            )
        ),
        SettingsPreviewConfig(
            configName = "Restore Success",
            mainActivityUiState = previewMainActivityUiState,
            uiState = SettingsDetailViewModel.UiState(
                backupResult = SettingsDetailViewModel.BackupResult.RestoreSuccess("Backup restored successfully")
            )
        ),
        SettingsPreviewConfig(
            configName = "Restore Failure",
            mainActivityUiState = previewMainActivityUiState,
            uiState = SettingsDetailViewModel.UiState(
                backupResult = SettingsDetailViewModel.BackupResult.RestoreFailure("Failed to restore backup")
            )
        )
    )

    override fun getDisplayName(index: Int): String {
        return values.elementAt(index).configName
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