package com.msmobile.visitas.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msmobile.visitas.AppScaffold
import com.msmobile.visitas.BuildConfig
import com.msmobile.visitas.R
import com.msmobile.visitas.extension.showShareIntent
import com.msmobile.visitas.ui.theme.PreviewFoldable
import com.msmobile.visitas.ui.theme.PreviewPhone
import com.msmobile.visitas.ui.theme.VisitasTheme
import com.msmobile.visitas.util.DetailScreenStyle
import com.msmobile.visitas.util.borderPadding
import com.msmobile.visitas.util.cardInnerPadding
import com.msmobile.visitas.util.snackbarPadding
import com.msmobile.visitas.visit.VisitMapEngineOption
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.SettingsScreenDestination

private const val BACKUP_MIME_TYPE = "application/octet-stream"

@Destination<RootGraph>(style = DetailScreenStyle::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsDetailViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onEvent = viewModel::onEvent

    LaunchedEffect(Unit) {
        onEvent(SettingsDetailViewModel.UiEvent.ViewCreated)
    }

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

    val backupResult =
        uiState.backupResult as? SettingsDetailViewModel.BackupResult.BackupCreationSuccess
    val shareUri = backupResult?.shareFileUri
    LaunchedEffect(shareUri) {
        if (shareUri == null) return@LaunchedEffect
        context.showShareIntent(shareUri, BACKUP_MIME_TYPE)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = borderPadding + cardInnerPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        SettingsSection(title = stringResource(R.string.settings_section_backup)) {
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

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    restoreBackupLauncher.launch(arrayOf(BACKUP_MIME_TYPE))
                }
            ) {
                Text(text = stringResource(R.string.restore_backup))
            }
        }

        SettingsSection(title = stringResource(R.string.settings_section_map)) {
            MapEngineDropdown(
                selectedEngine = uiState.selectedMapEngine,
                onEngineSelected = { engine ->
                    onEvent(SettingsDetailViewModel.UiEvent.MapEngineSelected(engine))
                }
            )
        }

        if (uiState.isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }


        when (uiState.backupResult) {
            is SettingsDetailViewModel.BackupResult.RestoreFailure -> {
                RestoreBackupSnackbar(
                    modifier = Modifier.snackbarPadding(),
                    message = uiState.backupResult.message
                )
            }

            is SettingsDetailViewModel.BackupResult.RestoreSuccess -> {
                RestoreBackupSnackbar(
                    modifier = Modifier.snackbarPadding(),
                    message = uiState.backupResult.message
                )
            }

            is SettingsDetailViewModel.BackupResult.BackupCreationSuccess,
            null -> {
                // Do nothing
            }
        }


        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = stringResource(R.string.app_version, uiState.versionName),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun RestoreBackupSnackbar(modifier: Modifier, message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Snackbar(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun ColumnScope.SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 8.dp)
    )
    HorizontalDivider()
    Spacer(modifier = Modifier.height(12.dp))
    content()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapEngineDropdown(
    selectedEngine: VisitMapEngineOption,
    onEngineSelected: (VisitMapEngineOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedEngine.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = stringResource(R.string.map_engine_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            VisitMapEngineOption.entries.forEach { engine ->
                DropdownMenuItem(
                    text = { Text(text = engine.name) },
                    onClick = {
                        onEngineSelected(engine)
                        expanded = false
                    }
                )
            }
        }
    }
}

@VisibleForTesting
@PreviewPhone
@PreviewFoldable
@Composable
internal fun SettingsScreenPreview(
    @PreviewParameter(SettingsPreviewConfigProvider::class) config: SettingsPreviewConfig
) {
    VisitasTheme {
        AppScaffold(
            uiState = config.mainActivityUiState,
            currentDestination = SettingsScreenDestination,
            onEvent = {},
            onNavigateToTab = {},
            onNavigate = {}
        ) {
            SettingsScreenContent(
                uiState = config.uiState,
                onEvent = {}
            )
        }
    }
}
