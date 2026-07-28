---
name: parallel-feature-work
description: Use when two or more features/entities are being developed at the same time (separate branches, worktrees, or agents) and you need to avoid collisions on shared registries and the Room DB version
---

# Parallel feature work

Each feature is a mostly-isolated package, but every feature touches a handful of **shared registries** and the **Room DB version**. Two features in flight will collide there. Plan for it.

## Highest risk: the Room DB version number

Both features bump `@Database(version = N)` to `N+1` and each adds `MIGRATION_N_(N+1)`. Git may auto-merge the text, but the result is **semantically broken** — you'd have two version-15 migrations and one declared version.

**Rule — land DB changes sequentially.** The second-to-land branch rebases and:
1. Takes the **next** version (`16`, not `15`).
2. Renames its migration to the correct contiguous pair (`MIGRATION_15_16`, file `Migration_15_16.kt`).
3. **Regenerates** the schema JSON — never hand-merge a generated `app/schemas/.../<version>.json`. Delete the conflicted file and re-run `sh scripts/verify-room-schemas.sh --export-only` (see `modify-entity` / AGENTS.md).

## Shared registries (mergeable, but plan additive edits)

Append on their own lines at the end of each list to minimize conflicts; resolve by keeping both:

- `VisitasDatabase.kt` — `entities`/`views` array, `MIGRATIONS` array, abstract-DAO block.
- `di/ApplicationModule.kt` — `@Provides` for DAO + repository.
- `di/NavigationDependencies.kt` — `destination(...)` blocks.
- `AppScaffold.kt` — `showFAB` / `showTopBar` / `showBottomNavigation` lists + the `title` `when`.
- `ui/views/BottomNavigation.kt` — `BottomNavigationTab` enum.
- `MainActivityViewModel.kt` — `asFabDestination` mapping.
- `res/values/strings.xml` — use **feature-prefixed** string keys (`tag_title`, not `title`) to avoid duplicate-key clashes.
- Screenshot reference PNGs — shared detail chrome (`DetailFooterAction`) means one change can shift several references; regenerate via the workflow, don't hand-edit.

## Not a concern

KSP-generated artifacts (`NavGraphs`, `VisitasDatabase_Impl`, Hilt registries) live under `build/` and aren't committed — no git conflict. A stale build cache after a merge can produce confusing errors; a clean rebuild fixes it.

## Isolation

Give each concurrent feature its own workspace so their uncommitted edits to shared files don't interleave — see the `superpowers:using-git-worktrees` skill.
