package com.msmobile.visitas.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msmobile.visitas.preference.PreferenceRepository
import com.msmobile.visitas.util.AppVersionProvider
import com.msmobile.visitas.util.BackupHandler
import com.msmobile.visitas.util.CalendarEventManager
import com.msmobile.visitas.util.DispatcherProvider
import com.msmobile.visitas.visit.VisitMapEngineOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsDetailViewModel @Inject constructor(
    private val preferenceRepository: PreferenceRepository,
    private val calendarEventManager: CalendarEventManager,
    private val backupHandler: BackupHandler,
    private val dispatchers: DispatcherProvider,
    private val appVersionProvider: AppVersionProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    init {
        _uiState.update { state ->
            state.copy(versionName = "${appVersionProvider.getVersionName()}+${appVersionProvider.getVersionCode()}")
        }
    }

    fun onEvent(event: UiEvent) {
        when (event) {
            is UiEvent.ViewCreated -> viewCreated()
            is UiEvent.MapEngineSelected -> mapEngineSelected(event.engine)
            is UiEvent.AddVisitsToCalendarToggled -> addVisitsToCalendarToggled(event.enabled)
            is UiEvent.CalendarRationaleAccepted -> calendarRationaleAccepted()
            is UiEvent.CalendarRationaleDismissed -> calendarRationaleDismissed()
            is UiEvent.CalendarPermissionGranted -> calendarPermissionGranted()
            is UiEvent.CalendarPermissionDenied -> calendarPermissionDenied()
            is UiEvent.CreateBackup -> handleCreateBackup(event.successMessage, event.errorMessage)
            is UiEvent.RestoreBackup -> handleRestoreBackup(
                event.fileUri,
                event.successMessage,
                event.errorMessage
            )

            is UiEvent.CreateBackupFailed -> handleCreateBackupError(event.message)
            is UiEvent.RestoreBackupFailed -> handleRestoreBackupError(event.message)
            is UiEvent.BackupCanceled -> handleBackupCanceled()
            is UiEvent.BackupResultAcknowledged -> handleBackupResultAcknowledged()
        }
    }

    private fun viewCreated() {
        viewModelScope.launch(dispatchers.io) {
            val preference = preferenceRepository.get()
            _uiState.update {
                it.copy(
                    selectedMapEngine = preference.visitMapEngineOption,
                    addVisitsToCalendar = preference.addVisitsToCalendar
                )
            }
        }
    }

    private fun mapEngineSelected(engine: VisitMapEngineOption) {
        viewModelScope.launch(dispatchers.io) {
            val preference = preferenceRepository.get().copy(visitMapEngineOption = engine)
            preferenceRepository.save(preference)
        }
        _uiState.update { it.copy(selectedMapEngine = engine) }
    }

    /**
     * Turning the setting on needs calendar permission, so an ungranted permission routes through
     * the rationale sheet first and only persists once the system prompt is granted. Turning it off
     * needs no permission and is persisted immediately; calendar events already created are left in
     * place so re-enabling keeps updating them.
     */
    private fun addVisitsToCalendarToggled(enabled: Boolean) {
        if (!enabled) {
            saveAddVisitsToCalendar(enabled = false)
            return
        }
        if (calendarEventManager.hasCalendarPermission()) {
            saveAddVisitsToCalendar(enabled = true)
            return
        }
        _uiState.update { it.copy(showCalendarRationale = true) }
    }

    private fun calendarRationaleAccepted() {
        _uiState.update {
            it.copy(
                showCalendarRationale = false,
                showCalendarPermissionDialog = true
            )
        }
    }

    private fun calendarRationaleDismissed() {
        _uiState.update {
            it.copy(
                showCalendarRationale = false,
                showCalendarPermissionDialog = false
            )
        }
    }

    private fun calendarPermissionGranted() {
        _uiState.update { it.copy(showCalendarPermissionDialog = false) }
        saveAddVisitsToCalendar(enabled = true)
    }

    private fun calendarPermissionDenied() {
        // The checkbox stays off: without permission there is nothing to write events to.
        _uiState.update { it.copy(showCalendarPermissionDialog = false) }
    }

    private fun saveAddVisitsToCalendar(enabled: Boolean) {
        viewModelScope.launch(dispatchers.io) {
            val preference = preferenceRepository.get().copy(addVisitsToCalendar = enabled)
            preferenceRepository.save(preference)
        }
        _uiState.update { it.copy(addVisitsToCalendar = enabled) }
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
        data object ViewCreated : UiEvent()
        data class MapEngineSelected(val engine: VisitMapEngineOption) : UiEvent()
        data class AddVisitsToCalendarToggled(val enabled: Boolean) : UiEvent()
        data object CalendarRationaleAccepted : UiEvent()
        data object CalendarRationaleDismissed : UiEvent()
        data object CalendarPermissionGranted : UiEvent()
        data object CalendarPermissionDenied : UiEvent()

        data class CreateBackup(
            val successMessage: String,
            val errorMessage: String
        ) : UiEvent()

        data class RestoreBackup(
            val fileUri: Uri,
            val successMessage: String,
            val errorMessage: String
        ) : UiEvent()

        data class CreateBackupFailed(val message: String) : UiEvent()
        data class RestoreBackupFailed(val message: String) : UiEvent()
        data object BackupCanceled : UiEvent()
        data object BackupResultAcknowledged : UiEvent()
    }

    sealed class UiEventState {
        data object Idle : UiEventState()
    }

    sealed class BackupResult {
        data class BackupCreationSuccess(
            val message: String,
            val shareFileUri: Uri
        ) : BackupResult()

        data class RestoreSuccess(val message: String) : BackupResult()
        data class RestoreFailure(val message: String) : BackupResult()
    }

    data class UiState(
        val isLoading: Boolean = false,
        val backupResult: BackupResult? = null,
        val eventState: UiEventState = UiEventState.Idle,
        val selectedMapEngine: VisitMapEngineOption = VisitMapEngineOption.MapLibre,
        val addVisitsToCalendar: Boolean = false,
        val showCalendarRationale: Boolean = false,
        val showCalendarPermissionDialog: Boolean = false,
        val versionName: String = ""
    )
}
