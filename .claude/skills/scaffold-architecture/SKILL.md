---
name: scaffold-architecture
description: Use when working on app navigation chrome — top bar, bottom nav, FAB, detail footer — or when a Compose screen needs to publish actions to the app-level Scaffold
---

# Single app-level Scaffold with route-driven chrome

## One Scaffold at the root

There is exactly **one Scaffold**, at the NavHost/root level — not one per screen. Screens are plain `@Composable`s that fill the content slot. This avoids TopBar/BottomNav flicker and recomposition on navigation (follows Google's Now in Android pattern).

```
AppNavHost (root)
└── Scaffold  ← single, app-level
    ├── topBar    = { AppTopBar(currentScreen) }
    ├── bottomBar = { if (showBottomNav) AppBottomNav() else DetailFooter(...) }
    ├── floatingActionButton = { if (showFab) AppFab() }
    └── content   = { NavHost(...) }
```

Chrome visibility is route-driven, read from `navController.currentBackStackEntryAsState()`:
`showBottomNav = currentRoute in bottomNavRoutes`, `showFab = currentRoute == Routes.HOME`, etc.

Detail screens (e.g. `VisitDetailScreen`) also have **no own Scaffold** — the app Scaffold owns their chrome too. `VisitDetail` is excluded from the nav-bar route set so there's no tab-bar flash before its footer publishes. `ConversationDetailScreen` still has its own Scaffold (not yet migrated).

## Screens publish chrome via a shared state holder

When a screen needs to add TopBar actions or a detail footer, it publishes to a shared **`AppScaffoldState`** — a plain `remember { AppScaffoldState() }` holder, **NOT a ViewModel**. It is created in `Main` and threaded into screens via `navigationDependencies` (`dependency(appScaffoldState)`, exactly like `paddingValues`). State is Compose `mutableStateOf`, read directly as `appScaffoldState.uiState` — no `StateFlow`/`collectAsStateWithLifecycle`.

**Why a holder, not a ViewModel:** it's a zero-logic cross-screen chrome relay with no DI deps and no need to survive config change (each screen re-publishes on re-entry). It was a `@HiltViewModel` earlier; the `ViewModel` suffix mis-signalled its role. **Do NOT** add an `onEvent`/`UiEvent` reducer — that reintroduces the deleted `ScaffoldConfigurationChanged` indirection.

### Single `uiState` + owner-token guard (critical — do not "simplify")

`AppScaffoldState` exposes ONE `uiState: UiState`, where
`UiState(topBarActions: List<TopBarAction>, detailFooterActions: DetailFooterActions?)`.

A screen publishes its whole chrome atomically via `setUiState(owner, uiState)` and clears via `clearUiState(owner)`. Each screen uses `remember { Any() }` as its **owner token**; `clearUiState` only resets if the caller still owns the chrome.

**Why the owner token:** when two adjacent screens both publish chrome, the exiting screen's `DisposableEffect.onDispose` fires at the END of the nav transition — *after* the entering screen already published. A bare `clear()` would wipe the new screen's chrome. The owner check makes set/clear order-independent. **Do NOT reduce this to a bare `clear()`.**

### Types

- `TopBarAction(label: String, icon: ImageVector?, onClick: () -> Unit)`. The `onClick` lambda should capture only the screen's own ViewModel — no Composable-scoped refs (leak risk).
- `DetailFooterActions(onBack, onSave, onAdd)` — the footer is a fixed Back/Save/Add triad rendered by a bespoke FAB-containing component.
- Keep `TopBarAction` and `DetailFooterActions` as **distinct types** — do not merge into one generic `BarAction` list.
- Delete is a direct top-bar `TopBarAction` (trash icon), guarded by the delete-confirmation dialog.

Screens set actions in `LaunchedEffect` and clear in `DisposableEffect { onDispose { ... } }`. The Scaffold renders actions knowing only label + icon + onClick.
