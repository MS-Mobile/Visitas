---
name: scaffold-architecture
description: Use when working on app navigation chrome — top bar, bottom nav, FAB, detail footer — or when a Compose screen needs to publish actions to the app-level Scaffold
---

# App chrome — decisions that are easy to undo by accident

How the mechanism works is documented where it lives: see the KDoc on
`util/scaffold/AppScaffoldState.kt` for the holder and the publishing pattern, `AppScaffold.kt` for
the route-driven visibility rules, and the five action types in `util/scaffold/`. **Read those first.**

This skill only records the things the code cannot tell you: choices that were made deliberately,
look like cruft, and have each been reverted or nearly reverted before.

## Do not give a screen its own Scaffold

There is exactly one Scaffold, at the root. Screens are plain composables that fill its content slot
and publish their chrome to `AppScaffoldState`. Per-screen Scaffolds were the original design and
caused top-bar/bottom-nav flicker and recomposition on every navigation.

Detail screens are deliberately excluded from the bottom-nav destination set in `AppScaffold`, so
there is no tab-bar flash before their footer publishes.

Previews are the exception: a screen preview wraps itself in `AppScaffold` with a throwaway
`remember { AppScaffoldState() }`. That is preview scaffolding, not per-screen chrome — don't "fix"
it by giving the real screen a Scaffold.

## Do not remove the owner token

`setUiState(owner, …)` / `clearUiState(owner)` take an owner token because the exiting screen's
`onDispose` runs *after* the entering screen has already published. An unconditional clear wipes the
new screen's chrome. This reads like redundant bookkeeping and is not.

## Do not turn the holder back into a ViewModel

`AppScaffoldState` was a `@HiltViewModel` once. It carries no logic, injects nothing, and doesn't
need to survive configuration change — every screen re-publishes on re-entry. The `ViewModel` suffix
mis-signalled all of that. Equally, **do not add an `onEvent`/`UiEvent` reducer** over it; that
restores the `ScaffoldConfigurationChanged` indirection that was deleted on purpose.

## Do not merge the action types

`TopNavigationAction`, `TopBarAction`, `TopMenuAction`, `DetailFooterAction` and
`FloatingActionButtonAction` are five separate types with overlapping fields, which invites
collapsing them into one `BarAction`. They render in five different slots with different affordances
(`DetailFooterAction` has `isEnabled`, `TopBarAction` has an optional `menu`, `TopMenuAction` shows
`text` rather than an icon description). Keep them distinct.

## Keep the publishing effect keyed on derived state

A screen whose chrome closes over changing state must key its `DisposableEffect` on that state, not
on `Unit`, or the scaffold holds stale lambdas. `VisitDetailScreen` keys on its "Draft" subtitle;
screens with static chrome correctly use `Unit`. Changing one to match the other is a real bug in
one direction and harmless noise in the other.
