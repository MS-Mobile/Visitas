---
name: modify-entity
description: Use when adding or changing a field on an existing Room entity (e.g. a new column on Visit) — the ordered steps including migration, schema, backup, UI, and tests
---

# Modifying an existing entity

Ordered steps to add/change a field on an existing `@Entity`. Schema-export mechanics are in AGENTS.md; this is the end-to-end flow.

## Steps

1. **Add the field with a Kotlin default** in `<feature>/<Feature>.kt`, so existing call sites and backup/restore keep compiling. The Kotlin default and the migration's column default must agree. Precedent: `isDraft: Boolean = false`, `calendarEventId: Long? = null`.

2. **Bump the `@Database` version** in `VisitasDatabase.kt` (`14` → `15`). Multiple schema changes landing together on one branch share **one** version bump and **one** migration doing all the DDL — don't burn a version number per column.

3. **Write + register the migration** — new `migration/Migration_N_M.kt` matching `Migration_13_14.kt` style (anonymous `Migration(N, M)`, val `MIGRATION_N_M`, KDoc). Then append it to the `MIGRATIONS` array.
   ```kotlin
   db.execSQL("ALTER TABLE `visit` ADD COLUMN `phoneCallReminder` INTEGER NOT NULL DEFAULT 0")
   ```
   `Boolean` → `INTEGER NOT NULL DEFAULT 0`. For non-null `String`/enum columns, confirm the exact `DEFAULT` against Room's generated schema.

4. **Regenerate + commit the schema JSON** (`app/schemas/.../<version>.json`): `sh scripts/verify-room-schemas.sh --export-only` then `sh scripts/verify-room-schemas.sh`. The PR build fails on a stale/missing JSON. (KSP-cache caveat + Regenerate Room Schemas workflow: AGENTS.md.)

5. **Mirror/embedded tables — check for a second table to ALTER.** If the entity is `@Embedded` in another entity, that table has the same columns and needs the **same `ALTER` in the same migration**. `Visit` is embedded in `VisitSnapshot` (table `visit_snapshot`, the discard-draft copy), so a new `Visit` column means two `ALTER TABLE` statements — `visit` **and** `visit_snapshot` — plus the matching Kotlin default. Miss this and draft-discard/restore breaks.

6. **DAO — usually nothing.** `@Upsert` + `SELECT *` round-trip the new column automatically. Only touch a DAO if the field must be **projected or filtered**:
   - Shown in the list view → add it to the `VisitHouseholder` `@DatabaseView` SQL + data class. **A view is part of the schema hash, so this also forces the version bump** — fold it into the same bump.
   - Needed by `summary` → edit `SummaryDao`'s raw SQL (no schema change, but table-name-string coupled).

7. **Backup/restore — verify, usually no change.** `BackupHandler` copies whole rows, and restoring an older backup runs `MIGRATIONS` on it — which is exactly why step 3 must exist and be registered.

8. **UI plumbing** (only if user-visible) in `<Feature>DetailViewModel.kt`: add the field to the editable/state data class, thread it through the `asState`/`asModel` mappers, and if editable add a `UiEvent` + `onEvent` branch + the control in the screen.

9. **Tests** — update the affected unit VM test via the `createViewModel()` factory pattern; for UI changes **append a new screenshot variant** (never mutate shared preview state). Both per AGENTS.md.

## Gotcha: pre-commit hook needs a device

Editing `VisitasDatabase.kt` (which step 2 does) triggers `BackupHandlerTest` in the pre-commit hook, which **requires a connected device/emulator**. Have one attached when committing.

## Concurrent work

Two entity changes in flight collide on the DB version number and schema JSON — see **`parallel-feature-work`**.
