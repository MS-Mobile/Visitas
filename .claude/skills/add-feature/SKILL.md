---
name: add-feature
description: Use when adding a brand-new feature slice (its own entity, screens, and package) to the Visitas app — the ordered map of every touch-point so nothing is missed
---

# Adding a new feature slice

The full ordered set of touch-points for a new feature. For the per-file *shapes* (ViewModel/screen/test patterns) follow **AGENTS.md** — this skill is the map of everything you must touch, including the parts AGENTS.md scatters.

## 1. Create the feature package `com.msmobile.visitas.<feature>`

- `<Feature>.kt` — `@Entity(tableName = "<feature>")`, `@PrimaryKey val id: UUID` (UUIDs use the global `RoomUUIDConverter` — no per-field converter).
- `<Feature>Dao.kt` — `@Dao interface`, `suspend` fns, `@Upsert save(...)`, `@Query` reads.
- `<Feature>Repository.kt` — a **plain class** taking the DAO as a constructor arg (not `@Inject`/`@Singleton`).
- `<Feature>ListViewModel.kt` / `<Feature>DetailViewModel.kt` — `@HiltViewModel @Inject constructor`; MVVM/UDF shape per AGENTS.md.
- `<Feature>ListScreen.kt` / `<Feature>DetailScreen.kt` — `@Destination<RootGraph>(...)`; signature `(navigator, viewModel, appScaffoldState)`.
- `<Feature>...PreviewConfigProvider.kt` — for the screenshot test.

## 2. Register in Room (`VisitasDatabase.kt`)

- Add `<Feature>::class` to `@Database(entities = [...])` (or `views = [...]` for a `@DatabaseView`).
- Bump `version`.
- Add `abstract fun <feature>Dao(): <Feature>Dao`.
- Add `MIGRATION_N_M` (a `CREATE TABLE`) in `migration/` and to the `MIGRATIONS` array.
- Regenerate + commit the exported schema JSON. **Don't hand-write the `CREATE TABLE` DDL from memory** — add the entity, bump the version, run `sh scripts/verify-room-schemas.sh --export-only`, then copy the exact `createSql` from the generated `app/schemas/.../<version>.json` into the migration. (Schema-export mechanics + the KSP-cache gotcha: AGENTS.md.)

## 3. DI (`di/ApplicationModule.kt`) — two `@Provides` (easy to forget)

Repositories are **not** `@Inject constructor`, so Hilt needs both:
```kotlin
@Singleton @Provides fun <feature>Dao(db: VisitasDatabase): <Feature>Dao = db.<feature>Dao()
@Singleton @Provides fun <feature>Repository(dao: <Feature>Dao): <Feature>Repository = <Feature>Repository(dao)
```
Omitting the repository `@Provides` is a Hilt compile error, not a runtime one.

## 4. Wire the ViewModel to the screen (`di/NavigationDependencies.kt`)

```kotlin
destination(<Feature>ListScreenDestination) {
    dependency(hiltViewModel<<Feature>ListViewModel>())
    dependency(appScaffoldState)
}
```
This makes the destination buildable — **not reachable**.

## 5. Make it reachable

See the **`screen-reachability`** skill (tab / FAB / menu + the `AppScaffold` chrome lists).

## 6. Screenshot test

Add `<Feature>...ScreenshotTest.kt` in `app/src/screenshotTest/` + preview variants (AGENTS.md Screenshot Tests). Dispatch the Regenerate Screenshots workflow.

## Cross-feature data & concurrent work

- Needs data from another feature? → **`feature-decoupling`**.
- Another feature being built in parallel? → **`parallel-feature-work`** (DB version + migration numbering collisions).
