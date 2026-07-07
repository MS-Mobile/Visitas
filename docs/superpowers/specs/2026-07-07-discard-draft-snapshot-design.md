# Discard-Draft Snapshot: Consolidated Flow

**Date:** 2026-07-07
**Status:** Approved design, ready for implementation planning
**Area:** `VisitDetailViewModel`, snapshot persistence, `Householder` entity

## Problem

The "discard draft" feature needs exactly two conceptual operations:

1. **Save a snapshot** — the last-committed copy of a record, captured right before edits overwrite it.
2. **Restore a draft** — put the record back using snapshot data.

The original mental model attached snapshot work to **preview / view-load** (load a snapshot on preview, save one if the record is non-draft, remove one on save). That forces a web of defensive branches — "there's no snapshot to load," "there's nothing to remove," "save a snapshot because it isn't a draft" — every one of which is a code path that can drift out of sync.

The goal is to collapse this to the two operations above with no preview-time snapshot logic and a single restore branch.

## Core insight

Attach snapshot work to the **only two moments that change committed state**, and never touch snapshots on preview:

- A snapshot is captured at the **committed → draft transition** — the first time an entity is dirtied — sourced from the still-committed live DB row.
- A snapshot is consumed on **discard** (restore) and deleted on **manual save** (commit).

The `isDraft` flag on the live row is the guard. Once it flips to `true`, later autosave passes skip the entity, so no "does a snapshot already exist?" check is needed, and reopening an already-draft record produces no transition (hence no re-snapshot). This holds across process death because the guard reads durable DB state.

## Decisions

| # | Decision | Choice |
|---|----------|--------|
| 1 | Where to capture the snapshot | At the committed→draft transition inside autosave; no preview-time logic |
| 2 | Draft granularity | Aggregate: **householder OR any visit** dirty ⇒ draft. `isDraft` becomes an explicit column on `Householder`, symmetric with `Visit` |
| 3 | Birth state of new records | `isDraft = false`. **Draft always means "dirty."** Never a birth property |
| 4 | Discard of a brand-new record | Delete the draft row, **reset to a fresh empty form, stay on screen** |

Decision 4 unifies both discard paths under one terminal rule: *return the screen to the state it had when the session opened; never dismiss*.

## State model

- Add `val isDraft: Boolean = false` to the `Householder` entity.
- Birth states (consistent with `Visit` today, after Decision 3):
  - Loaded from DB → `isDraft` reflects the stored column (committed record → `false`).
  - Brand-new (`newHouseholder()`, `newVisit()`) → `isDraft = false`.
- `hasDrafts = householder.isDraft || visitList.any { !it.wasRemoved && it.isDraft }`.

The live row's `isDraft` flag is the single source of truth for "dirtied since last commit" and doubles as the snapshot guard.

## The three operations

### ① Save snapshot — at the committed→draft transition, per entity, once

Inside the autosave pass, **before** `saveDraftSilently` overwrites the live rows:

For each entity that (a) differs from `initialEditableData` (the committed baseline) and (b) currently has `isDraft == false`:

1. Read the entity's **live DB row**. It still holds committed data at this instant (we snapshot before the overwrite).
2. **Snapshot only if that row exists and is `isDraft == false`** (a real committed row). A brand-new householder, or a visit added mid-session, has no committed DB row — so it is never snapshotted, which is correct: there is nothing to restore it to.
3. Flip the entity's flag to draft in UI state, then let `saveDraftSilently` persist the draft.

Because the flag flips to `true`, every later pass skips the entity — the original committed snapshot is retained. Extend the existing `updateUiStateChangedVisitsAsDraft` logic to also cover the householder.

### ② Restore — on discard only (`undoChangesConfirmed`)

Read `householder_snapshot` + this householder's `visit_snapshot`s, then branch on a single condition:

