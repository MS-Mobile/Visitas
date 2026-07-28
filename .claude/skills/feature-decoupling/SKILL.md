---
name: feature-decoupling
description: Use when one feature package needs data or behavior owned by another (e.g. visit needs householder), when deciding where shared code goes, or when reviewing whether two features are too coupled
---

# Cross-feature boundaries

Code is organized one package per feature (`visit`, `householder`, `conversation`, `summary`, …). `AGENTS.md` documents the intra-feature MVVM shape but **not** what may cross a package boundary. This is that rule.

## The one invariant

**The data/domain layer crosses feature boundaries; the presentation layer never does.**

- **Entities** (`data class` like `Householder`, `Visit`) — cross freely; import them anywhere.
- **The owning feature's Repository** — cross by **injecting it** into your ViewModel (every repository is a `@Singleton` `@Provides` in the single `di/ApplicationModule.kt`, so no extra wiring). Example: `VisitDetailViewModel` injects `HouseholderRepository`, `ConversationRepository`, `PreferenceRepository`.
- **ViewModel / UiState / UiEvent / UiEventState** of another feature — **never**. They are effectively private to their feature.
- **Composable / `*Screen`** of another feature — **never**. Reusable UI lives in `ui/views/`.
- **Another feature's DAO** — avoid. Use its Repository, or add your own DAO / `@DatabaseView` in *your* package (see below).

## How to consume feature B from feature A — pick by shape

- **Need to write B's data, or read one record for a detail screen → inject B's Repository** into A's ViewModel, then **map B's entity into an A-local state type** at the VM edge. Never embed B's entity directly in A's `UiState`. (Default for detail/edit flows.)
- **Need a foreign field alongside every row of a list → add a `@DatabaseView` in A** that joins the two tables. `visit/VisitHouseholder` joins `visit` + `householder`, is owned by `visit` (registered under `views = [...]`, see AGENTS.md), and feeds `VisitListViewModel`. Prefer this over N per-row repository lookups. Editing a view's SQL forces a DB version bump — see `modify-entity`.
- **Pure read-model / aggregate over B's tables → add a DAO in A that queries B's table by name.** `summary/SummaryDao` does `SELECT … FROM Visit JOIN householder …` and owns no entity. Caveat: couples by **table-name string** — renaming B's table breaks A with no compiler error, only a Room-verification failure. Use sparingly.

## Multi-feature screens: compose at the nav layer, never VM→VM

When one screen needs several features, hand it **independent side-by-side ViewModels** in `di/NavigationDependencies.kt` (e.g. `VisitListScreenDestination` gets `VisitListViewModel` + `SummaryViewModel` + `BackupViewModel`). Each owns its own `StateFlow`; none references another. Do **not** make one ViewModel call another feature's ViewModel.

## Where shared code belongs

`util/` (cross-cutting services/providers), `extension/` (Kotlin extensions), `ui/theme/` + `ui/views/` (reusable Composables), `serialization/` (Moshi adapters), `di/ApplicationModule.kt` (the single DI module — no per-feature modules).

## Too-coupled smells

- A ViewModel importing another feature's ViewModel/UiState/UiEvent.
- A Composable importing another feature's `*Screen` (→ move to `ui/views/`).
- **Bidirectional** repository dependencies (A→B and B→A). Keep dependencies one-directional.
- Embedding a foreign entity in your `UiState` instead of mapping it to a local type.
