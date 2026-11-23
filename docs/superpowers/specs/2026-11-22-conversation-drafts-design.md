# Conversation Drafts: Design

**Date:** 2026-11-22
**Status:** Approved design, ready for implementation planning
**Area:** `ConversationDetailViewModel`, `ConversationListViewModel`, `conversation` table, `VisitDetailViewModel` (conversation suggestions), `BackupHandler`
**Precedent:** Visit drafts: #227, #240, #245, #263, #281, and `2026-07-07-discard-draft-snapshot-design.md`

## Problem

Conversation edits only reach the database when the user taps Save. Going back, switching tabs or the app being killed all lose them. Visits already solve this with auto-saved drafts, a snapshot of the last committed state, and Undo. Conversations should behave the same way.

The conversation screen is already partly prepared: `UiState.hasDrafts` exists and the footer has a disabled Undo button. That button is wired to `CancelClicked`, which just navigates up.

## Lessons carried over from the visit story

The visit feature reached its final shape over five PRs. Conversations start from that final shape:

- **Draft means "dirty", never "new"** (#263, #281). A row created in memory starts with `isDraft = false`. Auto-save marks it draft only once it differs from the baseline.
- **Snapshot at the committed→draft transition, from the live DB row** (#281). Snapshots are never touched when the screen loads. They are deleted on Save and consumed on Undo.
- **The live row's `isDraft` flag guards the snapshot.** Once a row is draft, later auto-save passes skip it, so the first committed snapshot is kept. This holds across process death.
- **Drafts are always visible in the list and sorted first** (#245).
- **Translations ship in the same PR** (#240 was a follow-up).

## Decisions

| # | Decision | Choice |
|---|----------|--------|
| 1 | Draft granularity | Per row (`isDraft` on `conversation`). The group is a draft if any non-removed row is a draft |
| 2 | Snapshot storage | New `conversation_snapshot` table embedding `Conversation`. Its `id` has a foreign key to `conversation(id)` with `ON DELETE CASCADE`, the same pattern as `householder_snapshot`, so every delete path cleans up snapshots automatically |
| 3 | Snapshot repository | A separate `ConversationSnapshotDao` and `ConversationSnapshotRepository` in `conversation/`. `SnapshotRepository` stays visit-only |
| 4 | Visit next-conversation suggestions | Hide rows that were never committed (draft with no snapshot). Committed rows with draft edits show their current draft text |
| 5 | "Draft" string | Rename `visit_draft` to `draft` in all 3 locales and reuse it on both screens |
| 6 | Removing a child row | Mirrors visits: `wasRemoved` is not editable data. Removing a row alone does not create a draft or enable Undo, and the removal is lost if the user leaves without saving |
| 7 | Undo of a never-committed group | Delete its rows and reset to a fresh empty form. Stay on the screen |
| 8 | Back navigation | Keeps the draft, as for visits |

## The parent row's two roles

Row 0 (`orderIndex == 0`, `conversationGroupId == null`) is both a conversation and the group's identity:

1. **On the first auto-save of a never-persisted group, row 0 is marked draft even if only a child changed.** This keeps "every row is draft with no snapshot" a reliable "never committed" signal, the same reason `Householder.isDraft` covers a new record whose user only edited a visit.
2. **The first auto-save sets the ViewModel's `conversationGroupId`** to row 0's id. Otherwise `deleteAccepted` returns early on a null id and the trash button does nothing for a new draft group. Undo also needs the id.

Auto-save also writes `conversationGroupId = row0.id` on every child, as `addOrUpdateConversations` does today. Without it, draft children of a new group would be orphaned.

## Operations

### ① Auto-save (debounced 250 ms, started in `viewCreated`)

On each emission that differs from the baseline (`Map<UUID, EditableConversationData>`, where the editable data is `question`, `response` and `orderIndex`):

1. For each row that is not removed and not already draft:
   - If it is new or changed, read the committed row with `getByIdOrNull`. If that row exists and is not draft, save a `ConversationSnapshot` of it. Then mark the row draft.
   - If it is row 0 and has no DB row, mark it draft (role 1).
2. Save every non-removed row with its group id assigned, then set `conversationGroupId` (role 2).
3. Rebase the baseline onto the state that now carries the draft flags.

### ② Undo (`UndoChangesClicked` → `UndoChangesConfirmation` dialog → `UndoChangesConfirmed`)

The confirmation flow and strings are reused from the visit screen. With `g = conversationGroupId`:

```
snapshots    = snapshotRepository.listByGroup(g)
committedIds = snapshots.ids
delete live rows of g where isDraft && id !in committedIds   // added this session, or never committed
save each snapshot.conversation                              // restore edited rows; an upsert, so no cascade
snapshotRepository.deleteByGroup(g)
remaining = listByIdOrGroupId(g)
if remaining.isEmpty(): conversationGroupId = null; reset to a fresh empty form
else: rebuild the UI from remaining
rebase the baseline
```

There is one data path for both cases. Only the final UI step branches. For a never-committed group, every row is draft with no snapshot, so all rows are deleted and `remaining` is empty.

### ③ Save

Delete removed rows, which cascades their snapshots. Save every row with `isDraft = false`. Delete the group's snapshots. Clear the in-memory draft flags. Rebase the baseline.

### Delete (trash)

Cancel the auto-save job first, so a pending debounced pass cannot re-insert rows after the delete. Then delete the **whole group** (`id = :g OR conversationGroupId = :g`). The snapshot foreign key cascades on its own.

Today `deleteById` removes only the parent row. Before auto-save that was mostly unreachable for new groups, because `conversationGroupId` stayed null until Save. With auto-save, trashing a typed new group would orphan its draft children. Once the list shows drafts, they would become undeletable, always-visible "Draft" cards. So the group delete ships in PR 1.

## Effects outside the conversation screens (ship with PR 1)

- **Visit suggestions:** `VisitDetailViewModel` switches from `conversationRepository.listAll()` to `listForSuggestions()`. The query is `isDraft = 0 OR id IN (SELECT id FROM conversation_snapshot)`. Without this, a half-typed new group shows up in suggestions within 250 ms, and a visit can save it as `nextConversationId` before it is discarded.
- **Backup restore:** `restoreDataFrom` must copy `conversation_snapshot` after `conversation`. Otherwise a restored committed group with pending draft edits has every edited row draft with no snapshot, and Undo deletes those rows. Householders and visits have the same hole today; that is tracked separately in #431. This feature fixes only its own table.

## Migration

**DB version 18 → 19**, `MIGRATION_18_19`:

- `ALTER TABLE conversation ADD COLUMN isDraft INTEGER NOT NULL DEFAULT 0`
- `CREATE TABLE conversation_snapshot (...)`, matching Room's `createSql` exactly, including the embedded `isDraft` column and the foreign key
- Register the migration and commit `app/schemas/.../19.json`, regenerated with `sh scripts/verify-room-schemas.sh --export-only`. `assembleDebug` alone does not export schemas.
- `VisitasDatabase.kt` changes trigger the pre-commit `BackupHandlerTest` hook, which needs a connected device.

`ConversationDao.save` must stay `@Upsert`. A `REPLACE` insert would delete the row first and cascade-wipe its snapshot.

## Out of scope

- **Trash on a never-edited new form leaves the screen stuck in `Deleting`**, because `deleteAccepted` returns early on a null id. This predates the feature. After this change it only affects a form with no edits.
- Snapshot restore for householders and visits (#431).