```
if (householder.isDraft && householderSnapshot == null):   // brand-new, never committed
    householderRepository.deleteById(id)                   // cascades visits + snapshots
    reinitializeEmptyForm()                                // == viewCreated(null) path
else:                                                       // committed record edited
    householderSnapshot?.let { householderRepository.save(it.householder) }
    val committedVisitIds = visitSnapshots.map { it.visit.id }
    visitRepository.deleteBulk(liveVisitIds - committedVisitIds)  // visits added this session
    visitSnapshots.forEach { visitRepository.save(it.visit) }     // restore edited/committed visits
    snapshotRepo.deleteHouseholderSnapshot(id)
    snapshotRepo.deleteVisitSnapshots(id)
    rebuildUiFromDb(id)
// both paths converge: reset initialEditableData baseline, hasDrafts = false, stay on screen
```

`householder.isDraft && householderSnapshot == null` is the only real branch — it distinguishes "new record → delete + reset" from "committed record → restore" using durable DB state alone (no in-memory `wasExisting` flag), so it is correct after process death. When the householder was committed but never edited (`isDraft == false`, no snapshot), it is left as-is while visits are restored.

### ③ Delete snapshot — on manual save (`performSave`)

After the save commits, the committed state is the new truth:

- `snapshotRepo.deleteHouseholderSnapshot(id)` and `deleteVisitSnapshots(id)`.
- Finalize `isDraft = false` on both the householder and its visits (visits already do this via `finalized()`; add the equivalent for the householder).

### Preview / view-load

Touches snapshots **not at all**. Every "there's none / nothing to remove" branch is gone by construction.

## Edges (confirmed handled)

- **Calendar events** — `saveDraftSilently` never calls `syncVisitCalendarEvent`; calendar sync happens only in `performSave`. Drafts never create calendar events, so discard needs no calendar cleanup.
- **Visits added this session** — absent from the snapshot ⇒ deleted on restore.
- **Removed visits** (`wasRemoved`) mid-session — still live rows until a manual save; discard restores from snapshot and re-adds them, which is correct.
- **Partial-draft aggregate across sessions** — per-entity snapshots mean a householder edited in session 2 is snapshotted independently of a visit edited in session 1; restore reassembles from whatever snapshots exist.

## Migration & touched files

**DB version 10 → 11**, new `MIGRATION_10_11`:

- `ALTER TABLE householder ADD COLUMN isDraft INTEGER NOT NULL DEFAULT 0`
- `ALTER TABLE householder_snapshot ADD COLUMN isDraft INTEGER NOT NULL DEFAULT 0` — the snapshot table embeds `Householder`, so Room expects the column there too (same reason `visit_snapshot` already carries `isDraft`).
- Register `MIGRATION_10_11` in `VisitasDatabase.MIGRATIONS`; bump `version = 11`.

**`Householder.kt`** — add `val isDraft: Boolean = false`.

**`SnapshotRepository`** — expose the DAO methods not yet surfaced: `getVisitSnapshots`, `deleteHouseholderSnapshot`, `deleteVisitSnapshots`.

**`VisitDetailViewModel`**:

- `newHouseholder()` / `newVisit()` → `isDraft = false`.
- `Householder.asState` / `asModel` and `HouseholderState` → carry `isDraft`; add a householder `finalized()`.
- `hasDrafts()` → include `householder.isDraft`.
- `startAutoSave` → insert the snapshot-on-transition step (①) before `saveDraftSilently`; extend `updateUiStateChangedVisitsAsDraft` to the householder.
- `undoChangesConfirmed` → implement restore (②), including `reinitializeEmptyForm()`.
- `performSave` → delete snapshots + finalize `isDraft = false` (③).
- Inject `SnapshotRepository` into the constructor (verify the binding in `ApplicationModule`).

## Out of scope

- Any change to how drafts are auto-saved beyond adding the snapshot-on-transition step.
- Calendar-event handling (unaffected — drafts don't create events).
- UI/Compose changes beyond what the existing `hasDrafts` / discard wiring already consumes.
