package com.msmobile.visitas.util

import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.install.model.AppUpdateType
import com.msmobile.visitas.BuildConfig
import com.msmobile.visitas.MainActivity
import com.msmobile.visitas.R
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

// Release version - full installation validation
internal fun MainActivity.validateInstallationSource() {
    if (BuildConfig.DEBUG) {
        return
    }
    lifecycleScope.launch {
        if (!installerVerification.isValidInstallSource(this@validateInstallationSource)) {
            Toast.makeText(
                this@validateInstallationSource,
                getString(R.string.invalid_install_source),
                Toast.LENGTH_LONG
            ).show()
            finishAndRemoveTask()
        }
    }
}

/**
 * Initializes in-app updates and starts checking for available updates.
 *
 * @param launcher The activity result launcher for handling the update flow
 * @param onUpdateTypeChanged Callback to track the pending update type (for handling user decline)
 */
internal fun MainActivity.initializeInAppUpdates(
    launcher: ActivityResultLauncher<IntentSenderRequest>,
    onUpdateTypeChanged: (Int?) -> Unit
) {
    inAppUpdateManager.initialize(this)

    inAppUpdateManager.checkForUpdate()
        .onEach { state ->
            when (state) {
                is InAppUpdateManager.UpdateState.UpdateAvailable -> {
                    onUpdateTypeChanged(state.updateType)
                    inAppUpdateManager.startUpdate(
                        state.appUpdateInfo,
                        state.updateType,
                        launcher
                    )
                    if (state.updateType == AppUpdateType.FLEXIBLE) {
                        observeFlexibleUpdateProgress()
                    }
                }
                is InAppUpdateManager.UpdateState.UpdateInProgress -> {
                    onUpdateTypeChanged(AppUpdateType.IMMEDIATE)
                    inAppUpdateManager.startUpdate(
                        state.appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        launcher
                    )
                }
                is InAppUpdateManager.UpdateState.Error,
                is InAppUpdateManager.UpdateState.NoUpdateAvailable -> {
                    // No action needed
                }
            }
        }
        .launchIn(lifecycleScope)
}

/**
 * Observes flexible update download progress and completes the update when ready.
 */
private fun MainActivity.observeFlexibleUpdateProgress() {
    inAppUpdateManager.observeInstallState()
        .onEach { installState ->
            when (installState) {
                is InAppUpdateManager.InstallState.Downloaded -> {
                    Toast.makeText(
                        this,
                        R.string.update_downloaded_restart,
                        Toast.LENGTH_LONG
                    ).show()
                    inAppUpdateManager.completeUpdate()
                }
                else -> {
                    // Other states (downloading, installing, failed, etc.)
                }
            }
        }
        .launchIn(lifecycleScope)
}

