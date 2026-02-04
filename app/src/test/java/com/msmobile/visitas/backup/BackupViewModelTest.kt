package com.msmobile.visitas.backup

import android.net.Uri
import com.msmobile.visitas.util.BackupHandler
import com.msmobile.visitas.util.DispatcherProvider
import com.msmobile.visitas.util.MainDispatcherRule
import com.msmobile.visitas.util.MockReferenceHolder
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class BackupViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state has expected default values`() {
        // Arrange
        val viewModel = createViewModel()

        // Assert
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.backupResult)
    }

    @Test
    fun `onEvent with CreateBackup sets isLoading to true and then success result`() {
        // Arrange
        val uriRef = MockReferenceHolder<Uri>()
        val viewModel = createViewModel(
            createBackupResult = CreateBackupResult.Success,
            uriRef = uriRef
        )

        // Act
        viewModel.onEvent(
            BackupViewModel.UiEvent.CreateBackup(
                successMessage = "Backup created",
                errorMessage = "Backup failed"
            )
        )

        // Assert
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.backupResult is BackupViewModel.BackupResult.BackupCreationSuccess)
        val result = state.backupResult as BackupViewModel.BackupResult.BackupCreationSuccess
        assertEquals("Backup created", result.message)
        assertEquals(requireNotNull(uriRef.value), result.shareFileUri)
    }

    @Test
    fun `onEvent with CreateBackup failure sets error result`() {
        // Arrange
        // Note: The ViewModel uses RestoreFailure for all failure cases (both backup creation and restore)
        val viewModel = createViewModel(createBackupResult = CreateBackupResult.Failure)

        // Act
        viewModel.onEvent(
            BackupViewModel.UiEvent.CreateBackup(
                successMessage = "Backup created",
                errorMessage = "Backup failed"
            )
        )

        // Assert
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.backupResult is BackupViewModel.BackupResult.RestoreFailure)
        val result = state.backupResult as BackupViewModel.BackupResult.RestoreFailure
        assertEquals("Backup failed", result.message)
    }

    @Test
    fun `onEvent with RestoreBackup success sets restore success result`() {
        // Arrange
        val uriRef = MockReferenceHolder<Uri>()
        val viewModel = createViewModel(
            restoreBackupResult = RestoreBackupResult.Success,
            uriRef = uriRef
        )

        // Act
        viewModel.onEvent(
            BackupViewModel.UiEvent.RestoreBackup(
                fileUri = requireNotNull(uriRef.value),
                successMessage = "Restore successful",
                errorMessage = "Restore failed"
            )
        )

        // Assert
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.backupResult is BackupViewModel.BackupResult.RestoreSuccess)
        val result = state.backupResult as BackupViewModel.BackupResult.RestoreSuccess
        assertEquals("Restore successful", result.message)
    }

    @Test
    fun `onEvent with RestoreBackup failure sets restore failure result`() {
        // Arrange
        val uriRef = MockReferenceHolder<Uri>()
        val viewModel = createViewModel(
            restoreBackupResult = RestoreBackupResult.Failure,
            uriRef = uriRef
        )

        // Act
        viewModel.onEvent(
            BackupViewModel.UiEvent.RestoreBackup(
                fileUri = requireNotNull(uriRef.value),
                successMessage = "Restore successful",
                errorMessage = "Restore failed"
            )
        )

        // Assert
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.backupResult is BackupViewModel.BackupResult.RestoreFailure)
        val result = state.backupResult as BackupViewModel.BackupResult.RestoreFailure
        assertEquals("Restore failed", result.message)
    }

    @Test
    fun `onEvent with CreateBackUpFailed sets restore failure result`() {
        // Arrange
        val viewModel = createViewModel()

        // Act
        viewModel.onEvent(BackupViewModel.UiEvent.CreateBackUpFailed("Create backup error"))

        // Assert
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.backupResult is BackupViewModel.BackupResult.RestoreFailure)
        val result = state.backupResult as BackupViewModel.BackupResult.RestoreFailure
        assertEquals("Create backup error", result.message)
    }

    @Test
    fun `onEvent with RestoreBackupFailed sets restore failure result`() {
        // Arrange
        val viewModel = createViewModel()

        // Act
        viewModel.onEvent(BackupViewModel.UiEvent.RestoreBackupFailed("Restore error"))

        // Assert
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.backupResult is BackupViewModel.BackupResult.RestoreFailure)
        val result = state.backupResult as BackupViewModel.BackupResult.RestoreFailure
        assertEquals("Restore error", result.message)
    }

    @Test
    fun `onEvent with BackupCanceled clears backupResult`() {
        // Arrange
        val viewModel = createViewModel()
        viewModel.onEvent(BackupViewModel.UiEvent.CreateBackUpFailed("Error"))

        // Act
        viewModel.onEvent(BackupViewModel.UiEvent.BackupCanceled)

        // Assert
        assertNull(viewModel.uiState.value.backupResult)
    }

    @Test
    fun `onEvent with BackupResultAcknowledged clears backupResult`() {
        // Arrange
        val viewModel = createViewModel()
        viewModel.onEvent(BackupViewModel.UiEvent.CreateBackUpFailed("Error"))

        // Act
        viewModel.onEvent(BackupViewModel.UiEvent.BackupResultAcknowledged)

        // Assert
        assertNull(viewModel.uiState.value.backupResult)
    }

    private fun createViewModel(
        createBackupResult: CreateBackupResult? = null,
        restoreBackupResult: RestoreBackupResult? = null,
        uriRef: MockReferenceHolder<Uri>? = null
    ): BackupViewModel {
        val dispatchers = DispatcherProvider(
            io = mainDispatcherRule.dispatcher
        )
        val mockUri = mock<Uri>()
        uriRef?.value = mockUri

        val backupHandler = mock<BackupHandler> {
            when (createBackupResult) {
                CreateBackupResult.Success -> on { createBackupFile() } doReturn Result.success(mockUri)
                CreateBackupResult.Failure -> on { createBackupFile() } doReturn Result.failure(Exception("Test error"))
                null -> {}
            }
            when (restoreBackupResult) {
                RestoreBackupResult.Success -> on { restoreBackup(any()) } doReturn Result.success(Unit)
                RestoreBackupResult.Failure -> on { restoreBackup(any()) } doReturn Result.failure(Exception("Test error"))
                null -> {}
            }
        }

        return BackupViewModel(
            backupHandler = backupHandler,
            dispatchers = dispatchers
        )
    }

    private sealed class CreateBackupResult {
        data object Success : CreateBackupResult()
        data object Failure : CreateBackupResult()
    }

    private sealed class RestoreBackupResult {
        data object Success : RestoreBackupResult()
        data object Failure : RestoreBackupResult()
    }
}
