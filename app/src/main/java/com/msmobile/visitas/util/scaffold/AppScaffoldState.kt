package com.msmobile.visitas.util.scaffold

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Relays navigation chrome from the screen that owns it up to the single app-level `AppScaffold`.
 *
 * There is one Scaffold in the app, hosted by `Main`; screens are plain composables that fill its
 * content slot. A screen that needs a back arrow, app-bar actions, an overflow item, a detail
 * footer, a FAB or a subtitle publishes them here instead of nesting a Scaffold of its own.
 *
 * Deliberately a plain holder (`remember { AppScaffoldState() }` in `Main`, threaded to screens by
 * `di/NavigationDependencies.kt`) rather than a ViewModel: it carries no logic, has no injected
 * dependencies, and need not survive configuration change, since every screen re-publishes when it
 * re-enters composition. Do not reintroduce a `UiEvent`/`onEvent` reducer over it.
 *
 * Screens publish from a single `DisposableEffect`:
 *
 * ```
 * val chromeOwner = remember { Any() }
 * DisposableEffect(subtitle) {                       // key on the derived state the chrome captures
 *     appScaffoldState.setUiState(chromeOwner, AppScaffoldState.UiState(...))
 *     onDispose { appScaffoldState.clearUiState(chromeOwner) }
 * }
 * ```
 *
 * Key that effect on whatever derived state the published actions close over, not on `Unit`, or the
 * scaffold keeps stale lambdas — see `VisitDetailScreen`, which keys on its "Draft" subtitle.
 */
@Stable
class AppScaffoldState {
    var uiState: UiState by mutableStateOf(UiState())
        private set

    // The token identifies the screen that currently owns the chrome, so a screen
    // leaving composition only clears chrome it still owns. Without this, the exiting
    // screen's onDispose (which runs after the entering screen has already published)
    // would wipe the new screen's chrome.
    private var currentOwner: Any? = null

    /** Publishes [uiState] as a whole and records [owner] as the current chrome owner. */
    fun setUiState(owner: Any, uiState: UiState) {
        currentOwner = owner
        this.uiState = uiState
    }

    /**
     * Resets the chrome, but only if [owner] still owns it — this guard is what makes set/clear
     * order-independent across a navigation transition. Do not reduce it to an unconditional clear.
     */
    fun clearUiState(owner: Any) {
        if (currentOwner === owner) {
            currentOwner = null
            uiState = UiState()
        }
    }

    /**
     * The whole of a screen's chrome, published atomically. The five action lists stay distinct
     * types because each renders in a different slot — do not merge them into one generic list.
     */
    data class UiState(
        val topNavigationActions: List<TopNavigationAction> = emptyList(),
        val topBarActions: List<TopBarAction> = emptyList(),
        val topMenuActions: List<TopMenuAction> = emptyList(),
        val detailFooterActions: List<DetailFooterAction> = emptyList(),
        val floatingActionButtonActions: List<FloatingActionButtonAction> = emptyList(),
        // Optional accent-styled supporting line shown under the top bar title that any
        // screen can set (e.g. to surface a status like "Draft").
        val subtitle: String? = null,
    )
}
