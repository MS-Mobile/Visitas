package com.msmobile.visitas

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.msmobile.visitas.util.IntentState
import com.ramcosta.composedestinations.generated.destinations.VisitListScreenDestination
import com.ramcosta.composedestinations.spec.DestinationSpec

@VisibleForTesting
internal class AppScaffoldPreviewConfigProvider : PreviewParameterProvider<AppScaffoldPreviewConfig> {

    override val values: Sequence<AppScaffoldPreviewConfig> = sequenceOf(
        AppScaffoldPreviewConfig(
            configName = "With Bottom Bar and FAB",
            uiState = MainActivityViewModel.UiState(
                eventState = MainActivityViewModel.UiEventState.Idle,
                intentState = IntentState.None
            ),
            currentDestination = VisitListScreenDestination
        ),
        AppScaffoldPreviewConfig(
            configName = "Without Bottom Bar and FAB",
            uiState = MainActivityViewModel.UiState(
                eventState = MainActivityViewModel.UiEventState.Idle,
                intentState = IntentState.None
            ),
            currentDestination = VisitListScreenDestination
        )
    )

    override fun getDisplayName(index: Int): String {
        return values.elementAt(index).configName
    }
}

@VisibleForTesting
internal data class AppScaffoldPreviewConfig(
    val configName: String,
    val uiState: MainActivityViewModel.UiState,
    val currentDestination: DestinationSpec
)

