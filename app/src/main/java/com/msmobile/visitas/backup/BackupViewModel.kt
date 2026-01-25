package com.msmobile.visitas.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msmobile.visitas.util.BackupHandler
import com.msmobile.visitas.util.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupHandler: BackupHandler,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onEvent(event: UiEvent) {
        when (event) {
            is UiEvent.CreateBackup -> handleCreateBackup(event.successMessage, event.errorMessage)
            is UiEvent.RestoreBackup -> handleRestoreBackup(event.fileUri, event.successMessage, event.errorMessage)
            is UiEvent.CreateBackUpFailed -> handleCreateBackupError(event.message)
            is UiEvent.RestoreBackupFailed -> handleRestoreBackupError(event.message)
            is UiEvent.BackupCanceled -> handleBackupCanceled()
            is UiEvent.BackupResultAcknowledged -> handleBackupResultAcknowledged()
        }
    }

    private fun handleCreateBackup(successMessage: String, errorMessage: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch(dispatchers.io) {
            backupHandler.createBackupFile()
                .onSuccess { uri ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            backupResult = BackupResult.BackupCreationSuccess(
                                message = successMessage,
                                shareFileUri = uri
                            )
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            backupResult = BackupResult.RestoreFailure(errorMessage)
                        )
                    }
                }
        }
    }

    private fun handleRestoreBackup(fileUri: Uri, successMessage: String, errorMessage: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch(dispatchers.io) {
            backupHandler.restoreBackup(fileUri)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            backupResult = BackupResult.RestoreSuccess(successMessage)
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            backupResult = BackupResult.RestoreFailure(errorMessage)
                        )
                    }
                }
        }
    }

    private fun handleCreateBackupError(message: String) {
        _uiState.update {
            it.copy(
                isLoading = false,
                backupResult = BackupResult.RestoreFailure(message)
            )
        }
    }

    private fun handleRestoreBackupError(message: String) {
        _uiState.update {
            it.copy(
                isLoading = false,
                backupResult = BackupResult.RestoreFailure(message)
            )
        }
    }

    private fun handleBackupResultAcknowledged() {
        _uiState.update { it.copy(backupResult = null) }
    }

    private fun handleBackupCanceled() {
        _uiState.update { it.copy(backupResult = null) }
    }

    sealed class UiEvent {
        data class CreateBackup(
            val successMessage: String,
            val errorMessage: String
        ) : UiEvent()

        data class RestoreBackup(
            val fileUri: Uri,
            val successMessage: String,
            val errorMessage: String
        ) : UiEvent()

        data class CreateBackUpFailed(val message: String) : UiEvent()
        data class RestoreBackupFailed(val message: String) : UiEvent()
        data object BackupCanceled : UiEvent()
        data object BackupResultAcknowledged : UiEvent()
    }

    data class UiState(
        val isLoading: Boolean = false,
        val backupResult: BackupResult? = null
    )

    sealed class BackupResult {
        data class BackupCreationSuccess(
            val message: String,
            val shareFileUri: Uri
        ) : BackupResult()

        data class RestoreSuccess(val message: String) : BackupResult()
        data class RestoreFailure(val message: String) : BackupResult()
    }
}