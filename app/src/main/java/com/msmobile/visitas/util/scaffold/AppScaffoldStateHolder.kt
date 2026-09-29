package com.msmobile.visitas.util.scaffold

import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.NavKey

/**
 * Keeps one [AppScaffoldState] per destination, so the app-level `AppScaffold` shows the chrome of
 * the destination on top of the back stack rather than that of whichever screen composed last.
 *
 * A single shared state is not enough once screens compose out of order: during the predictive back
 * gesture the screen underneath enters composition and publishes its chrome while the top screen is
 * still the current destination, and if the gesture is cancelled it leaves again and clears it —
 * leaving the top screen without its back arrow and actions. With a state per destination, the
 * screen below only ever writes its own chrome, which is shown once the pop actually happens.
 *
 * Tabs holding the same key (e.g. `Settings`) share that key's state; [AppScaffoldState]'s owner
 * guard keeps their set/clear order-independent across a tab switch, as it does across a transition.
 */
@Stable
class AppScaffoldStateHolder {
    private val states = mutableMapOf<NavKey, AppScaffoldState>()

    /** The chrome state [destination] publishes to and the scaffold reads while it is current. */
    fun stateFor(destination: NavKey): AppScaffoldState {
        return states.getOrPut(destination) { AppScaffoldState() }
    }

    /** Drops the states of destinations no longer on any back stack, so the map stays bounded. */
    fun retainOnly(destinations: Collection<NavKey>) {
        states.keys.retainAll(destinations.toSet())
    }
}
