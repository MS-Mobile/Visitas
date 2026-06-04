package com.msmobile.visitas.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msmobile.visitas.BuildConfig
import com.msmobile.visitas.R
import com.msmobile.visitas.extension.showShareIntent
import com.msmobile.visitas.util.DetailScreenStyle
import com.msmobile.visitas.util.borderPadding
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

private const val BACKUP_MIME_TYPE = "application/octet-stream"

@Destination<RootGraph>(style = DetailScreenStyle::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsDetailViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onEvent = viewModel::onEvent

    SettingsScreenContent(
        uiState = uiState,
        onEvent = onEvent
    )
}

@Composable
private fun SettingsScreenContent(
    uiState: SettingsDetailViewModel.UiState,
    onEvent: (SettingsDetailViewModel.UiEvent) -> Unit
) {
    val context = LocalContext.current
    val createBackupSuccessMessage = stringResource(R.string.create_backup_success)
    val createBackupFailureMessage = stringResource(R.string.create_backup_failure)
    val restoreBackupLauncher = rememberRestoreBackupLauncher(onEvent)

    val backupResult = uiState.backupResult as? SettingsDetailViewModel.BackupResult.BackupCreationSuccess
    val shareUri = backupResult?.shareFileUri
    LaunchedEffect(shareUri) {
        if (shareUri == null) return@LaunchedEffect
        context.showShareIntent(shareUri, BACKUP_MIME_TYPE)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = borderPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onEvent(
                    SettingsDetailViewModel.UiEvent.CreateBackup(
                        successMessage = createBackupSuccessMessage,
                        errorMessage = createBackupFailureMessage
                    )
                )
            }
        ) {
            Text(text = stringResource(R.string.create_backup))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                restoreBackupLauncher.launch(arrayOf(BACKUP_MIME_TYPE))
            }
        ) {
            Text(text = stringResource(R.string.restore_backup))
        }

        if (uiState.isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }

        uiState.backupResult?.let { result ->
            if (result is SettingsDetailViewModel.BackupResult.BackupCreationSuccess) {
                return@let
            }
            Spacer(modifier = Modifier.height(16.dp))
            Snackbar(
                modifier = Modifier.padding(borderPadding),
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = when (result) {
                        is SettingsDetailViewModel.BackupResult.RestoreFailure -> result.message
                        is SettingsDetailViewModel.BackupResult.RestoreSuccess -> result.message
                        is SettingsDetailViewModel.BackupResult.BackupCreationSuccess -> return@Snackbar
                    },
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = stringResource(R.string.app_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun rememberRestoreBackupLauncher(
    onEvent: (SettingsDetailViewModel.UiEvent) -> Unit
): ActivityResultLauncher<Array<String>> {
    val restoreBackupSuccessMessage = stringResource(R.string.restore_backup_success)
    val restoreBackupFailureMessage = stringResource(R.string.restore_backup_failure)
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            onEvent(
                SettingsDetailViewModel.UiEvent.RestoreBackup(
                    fileUri = uri,
                    successMessage = restoreBackupSuccessMessage,
                    errorMessage = restoreBackupFailureMessage
                )
            )
        }
    }
}
