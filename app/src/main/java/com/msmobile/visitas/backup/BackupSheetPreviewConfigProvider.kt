package com.msmobile.visitas.backup

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.msmobile.visitas.MainActivityViewModel
import com.msmobile.visitas.util.IntentState

@VisibleForTesting
internal class BackupSheetPreviewConfigProvider : PreviewParameterProvider<BackupSheetPreviewConfig> {

    private val previewConfigLight = sequenceOf(
        BackupSheetPreviewConfig(
            configName = "Idle",
            mainActivityUiState = previewMainActivityUiState,
            backupUiState = BackupViewModel.UiState(),
            isDarkMode = false
        ),
        BackupSheetPreviewConfig(
            configName = "Loading",
            mainActivityUiState = previewMainActivityUiState,
            backupUiState = BackupViewModel.UiState(isLoading = true),
            isDarkMode = false
        ),
        BackupSheetPreviewConfig(
            configName = "Restore Failure",
            mainActivityUiState = previewMainActivityUiState,
            backupUiState = BackupViewModel.UiState(
                isLoading = false,
                backupResult = BackupViewModel.BackupResult.RestoreFailure(
                    message = "Failed to restore backup"
                )
            ),
            isDarkMode = false
        ),
        BackupSheetPreviewConfig(
            configName = "Restore Success",
            mainActivityUiState = previewMainActivityUiState,
            backupUiState = BackupViewModel.UiState(
                isLoading = false,
                backupResult = BackupViewModel.BackupResult.RestoreSuccess(
                    message = "Backup restored successfully"
                )
            ),
            isDarkMode = false
        )
    )

    private val previewConfigDark = previewConfigLight.map { config ->
        config.copy(
            configName = "${config.configName} - Dark Mode",
            isDarkMode = true
        )
    }

    override val values: Sequence<BackupSheetPreviewConfig> = previewConfigLight + previewConfigDark

    override fun getDisplayName(index: Int): String {
        return values.elementAt(index).configName
    }
}

@VisibleForTesting
internal data class BackupSheetPreviewConfig(
    val configName: String,
    val mainActivityUiState: MainActivityViewModel.UiState,
    val backupUiState: BackupViewModel.UiState,
    val isDarkMode: Boolean
)

private val previewMainActivityUiState = MainActivityViewModel.UiState(
    eventState = MainActivityViewModel.UiEventState.Idle,
    intentState = IntentState.None
)

