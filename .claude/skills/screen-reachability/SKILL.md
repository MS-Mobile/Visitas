---
name: screen-reachability
description: Use when adding a new screen/destination and needing to make it actually reachable — as a bottom-nav tab, from the FAB, or from a menu — beyond just registering its ViewModel
---

# Making a screen reachable

Navigation is **Compose Destinations** (KSP-generated `NavGraphs`). Annotating a screen `@Destination<RootGraph>` and registering its ViewModel in `di/NavigationDependencies.kt` (see AGENTS.md) makes the destination **buildable but not reachable**. To make it reachable, pick one entry surface and also update the app-level chrome lists.

## Entry surfaces (pick one)

**Bottom-nav tab** — add an entry to the `BottomNavigationTab` enum in `ui/views/BottomNavigation.kt`:
```kotlin
Tags(TagListScreenDestination, Icons.Rounded.Label, R.string.tags),
```

**FAB → detail** — add the list→detail mapping in `MainActivityViewModel.asFabDestination`:
```kotlin
is TagListScreenDestination -> TagDetailScreenDestination
```

**From a menu / another screen** — navigate with the generated `TagListScreenDestination` via `DestinationsNavigator` (mirror how `settingsTopMenuActions` reaches `SettingsScreenDestination`).

## Always update the chrome lists in `AppScaffold.kt`

The app-level Scaffold gates chrome by destination (see the `scaffold-architecture` skill). Add your destination to the relevant lists:

- `showTopBar` — if the screen has the app top bar.
- `showBottomNavigation` — if it's a tab.
- `showFAB` — if it shows the FAB.
- the `title` `when (currentDestination)` — add a branch, else it falls back to `app_name`.

## Gotchas

- A tab **without** a detail screen still hits `showFAB` logic — either exclude it from `showFAB` and use an on-screen add button, or give it a detail destination and an `asFabDestination` mapping.
- Add a **feature-prefixed** string resource for the title/label (avoid generic keys like `tags`) — see `parallel-feature-work` for why.
- These files (`AppScaffold.kt`, `BottomNavigation.kt`, `MainActivityViewModel.kt`, `NavigationDependencies.kt`) are shared collision hotspots — see `parallel-feature-work`.
