package com.msmobile.visitas

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.msmobile.visitas.navigation.AppDestination
import com.msmobile.visitas.util.IntentState

@VisibleForTesting
internal class AppScaffoldPreviewConfigProvider : PreviewParameterProvider<AppScaffoldPreviewConfig> {

    override val values: Sequence<AppScaffoldPreviewConfig> = sequenceOf(
        AppScaffoldPreviewConfig(
            configName = "With Bottom Bar and FAB",
            uiState = MainActivityViewModel.UiState(
                eventState = MainActivityViewModel.UiEventState.Idle,
                intentState = IntentState.None
            ),
            currentDestination = AppDestination.VisitList
        ),
        AppScaffoldPreviewConfig(
            configName = "Without Bottom Bar and FAB",
            uiState = MainActivityViewModel.UiState(
                eventState = MainActivityViewModel.UiEventState.Idle,
                intentState = IntentState.None
            ),
            currentDestination = AppDestination.VisitList
        ),
        AppScaffoldPreviewConfig(
            configName = "With Subtitle",
            uiState = MainActivityViewModel.UiState(
                eventState = MainActivityViewModel.UiEventState.Idle,
                intentState = IntentState.None
            ),
            currentDestination = AppDestination.VisitDetail(),
            subtitle = "Draft"
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
    val currentDestination: AppDestination,
    val subtitle: String? = null
)

