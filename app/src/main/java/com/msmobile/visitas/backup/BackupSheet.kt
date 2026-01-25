package com.msmobile.visitas.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.msmobile.visitas.AppScaffold
import com.msmobile.visitas.MainActivityViewModel
import com.msmobile.visitas.R
import com.msmobile.visitas.extension.showShareIntent
import com.msmobile.visitas.util.IntentState
import com.msmobile.visitas.util.borderPadding
import com.ramcosta.composedestinations.generated.destinations.VisitListScreenDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSheet(
    isVisible: Boolean,
    uiState: BackupViewModel.UiState,
    onBackupSheetEvent: (BackupViewModel.UiEvent) -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(visible = isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            BackupScreenContent(
                uiState = uiState,
                onEvent = onBackupSheetEvent
            )
        }
    }
}

@Composable
private fun BackupScreenContent(
    uiState: BackupViewModel.UiState,
    onEvent: (BackupViewModel.UiEvent) -> Unit
) {
    val context = LocalContext.current
    val restoreBackupLauncher = rememberRestoreBackupLauncher(onEvent)

    // Handle share intent when backup creation is successful
    val backupResult = uiState.backupResult as? BackupViewModel.BackupResult.BackupCreationSuccess
    val shareUri = backupResult?.shareFileUri
    LaunchedEffect(shareUri) {
        if (shareUri == null) return@LaunchedEffect
        context.showShareIntent(shareUri, BACKUP_MIME_TYPE)
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedButton(
            onClick = {
                onEvent(
                    BackupViewModel.UiEvent.CreateBackup(
                        successMessage = context.getString(R.string.create_backup_success),
                        errorMessage = context.getString(R.string.create_backup_failure)
                    )
                )
            }
        ) {
            Text(text = stringResource(R.string.create_backup))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                restoreBackupLauncher.launch(arrayOf("application/octet-stream"))
            }
        ) {
            Text(text = stringResource(R.string.restore_backup))
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp)
            )
        }

        uiState.backupResult?.let { result ->
            if (result is BackupViewModel.BackupResult.BackupCreationSuccess) {
                /* Don't show the success message as the share intent is being handled */
                return@let
            }
            Snackbar(
                modifier = Modifier.padding(borderPadding),
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = when (result) {
                        is BackupViewModel.BackupResult.RestoreFailure -> result.message
                        is BackupViewModel.BackupResult.RestoreSuccess -> result.message
                        is BackupViewModel.BackupResult.BackupCreationSuccess -> {
                            return@Snackbar
                        }
                    },
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun rememberRestoreBackupLauncher(
    onEvent: (BackupViewModel.UiEvent) -> Unit
): ActivityResultLauncher<Array<String>> {
    val context = LocalContext.current
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            onEvent(
                BackupViewModel.UiEvent.RestoreBackup(
                    fileUri = uri,
                    successMessage = context.getString(R.string.restore_backup_success),
                    errorMessage = context.getString(R.string.restore_backup_failure)
                )
            )
        }
    }
}

@Preview
@Composable
fun BackupScreenPreview() {
    AppScaffold(
        uiState = MainActivityViewModel.UiState(
            scaffoldState = MainActivityViewModel.ScaffoldState(
                showBottomBar = false,
                showFAB = false
            ),
            eventState = MainActivityViewModel.UiEventState.Idle,
            isTimerRunning = false,
            intentState = IntentState.None
        ),
        currentDestination = VisitListScreenDestination,
        onEvent = {},
        onNavigateToTab = {},
        onNavigate = {}
    ) {
        BackupScreenContent(
            uiState = BackupViewModel.UiState(
                isLoading = false,
                backupResult = BackupViewModel.BackupResult.RestoreFailure(message = stringResource(R.string.restore_backup_failure))
            ),
            onEvent = {}
        )
    }
}

private const val BACKUP_MIME_TYPE = "application/octet-stream"