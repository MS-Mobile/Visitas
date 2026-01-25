package com.msmobile.visitas

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.play.core.install.model.AppUpdateType
import com.msmobile.visitas.util.InAppUpdateManager
import com.msmobile.visitas.util.InstallerVerification
import com.msmobile.visitas.util.IntentState
import com.msmobile.visitas.util.initializeInAppUpdates
import com.msmobile.visitas.util.isValidBackupUri
import com.msmobile.visitas.util.validateInstallationSource
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

    @Inject
    lateinit var installerVerification: InstallerVerification

    @Inject
    lateinit var inAppUpdateManager: InAppUpdateManager

    private var pendingUpdateType: Int? = null

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK && pendingUpdateType == AppUpdateType.IMMEDIATE) {
            finish()
        }
        pendingUpdateType = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        validateInstallationSource()
        initializeInAppUpdates(updateLauncher, ::updateTypeChanged)
        handleIntent(intent)
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val onEvent = viewModel::onEvent
            Main(
                uiState = uiState,
                onEvent = onEvent
            )
        }
    }

    override fun onResume() {
        super.onResume()
        inAppUpdateManager.checkForStalledUpdate(updateLauncher)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            val uri = intent.data ?: return
            if (contentResolver.isValidBackupUri(uri)) {
                viewModel.onEvent(
                    MainActivityViewModel.UiEvent.IntentStateChanged(
                        IntentState.PreviewBackupFile(uri)
                    )
                )
            }
        }
    }

    private fun updateTypeChanged(updateType: Int?) {
        pendingUpdateType = updateType
    }
}