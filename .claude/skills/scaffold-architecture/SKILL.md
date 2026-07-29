---
name: scaffold-architecture
description: Use when working on app navigation chrome — top bar, bottom nav, FAB, detail footer — or when a Compose screen needs to publish actions to the app-level Scaffold
---

# Single app-level Scaffold with route-driven chrome

## One Scaffold at the root

There is exactly **one Scaffold**, in `AppScaffold.kt`, hosted by `Main.kt` — not one per screen.
Screens are plain `@Composable`s that fill the content slot (`DestinationsNavHost`). This avoids
TopBar/BottomNav flicker and recomposition on navigation.

```
Main
└── AppScaffold          ← the single, app-level Scaffold
    ├── topBar    = TopAppBar (title + navigation + actions + overflow menu + subtitle)
    ├── bottomBar = BottomNavigation / FloatingBar (detail footer)
    ├── floatingActionButton
    └── content   = DestinationsNavHost(NavGraphs.root)
```

Chrome visibility is route-driven, from `navController.currentDestinationWithLifecycle()`
(`extension/NavControllerExtensions.kt`) — **not** `currentBackStackEntryAsState()`. `AppScaffold`
compares that destination against literal sets:

```kotlin
val showFAB = currentDestination in listOf(VisitListScreenDestination, ConversationListScreenDestination)
val showBottomNavigation = currentDestination in listOf(VisitListScreenDestination, ConversationListScreenDestination)
val showTopBar = currentDestination in listOf(/* those two + the detail and settings destinations */)
```

Detail screens have **no own Scaffold** — the app Scaffold owns their chrome too. They are excluded
from the bottom-nav destination set so there is no tab-bar flash before the footer publishes.
(Screen previews *do* wrap themselves in `AppScaffold` with a throwaway `remember { AppScaffoldState() }`
— that is preview-only scaffolding, not per-screen chrome.)

## Screens publish chrome via a shared state holder

A screen publishes to a shared **`AppScaffoldState`** (`util/scaffold/AppScaffoldState.kt`) — a plain
`remember { AppScaffoldState() }` holder created in `Main`, **NOT a ViewModel**. It is threaded into
screens through `di/NavigationDependencies.kt` via `dependency(appScaffoldState)`, on every screen
destination. State is Compose `mutableStateOf`, read directly as `appScaffoldState.uiState` — no
`StateFlow` / `collectAsStateWithLifecycle`.

**Why a holder, not a ViewModel:** it is a zero-logic cross-screen chrome relay with no DI deps and no
need to survive config change (each screen re-publishes on re-entry). It was a `@HiltViewModel` earlier;
the `ViewModel` suffix mis-signalled its role. **Do NOT** add an `onEvent`/`UiEvent` reducer — that
reintroduces the deleted `ScaffoldConfigurationChanged` indirection.

### Single `uiState` + owner-token guard (critical — do not "simplify")

`AppScaffoldState` exposes ONE `uiState`, carrying all five action lists plus an optional subtitle:

```kotlin
data class UiState(
    val topNavigationActions: List<TopNavigationAction> = emptyList(),
    val topBarActions: List<TopBarAction> = emptyList(),
    val topMenuActions: List<TopMenuAction> = emptyList(),
    val detailFooterActions: List<DetailFooterAction> = emptyList(),
    val floatingActionButtonActions: List<FloatingActionButtonAction> = emptyList(),
    val subtitle: String? = null,   // accent line under the title, e.g. "Draft"
)
```

A screen publishes its whole chrome atomically via `setUiState(owner, uiState)` and clears via
`clearUiState(owner)`, using `remember { Any() }` as its **owner token**. `clearUiState` only resets
if the caller still owns the chrome.

**Why the owner token:** when two adjacent screens both publish chrome, the exiting screen's
`onDispose` fires *after* the entering screen has already published. A bare `clear()` would wipe the
new screen's chrome. The owner check makes set/clear order-independent. **Do NOT reduce this to a
bare `clear()`.**

### Publishing pattern

One `DisposableEffect` does both set and clear:

```kotlin
val chromeOwner = remember { Any() }

DisposableEffect(subtitle) {                    // key on any derived state the chrome captures
    appScaffoldState.setUiState(
        owner = chromeOwner,
        uiState = AppScaffoldState.UiState(
            topNavigationActions = topNavigationActions,
            topBarActions = topBarActions,
            subtitle = subtitle
        )
    )
    onDispose { appScaffoldState.clearUiState(chromeOwner) }
}
```

**Key the effect on state the chrome depends on**, not `Unit`. `VisitDetailScreen` uses
`DisposableEffect(subtitle)` so the "Draft" subtitle re-publishes; a `Unit` key would pin a stale
subtitle and stale lambdas. Screens with static chrome (`VisitListScreen`, `SettingsScreen`)
correctly use `DisposableEffect(Unit)`.

### Action types (`util/scaffold/`)

Five **distinct** types — do not merge them into one generic `BarAction` list:

| Type | Fields | Rendered as |
|---|---|---|
| `TopNavigationAction` | `contentDescription`, `icon`, `onClick` | app-bar navigation slot (back arrow) |
| `TopBarAction` | `contentDescription`, `icon`, `onClick`, `menu: (@Composable () -> Unit)?` | app-bar action icons |
| `TopMenuAction` | `text`, `icon`, `onClick` | overflow (⋮) menu items |
| `DetailFooterAction` | `contentDescription`, `icon`, `isEnabled`, `onClick` | bottom `FloatingBar` on detail screens |
| `FloatingActionButtonAction` | `contentDescription`, `icon`, `onClick` | FAB |

There is no `label` field (`TopMenuAction` uses `text`, the rest use `contentDescription`), and `icon`
is non-nullable everywhere.

Shared builders exist for chrome every screen wires identically: `topNavigationActions(onNavigateUp)`
and `settingsTopMenuActions(onNavigateToSettings)`. Per-screen builders are private `@Composable`
functions next to the screen (e.g. `visitDetailTopBarActions`, `visitDetailFooterActions`).

`onClick` lambdas should dispatch to the screen's own `onEvent` / ViewModel — avoid capturing
Composable-scoped references (leak risk).

Concrete example — `VisitDetailScreen`: top bar = call / copy / **delete** (trash icon, guarded by the
delete-confirmation dialog); footer = undo (enabled only when drafts exist) + save; FAB = add visit.
