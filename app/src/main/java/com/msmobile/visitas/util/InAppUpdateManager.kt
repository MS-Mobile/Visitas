package com.msmobile.visitas.util

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.isFlexibleUpdateAllowed
import com.google.android.play.core.ktx.isImmediateUpdateAllowed
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages in-app updates using Google Play's In-App Updates API.
 *
 * Update priority (set via Google Play Developer API when publishing):
 * - 0: Default priority (no prompt)
 * - 1-2: Low priority (flexible update)
 * - 3-4: Medium priority (flexible update with stronger prompt)
 * - 5: High priority (immediate/forced update)
 *
 * The priority threshold for immediate updates can be configured via [IMMEDIATE_UPDATE_PRIORITY_THRESHOLD].
 */
@Singleton
class InAppUpdateManager @Inject constructor() {

    private var appUpdateManager: AppUpdateManager? = null

    /**
     * Minimum priority level that triggers an immediate (forced) update.
     * Updates with priority >= this value will block the user until they update.
     * Updates with priority < this value will use flexible (background) updates.
     */
    private companion object {
        const val IMMEDIATE_UPDATE_PRIORITY_THRESHOLD = 5
    }

    /**
     * Initializes the update manager. Must be called before other methods.
     */
    fun initialize(activity: Activity) {
        appUpdateManager = AppUpdateManagerFactory.create(activity)
    }

    /**
     * Checks for available updates and returns the update state.
     */
    fun checkForUpdate(): Flow<UpdateState> = callbackFlow {
        val manager = appUpdateManager ?: run {
            trySend(UpdateState.Error("AppUpdateManager not initialized"))
            close()
            return@callbackFlow
        }

        manager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            val state = when (appUpdateInfo.updateAvailability()) {
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    val updateType = determineUpdateType(appUpdateInfo)
                    UpdateState.UpdateAvailable(appUpdateInfo, updateType)
                }
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    UpdateState.UpdateInProgress(appUpdateInfo)
                }
                else -> UpdateState.NoUpdateAvailable
            }
            trySend(state)
        }.addOnFailureListener { exception ->
            trySend(UpdateState.Error(exception.message ?: "Unknown error"))
        }

        awaitClose()
    }

    /**
     * Determines the update type based on update priority.
     * Priority 5 = Immediate (forced), Priority 1-4 = Flexible (background).
     */
    private fun determineUpdateType(appUpdateInfo: AppUpdateInfo): Int {
        val priority = appUpdateInfo.updatePriority()
        return when {
            priority >= IMMEDIATE_UPDATE_PRIORITY_THRESHOLD && appUpdateInfo.isImmediateUpdateAllowed -> {
                AppUpdateType.IMMEDIATE
            }
            appUpdateInfo.isFlexibleUpdateAllowed -> AppUpdateType.FLEXIBLE
            appUpdateInfo.isImmediateUpdateAllowed -> AppUpdateType.IMMEDIATE
            else -> AppUpdateType.FLEXIBLE
        }
    }

    /**
     * Starts the update flow.
     *
     * @param appUpdateInfo The update info from [checkForUpdate]
     * @param updateType Either [AppUpdateType.IMMEDIATE] or [AppUpdateType.FLEXIBLE]
     * @param launcher The activity result launcher for handling the update flow
     */
    fun startUpdate(
        appUpdateInfo: AppUpdateInfo,
        updateType: Int,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        val manager = appUpdateManager ?: return

        manager.startUpdateFlowForResult(
            appUpdateInfo,
            launcher,
            AppUpdateOptions.newBuilder(updateType).build()
        )
    }

    /**
     * Observes the install state for flexible updates.
     * Use this to show download progress and prompt the user to restart when ready.
     */
    fun observeInstallState(): Flow<InstallState> = callbackFlow {
        val manager = appUpdateManager ?: run {
            close()
            return@callbackFlow
        }

        val listener = InstallStateUpdatedListener { state ->
            val installState = when (state.installStatus()) {
                InstallStatus.DOWNLOADING -> {
                    val progress = if (state.totalBytesToDownload() > 0) {
                        (state.bytesDownloaded() * 100 / state.totalBytesToDownload()).toInt()
                    } else 0
                    InstallState.Downloading(progress)
                }
                InstallStatus.DOWNLOADED -> InstallState.Downloaded
                InstallStatus.INSTALLING -> InstallState.Installing
                InstallStatus.INSTALLED -> InstallState.Installed
                InstallStatus.FAILED -> InstallState.Failed
                InstallStatus.CANCELED -> InstallState.Canceled
                else -> InstallState.Unknown
            }
            trySend(installState)
        }

        manager.registerListener(listener)

        awaitClose {
            manager.unregisterListener(listener)
        }
    }

    /**
     * Completes the update for flexible updates.
     * Call this when the user confirms they want to restart the app.
     */
    fun completeUpdate() {
        appUpdateManager?.completeUpdate()
    }

    /**
     * Checks if an immediate update was interrupted and needs to be resumed.
     * Call this in onResume to handle cases where user backs out of an immediate update.
     */
    fun checkForStalledUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        appUpdateManager?.appUpdateInfo?.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                // Resume the update if it was an immediate update
                if (appUpdateInfo.isImmediateUpdateAllowed) {
                    startUpdate(appUpdateInfo, AppUpdateType.IMMEDIATE, launcher)
                }
            }
        }
    }

    /**
     * Represents the current state of update availability.
     */
    sealed class UpdateState {
        data object NoUpdateAvailable : UpdateState()
        data class UpdateAvailable(
            val appUpdateInfo: AppUpdateInfo,
            val updateType: Int
        ) : UpdateState()
        data class UpdateInProgress(val appUpdateInfo: AppUpdateInfo) : UpdateState()
        data class Error(val message: String) : UpdateState()
    }

    /**
     * Represents the install progress state for flexible updates.
     */
    sealed class InstallState {
        data class Downloading(val progress: Int) : InstallState()
        data object Downloaded : InstallState()
        data object Installing : InstallState()
        data object Installed : InstallState()
        data object Failed : InstallState()
        data object Canceled : InstallState()
        data object Unknown : InstallState()
    }
}
