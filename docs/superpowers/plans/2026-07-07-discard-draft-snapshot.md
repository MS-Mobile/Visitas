# Discard-Draft Snapshot Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the "discard draft" feature as exactly two operations — capture a snapshot at the committed→draft transition, and restore from it on discard — with snapshot deletion on manual save and no preview-time snapshot logic.

**Architecture:** `isDraft` becomes an explicit column on `Householder` (symmetric with `Visit`), meaning "the row is uncommitted." The autosave loop captures a per-entity snapshot of the committed DB row the first time an entity is dirtied, reading the row via a nullable `getByIdOrNull` so it is robust to a prior manual save. Discard (`undoChangesConfirmed`) restores committed rows from snapshots (or deletes+resets a never-committed record); manual save (`performSave`) deletes snapshots and finalizes `isDraft = false`.

**Tech Stack:** Kotlin, Android, Room, Hilt, Kotlin coroutines/Flow, JUnit4 + Mockito-Kotlin (`org.mockito.kotlin`), `MainDispatcherRule` with `advanceUntilIdle()` for the debounced autosave.

**Reference spec:** `docs/superpowers/specs/2026-07-07-discard-draft-snapshot-design.md`

**Conventions from the existing codebase:**
- Run unit tests: `./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.visit.VisitDetailViewModelTest"`
- Compile only: `./gradlew :app:compileDebugKotlin`
- ViewModel tests live in `app/src/test/java/com/msmobile/visitas/visit/VisitDetailViewModelTest.kt` and build the ViewModel via the `createViewModel(...)` helper with Mockito mocks.
- The autosave debounces 250ms; drive it in tests with `mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()`.

---

## File Structure

| File | Responsibility | Change |
|------|----------------|--------|
| `app/.../householder/Householder.kt` | Householder entity | Add `isDraft` column |
| `app/.../migration/Migration_10_11.kt` | Schema migration 10→11 | Create |
| `app/.../VisitasDatabase.kt` | Room DB config | Register migration, bump version to 11 |
| `app/.../householder/HouseholderDao.kt` | Householder DAO | Add `getByIdOrNull` |
| `app/.../householder/HouseholderRepository.kt` | Householder repo | Add `getByIdOrNull` |
| `app/.../visit/VisitDao.kt` | Visit DAO | Add `getByIdOrNull` |
| `app/.../visit/VisitRepository.kt` | Visit repo | Add `getByIdOrNull` |
| `app/.../visit/SnapshotRepository.kt` | Snapshot repo | Expose get/delete methods |
| `app/.../visit/VisitDetailViewModel.kt` | Screen state + draft/snapshot logic | Inject `SnapshotRepository`; thread `isDraft` through householder; snapshot-on-transition; restore; delete-on-save |
| `app/src/test/.../visit/VisitDetailViewModelTest.kt` | ViewModel unit tests | Add `SnapshotRepository` mock + new tests |

---

## Task 1: Add `isDraft` to Householder + migration 10→11

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/householder/Householder.kt`
- Create: `app/src/main/java/com/msmobile/visitas/migration/Migration_10_11.kt`
- Modify: `app/src/main/java/com/msmobile/visitas/VisitasDatabase.kt`

There is no migration unit-test harness in this repo; Room validates the migrated schema against the entity definition when the DB is opened, so a mismatch fails at runtime. Verification for this task is a clean compile plus the existing suite staying green.

- [ ] **Step 1: Add the column to the entity**

In `Householder.kt`, add `isDraft` as the last field (default `false`, matching `Visit`):

```kotlin
@Entity(tableName = "householder")
data class Householder(
    @PrimaryKey val id: UUID,
    val name: String,
    val address: String,
    val notes: String?,
    val addressLatitude: Double? = null,
    val addressLongitude: Double? = null,
    val preferredDay: VisitPreferredDay = VisitPreferredDay.ANY,
    val preferredTime: VisitPreferredTime = VisitPreferredTime.ANY,
    val isDraft: Boolean = false
)
```

- [ ] **Step 2: Create the migration**

`Migration_10_11.kt` adds the column to both the live table and its snapshot mirror (the snapshot table embeds `Householder`, so Room expects the column there too — the same reason `visit_snapshot` already carries `isDraft`):

```kotlin
package com.msmobile.visitas.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds the `isDraft` column to `householder` and its snapshot mirror `householder_snapshot`.
 * `isDraft` marks a householder row as uncommitted (a persisted draft), symmetric with the
 * column already present on `visit` / `visit_snapshot`. Existing rows are committed data, so
 * they default to 0 (false).
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `householder` ADD COLUMN `isDraft` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `householder_snapshot` ADD COLUMN `isDraft` INTEGER NOT NULL DEFAULT 0")
    }
}
```

- [ ] **Step 3: Register the migration and bump the version**

In `VisitasDatabase.kt`, add the import, append `MIGRATION_10_11` to the `MIGRATIONS` array, and change `version = 10` to `version = 11`.

Add near the other migration imports:
```kotlin
import com.msmobile.visitas.migration.MIGRATION_10_11
```

Change the annotation:
```kotlin
    version = 11
```

Extend the array:
```kotlin
        private val MIGRATIONS = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11
        )
```

- [ ] **Step 4: Compile to verify Room accepts the schema**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. (A schema/entity mismatch would fail Room's annotation processing here.)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/householder/Householder.kt \
        app/src/main/java/com/msmobile/visitas/migration/Migration_10_11.kt \
        app/src/main/java/com/msmobile/visitas/VisitasDatabase.kt
git commit -m "Add isDraft column to Householder with migration 10->11"
```

---

## Task 2: Add nullable getters and expose snapshot get/delete

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/householder/HouseholderDao.kt`
- Modify: `app/src/main/java/com/msmobile/visitas/householder/HouseholderRepository.kt`
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitDao.kt`
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitRepository.kt`
- Modify: `app/src/main/java/com/msmobile/visitas/visit/SnapshotRepository.kt`

Pure passthrough plumbing with no branching logic; verified by compile. Behavior is exercised in Tasks 4–6.

- [ ] **Step 1: Add `getByIdOrNull` to `HouseholderDao`**

Add this query (the existing `getById` returns non-null and throws for a missing row; the snapshot guard needs to detect "no committed row"):

```kotlin
    @Query("SELECT * FROM Householder WHERE id = :id")
    suspend fun getByIdOrNull(id: UUID): Householder?
```

- [ ] **Step 2: Expose it on `HouseholderRepository`**

```kotlin
    suspend fun getByIdOrNull(id: UUID): Householder? {
        return householderDao.getByIdOrNull(id)
    }
```

- [ ] **Step 3: Add `getByIdOrNull` to `VisitDao`**

```kotlin
    @Query("SELECT * FROM visit WHERE id = :id")
    suspend fun getByIdOrNull(id: UUID): Visit?
```

- [ ] **Step 4: Expose it on `VisitRepository`**

```kotlin
    suspend fun getByIdOrNull(id: UUID): Visit? {
        return visitDao.getByIdOrNull(id)
    }
```

- [ ] **Step 5: Expose get/delete on `SnapshotRepository`**

The `SnapshotDao` already declares `getVisitSnapshots`, `deleteHouseholderSnapshot`, and `deleteVisitSnapshots`. Surface them:

```kotlin
package com.msmobile.visitas.visit

import com.msmobile.visitas.householder.HouseholderSnapshot
import java.util.UUID

class SnapshotRepository(private val snapshotDao: SnapshotDao) {
    suspend fun getHouseholderSnapshot(householderId: UUID): HouseholderSnapshot? {
        return snapshotDao.getHouseholderSnapshot(householderId)
    }

    suspend fun getVisitSnapshots(householderId: UUID): List<VisitSnapshot> {
        return snapshotDao.getVisitSnapshots(householderId)
    }

    suspend fun saveHouseholderSnapshot(householderSnapshot: HouseholderSnapshot) {
        snapshotDao.saveHouseholderSnapshot(householderSnapshot)
    }

    suspend fun saveVisitSnapshot(visitSnapshot: VisitSnapshot) {
        snapshotDao.saveVisitSnapshot(visitSnapshot)
    }

    suspend fun deleteHouseholderSnapshot(householderId: UUID) {
        snapshotDao.deleteHouseholderSnapshot(householderId)
    }

    suspend fun deleteVisitSnapshots(householderId: UUID) {
        snapshotDao.deleteVisitSnapshots(householderId)
    }
}
```

- [ ] **Step 6: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/householder/HouseholderDao.kt \
        app/src/main/java/com/msmobile/visitas/householder/HouseholderRepository.kt \
        app/src/main/java/com/msmobile/visitas/visit/VisitDao.kt \
        app/src/main/java/com/msmobile/visitas/visit/VisitRepository.kt \
        app/src/main/java/com/msmobile/visitas/visit/SnapshotRepository.kt
git commit -m "Add nullable getById and expose snapshot get/delete on repositories"
```

---

## Task 3: Thread `isDraft` through the ViewModel and inject `SnapshotRepository`

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitDetailViewModel.kt`
- Test: `app/src/test/java/com/msmobile/visitas/visit/VisitDetailViewModelTest.kt`

This task wires `isDraft` into `HouseholderState`, its mappers, and `hasDrafts`, and adds the `SnapshotRepository` constructor dependency (already provided by Hilt in `ApplicationModule`). It does not yet capture or restore snapshots — that's Tasks 4–6 — so `isDraft` on the householder is only set via the existing mappers here.

- [ ] **Step 1: Update the test helper to supply a `SnapshotRepository` mock and snapshot/committed-row stubs**

All stubbing is done inside `mock { ... }` blocks — mockito-kotlin's `on { ... }` handles `suspend` functions correctly there, whereas post-construction `whenever(mock).suspendFun()` does not compile. Later tests select behavior through new `createViewModel` parameters rather than re-stubbing.

Add the import at the top of `VisitDetailViewModelTest.kt`:

```kotlin
import com.msmobile.visitas.householder.HouseholderSnapshot
```

(`Householder`, `SnapshotRepository`, `VisitSnapshot`, and `Visit` are already imported / in scope for this test file.)

Add these parameters to the `createViewModel` signature (after `visitRepositoryRef`):

```kotlin
        snapshotRepositoryRef: MockReferenceHolder<SnapshotRepository>? = null,
        committedRowsExist: Boolean = true,
        householderIsDraft: Boolean = false,
        householderSnapshot: HouseholderSnapshot? = null,
        visitSnapshots: List<VisitSnapshot> = emptyList(),
```

Add an `isDraft` parameter to the `createHouseholder` helper (default `false`) and pass it to the `Householder(...)` it builds:

```kotlin
    private fun createHouseholder(
        latitude: Double? = null,
        longitude: Double? = null,
        preferredDay: VisitPreferredDay = VisitPreferredDay.ANY,
        preferredTime: VisitPreferredTime = VisitPreferredTime.ANY,
        notes: String = "Test Notes",
        isDraft: Boolean = false
    ): Householder {
        return Householder(
            id = HOUSEHOLDER_ID,
            name = "Test Name",
            address = "Test Address",
            notes = notes,
            addressLatitude = latitude,
            addressLongitude = longitude,
            preferredDay = preferredDay,
            preferredTime = preferredTime,
            isDraft = isDraft
        )
    }
```

Update the existing `householderRepository` `on { getById(any()) } doReturn createHouseholder(...)` stub to pass `isDraft = householderIsDraft` (so a loaded householder can be a draft). Then add the nullable getter to the same `mock<HouseholderRepository> { ... }` block (committed source is always `isDraft = false`):

```kotlin
            on { getByIdOrNull(any()) } doReturn if (committedRowsExist) {
                createHouseholder(
                    latitude = householderLatitude,
                    longitude = householderLongitude,
                    preferredDay = householderPreferredDay,
                    preferredTime = householderPreferredTime,
                    notes = householderNotes
                )
            } else {
                null
            }
```

Add to the `visitRepository` mock lambda:

```kotlin
            on { getByIdOrNull(any()) } doReturn if (committedRowsExist) createVisitList().first() else null
```

After the `visitRepository` mock block, add the `SnapshotRepository` mock:

```kotlin
        val snapshotRepository = mock<SnapshotRepository> {
            on { getHouseholderSnapshot(any()) } doReturn householderSnapshot
            on { getVisitSnapshots(any()) } doReturn visitSnapshots
        }
        snapshotRepositoryRef?.value = snapshotRepository
```

Pass the new dependency in the `VisitDetailViewModel(...)` constructor call (add after `visitRepository = visitRepository,`):

```kotlin
            snapshotRepository = snapshotRepository,
```

- [ ] **Step 2: Write a failing test — loading a draft householder makes `hasDrafts` true**

This exercises the `Householder.asState` mapping and the aggregate `hasDrafts` wiring without depending on autosave marking (which arrives in Task 4).

```kotlin
    @Test
    fun `hasDrafts is true when the loaded householder is a draft`() {
        // Arrange — the committed householder row is itself a draft (e.g. a reopened draft session)
        val viewModel = createViewModel(householderIsDraft = true)

        // Act
        viewModel.onEvent(VisitDetailViewModel.UiEvent.ViewCreated(householderId = HOUSEHOLDER_ID))

        // Assert
        assertTrue(viewModel.uiState.value.householder.isDraft)
        assertTrue(viewModel.uiState.value.hasDrafts)
    }
```

- [ ] **Step 3: Run it to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.visit.VisitDetailViewModelTest"`
Expected: FAIL — `HouseholderState` has no `isDraft` (compile error), and `viewCreated` does not fold the householder flag into `hasDrafts`.

- [ ] **Step 4: Add `isDraft` to `HouseholderState` and its accessors**

In `VisitDetailViewModel.kt`, add `isDraft` to `HouseholderState` (default `false`):

```kotlin
    data class HouseholderState(
        var id: UUID,
        val editable: EditableHouseholderData,
        val showClearName: Boolean,
        val showCopyData: Boolean,
        val addressState: HouseholderAddressState,
        val showClearNotes: Boolean,
        val isLoadingAddress: Boolean,
        val isNotesExpanded: Boolean,
        val isDraft: Boolean = false
    ) {
```

- [ ] **Step 5: Map `isDraft` in the householder mappers and `newHouseholder`**

In `Householder.asState`, carry the flag (add as the last argument of the `HouseholderState(...)` constructor):

```kotlin
                isLoadingAddress = false,
                isDraft = isDraft
```

In `HouseholderState.asModel`, map it back:

```kotlin
                preferredDay = preferredDay,
                preferredTime = preferredTime,
                isDraft = isDraft
```

`newHouseholder()` already omits `isDraft`, so it defaults to `false` (Decision 3: draft means dirty; new records are not born draft). Leave it as-is.

- [ ] **Step 6: Fold the householder flag into `hasDrafts` at load**

Leave the `List<VisitState>.hasDrafts()` extension unchanged (it still computes the visit part). In `viewCreated` (the existing-householder branch), replace `val hasDrafts = visitList.hasDrafts()` with a version that also reads the loaded householder flag:

```kotlin
            val hasDrafts = householder.isDraft || visitList.hasDrafts()
```

(The `householder` in that scope is the `HouseholderState` mapped from the DB, so its `isDraft` reflects the stored column. `updateUiStateChangedVisitsAsDraft` is left alone here — Task 4 replaces it entirely with householder-aware marking.)

- [ ] **Step 7: Add the `SnapshotRepository` constructor dependency**

Add the import near the other repository imports in `VisitDetailViewModel.kt`:

```kotlin
import com.msmobile.visitas.householder.HouseholderSnapshot
```

(`SnapshotRepository`, `VisitSnapshot`, and `Visit` are already in the `com.msmobile.visitas.visit` package — no import needed.)

Add the constructor parameter after `visitRepository`:

```kotlin
    private val visitRepository: VisitRepository,
    private val snapshotRepository: SnapshotRepository,
    private val conversationRepository: ConversationRepository,
```

Hilt already provides `SnapshotRepository` via `ApplicationModule.draftSnapshotRepository`, so no DI change is needed.

- [ ] **Step 8: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.visit.VisitDetailViewModelTest"`
Expected: PASS (the new test and all existing tests).

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/visit/VisitDetailViewModel.kt \
        app/src/test/java/com/msmobile/visitas/visit/VisitDetailViewModelTest.kt
git commit -m "Thread isDraft through householder state and inject SnapshotRepository"
```

---

## Task 4: Capture snapshots at the committed→draft transition

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitDetailViewModel.kt`
- Test: `app/src/test/java/com/msmobile/visitas/visit/VisitDetailViewModelTest.kt`

Replace `updateUiStateChangedVisitsAsDraft` with logic that, before persisting a draft, snapshots the committed DB row of each entity transitioning committed→draft, and marks householder + visits draft. Snapshot only when a committed (`isDraft == false`) row exists; a brand-new householder is still marked draft (no snapshot) so discard can detect "never committed".

- [ ] **Step 1: Write a failing test — editing an existing visit snapshots the committed visit**

```kotlin
    @Test
    fun `auto save snapshots the committed visit before marking it a draft`() {
        // Arrange
        val snapshotRepositoryRef = MockReferenceHolder<SnapshotRepository>()
        val viewModel = createViewModel(snapshotRepositoryRef = snapshotRepositoryRef)
        viewModel.onEvent(VisitDetailViewModel.UiEvent.ViewCreated(householderId = HOUSEHOLDER_ID))
        val visit = viewModel.uiState.value.visitList.first()

        // Act — edit the existing visit, then let the debounced auto-save run
        viewModel.onEvent(VisitDetailViewModel.UiEvent.VisitDoneChanged(value = true, visit = visit))
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        // Assert — the committed visit (isDone = false) was snapshotted
        val snapshotRepository = requireNotNull(snapshotRepositoryRef.value)
        val captor = org.mockito.kotlin.argumentCaptor<VisitSnapshot>()
        verifyBlocking(snapshotRepository) { saveVisitSnapshot(captor.capture()) }
        assertEquals(FIRST_VISIT_ID, captor.firstValue.visit.id)
        assertFalse(captor.firstValue.visit.isDone)
    }
```

- [ ] **Step 2: Write a failing test — editing the householder snapshots the committed householder**

```kotlin
    @Test
    fun `auto save snapshots the committed householder before marking it a draft`() {
        // Arrange
        val snapshotRepositoryRef = MockReferenceHolder<SnapshotRepository>()
        val viewModel = createViewModel(snapshotRepositoryRef = snapshotRepositoryRef)
        viewModel.onEvent(VisitDetailViewModel.UiEvent.ViewCreated(householderId = HOUSEHOLDER_ID))

        // Act
        viewModel.onEvent(VisitDetailViewModel.UiEvent.HouseholderNameChanged("Changed Name"))
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        // Assert — the committed householder ("Test Name") was snapshotted
        val snapshotRepository = requireNotNull(snapshotRepositoryRef.value)
        val captor = org.mockito.kotlin.argumentCaptor<HouseholderSnapshot>()
        verifyBlocking(snapshotRepository) { saveHouseholderSnapshot(captor.capture()) }
        assertEquals("Test Name", captor.firstValue.householder.name)
    }
```

- [ ] **Step 3: Write a failing test — a brand-new record is marked draft but not snapshotted**

For a new record, pass `committedRowsExist = false` so the nullable getters return `null` (no committed row):

```kotlin
    @Test
    fun `auto save on a brand-new record marks householder draft without snapshotting`() {
        // Arrange — a new record has no committed rows in the DB
        val snapshotRepositoryRef = MockReferenceHolder<SnapshotRepository>()
        val viewModel = createViewModel(
            snapshotRepositoryRef = snapshotRepositoryRef,
            committedRowsExist = false
        )
        viewModel.onEvent(VisitDetailViewModel.UiEvent.ViewCreated(householderId = null))

        // Act — edit only a householder field
        viewModel.onEvent(VisitDetailViewModel.UiEvent.HouseholderNameChanged("New Name"))
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        // Assert — marked draft, but no snapshot written
        assertTrue(viewModel.uiState.value.householder.isDraft)
        val snapshotRepository = requireNotNull(snapshotRepositoryRef.value)
        verifyBlocking(snapshotRepository, org.mockito.kotlin.never()) { saveHouseholderSnapshot(any()) }
    }
```

- [ ] **Step 4: Run the tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.visit.VisitDetailViewModelTest"`
Expected: FAIL — no snapshots are written yet.

- [ ] **Step 5: Implement the capture-and-mark step in the autosave**

In `VisitDetailViewModel.kt`, change the autosave `onEach` body to call a new suspend `captureSnapshotsAndMarkDrafts` (which both snapshots committed rows and marks drafts), replacing the call to `updateUiStateChangedVisitsAsDraft`:

```kotlin
    private fun startAutoSave() {
        if (autoSaveJob != null) return
        autoSaveJob = _uiState
            .debounce(250)
            .onEach { state ->
                val baseline = initialEditableData ?: return@onEach

                if (state.getEditableDataSnapshot() == baseline) {
                    return@onEach
                }

                captureSnapshotsAndMarkDrafts(baseline)

                val updatedState = _uiState.value
                saveDraftSilently(updatedState)
                // Rebase from the marked state so the draft flags we just set
                // don't count as a fresh change on the next emission.
                initialEditableData = updatedState.getEditableDataSnapshot()
            }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
    }
```

- [ ] **Step 6: Replace `updateUiStateChangedVisitsAsDraft` with `captureSnapshotsAndMarkDrafts`**

Delete `updateUiStateChangedVisitsAsDraft` and add:

```kotlin
    /**
     * For each entity dirtied relative to [baseline], snapshots its committed DB row (once) and
     * marks it a draft. The committed row is read via `getByIdOrNull` so this is robust to a prior
     * manual save. A brand-new record has no committed row: it is still marked draft (so discard can
     * detect "never committed") but is not snapshotted.
     */
    private suspend fun captureSnapshotsAndMarkDrafts(baseline: EditableDataSnapshot) {
        val state = _uiState.value

        // Householder
        val householder = state.householder
        if (!householder.isDraft) {
            val committed = householderRepository.getByIdOrNull(householder.id)
            val householderChanged = householder.editable != baseline.householder
            when {
                committed == null -> markHouseholderDraft()          // new record, nothing to snapshot
                householderChanged -> {                              // committed householder first dirtied
                    snapshotRepository.saveHouseholderSnapshot(HouseholderSnapshot(committed))
                    markHouseholderDraft()
                }
                // committed && unchanged -> leave isDraft = false
            }
        }

        // Visits
        state.visitList.forEach { visit ->
            if (visit.isDraft || visit.wasRemoved) return@forEach
            val baselineEditable = baseline.visits[visit.id]
            val isNewOrChanged = baselineEditable == null || baselineEditable != visit.editable
            if (!isNewOrChanged) return@forEach

            val committed = visitRepository.getByIdOrNull(visit.id)
            if (committed != null && !committed.isDraft) {
                snapshotRepository.saveVisitSnapshot(VisitSnapshot(committed))
            }
            markVisitDraft(visit.id)
        }
    }

    private fun markHouseholderDraft() {
        newState {
            copy(
                householder = householder.copy(isDraft = true),
                hasDrafts = true
            )
        }
    }

    private fun markVisitDraft(visitId: UUID) {
        newState {
            val updatedList = visitList.map { visit ->
                if (visit.id == visitId) visit.copy(isDraft = true) else visit
            }
            copy(
                visitList = updatedList,
                hasDrafts = householder.isDraft || updatedList.hasDrafts()
            )
        }
    }
```

- [ ] **Step 7: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.visit.VisitDetailViewModelTest"`
Expected: PASS (new tests plus the existing `auto save marks an edited existing visit as draft` and `... only householder changes` tests still pass — an unchanged visit is never marked, and a changed householder no longer leaves visits untouched incorrectly).

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/visit/VisitDetailViewModel.kt \
        app/src/test/java/com/msmobile/visitas/visit/VisitDetailViewModelTest.kt
git commit -m "Capture committed snapshot at the draft transition in autosave"
```

---

## Task 5: Restore on discard (`undoChangesConfirmed`)

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitDetailViewModel.kt`
- Test: `app/src/test/java/com/msmobile/visitas/visit/VisitDetailViewModelTest.kt`

Implement the single-branch restore: a never-committed record (`householder.isDraft && no householder snapshot`) is deleted and the form resets to empty; otherwise committed rows are restored from snapshots, session-added visits are deleted, snapshots are consumed, and the UI is rebuilt from the DB.

- [ ] **Step 1: Write a failing test — discarding a committed edit restores from snapshot**

```kotlin
    @Test
    fun `undo changes confirmed restores committed householder and visits from snapshot`() {
        // Arrange — a committed householder snapshot exists (name "Test Name").
        val committedSnapshot = HouseholderSnapshot(
            Householder(
                id = HOUSEHOLDER_ID,
                name = "Test Name",
                address = "Test Address",
                notes = "Test Notes"
            )
        )
        val snapshotRepositoryRef = MockReferenceHolder<SnapshotRepository>()
        val householderRepositoryRef = MockReferenceHolder<HouseholderRepository>()
        val viewModel = createViewModel(
            snapshotRepositoryRef = snapshotRepositoryRef,
            householderRepositoryRef = householderRepositoryRef,
            householderSnapshot = committedSnapshot
        )
        viewModel.onEvent(VisitDetailViewModel.UiEvent.ViewCreated(householderId = HOUSEHOLDER_ID))

        // Dirty the householder so it is a draft -> restore (not delete) branch on discard.
        viewModel.onEvent(VisitDetailViewModel.UiEvent.HouseholderNameChanged("Edited Name"))
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        // Act
        viewModel.onEvent(VisitDetailViewModel.UiEvent.UndoChangesConfirmed)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        // Assert — snapshot restored to the live table, snapshots consumed, UI reset
        val householderRepository = requireNotNull(householderRepositoryRef.value)
        val snapshotRepository = requireNotNull(snapshotRepositoryRef.value)
        val captor = org.mockito.kotlin.argumentCaptor<Householder>()
        // save() is called by both the draft autosave and the restore, so capture all calls.
        verifyBlocking(householderRepository, org.mockito.kotlin.atLeastOnce()) { save(captor.capture()) }
        assertTrue(captor.allValues.any { it.name == "Test Name" && !it.isDraft })
        verifyBlocking(snapshotRepository) { deleteHouseholderSnapshot(HOUSEHOLDER_ID) }
        verifyBlocking(snapshotRepository) { deleteVisitSnapshots(HOUSEHOLDER_ID) }
        assertFalse(viewModel.uiState.value.hasDrafts)
        assertEquals(VisitDetailViewModel.UiEventState.Idle, viewModel.uiState.value.eventState)
    }
```

Note: `rebuildUiFromDb` (Task 5 Step 5) calls `householderRepository.getById(HOUSEHOLDER_ID)`, already stubbed to return the default committed householder (name "Test Name"), so the restored UI shows the committed name.

- [ ] **Step 2: Write a failing test — discarding a brand-new record deletes it and resets the form**

```kotlin
    @Test
    fun `undo changes confirmed on a brand-new record deletes it and resets to empty form`() {
        // Arrange — new record: no committed rows (committedRowsExist = false), no snapshots (default)
        val householderRepositoryRef = MockReferenceHolder<HouseholderRepository>()
        val viewModel = createViewModel(
            householderRepositoryRef = householderRepositoryRef,
            committedRowsExist = false
        )
        viewModel.onEvent(VisitDetailViewModel.UiEvent.ViewCreated(householderId = null))
        viewModel.onEvent(VisitDetailViewModel.UiEvent.HouseholderNameChanged("New Name"))
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        val newId = viewModel.uiState.value.householder.id

        // Act
        viewModel.onEvent(VisitDetailViewModel.UiEvent.UndoChangesConfirmed)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        // Assert — draft row deleted, form reset to empty, still on screen (not dismissed)
        val householderRepository = requireNotNull(householderRepositoryRef.value)
        verifyBlocking(householderRepository) { deleteById(newId) }
        assertEquals("", viewModel.uiState.value.householder.name)
        assertFalse(viewModel.uiState.value.hasDrafts)
        assertEquals(1, viewModel.uiState.value.visitList.size)
        assertEquals(VisitDetailViewModel.UiEventState.Idle, viewModel.uiState.value.eventState)
    }
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.visit.VisitDetailViewModelTest"`
Expected: FAIL — `undoChangesConfirmed` is still the no-op stub.

- [ ] **Step 4: Implement `undoChangesConfirmed`**

Replace the stub:

```kotlin
    private fun undoChangesConfirmed() {
        val householderId = _uiState.value.householder.id
        val householderIsDraft = _uiState.value.householder.isDraft
        newState { copy(eventState = UiEventState.Idle) }
        viewModelScope.launch(dispatchers.io) {
            val householderSnapshot = snapshotRepository.getHouseholderSnapshot(householderId)

            if (householderIsDraft && householderSnapshot == null) {
                // Never committed -> nothing to restore to. Delete the draft (cascades visits +
                // snapshots) and reset to a fresh empty form (Decision 4: reset, do not dismiss).
                householderRepository.deleteById(householderId)
                reinitializeEmptyForm()
                return@launch
            }

            // Committed record: restore from snapshots.
            householderSnapshot?.let { householderRepository.save(it.householder) }

            val visitSnapshots = snapshotRepository.getVisitSnapshots(householderId)
            val committedVisitIds = visitSnapshots.map { it.visit.id }.toSet()
            // Delete ONLY visits added this session: draft in the DB and with no snapshot.
            // Untouched committed visits (not draft, no snapshot) must be preserved.
            val liveVisits = visitRepository.getAll(householderId)
            val addedVisitIds = liveVisits
                .filter { it.isDraft && it.id !in committedVisitIds }
                .map { it.id }
            visitRepository.deleteBulk(addedVisitIds)
            visitSnapshots.forEach { visitRepository.save(it.visit) }

            snapshotRepository.deleteHouseholderSnapshot(householderId)
            snapshotRepository.deleteVisitSnapshots(householderId)

            rebuildUiFromDb(householderId)
        }
    }
```

- [ ] **Step 5: Implement `reinitializeEmptyForm` and `rebuildUiFromDb`**

Add these helpers. `reinitializeEmptyForm` mirrors the `viewCreated(null)` success block; `rebuildUiFromDb` mirrors the existing-householder load and resets the baseline so the restored state is not seen as a fresh change:

```kotlin
    private fun reinitializeEmptyForm() {
        newState {
            copy(
                householder = newHouseholder(),
                visitList = listOf(newVisit(0)),
                eventState = UiEventState.Idle,
                hasDrafts = false
            )
        }
        initialEditableData = _uiState.value.getEditableDataSnapshot()
    }

    private suspend fun rebuildUiFromDb(householderId: UUID) {
        val householder = householderRepository.getById(householderId).asState
        val restoredVisits = visitRepository.getAll(householderId)
        val visitList = restoredVisits.map { visit ->
            val conversation = _uiState.value.conversationList.firstOrNull { conversation ->
                conversation.id == visit.nextConversationId
            }
            visit.asState(conversation)
        }
            .reindexIfNeeded()
            .revalidatePendingVisits(householder)
        newState {
            copy(
                householder = householder,
                visitList = visitList,
                eventState = UiEventState.Idle,
                hasDrafts = householder.isDraft || visitList.hasDrafts()
            )
        }
        initialEditableData = _uiState.value.getEditableDataSnapshot()
    }
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.visit.VisitDetailViewModelTest"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/visit/VisitDetailViewModel.kt \
        app/src/test/java/com/msmobile/visitas/visit/VisitDetailViewModelTest.kt
git commit -m "Restore committed state from snapshot on discard"
```

---

## Task 6: Delete snapshots and finalize on manual save (`performSave`)

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitDetailViewModel.kt`
- Test: `app/src/test/java/com/msmobile/visitas/visit/VisitDetailViewModelTest.kt`

After a successful manual save the committed state is the new truth: delete this householder's snapshots and finalize `isDraft = false` on the householder (visits already finalize via `finalized()`).

- [ ] **Step 1: Write a failing test — saving deletes snapshots and clears the householder draft flag**

```kotlin
    @Test
    fun `save deletes snapshots and finalizes householder draft flag`() {
        // Arrange
        val snapshotRepositoryRef = MockReferenceHolder<SnapshotRepository>()
        val viewModel = createViewModel(snapshotRepositoryRef = snapshotRepositoryRef)
        viewModel.onEvent(VisitDetailViewModel.UiEvent.ViewCreated(householderId = HOUSEHOLDER_ID))
        // Dirty the householder so it becomes a draft first.
        viewModel.onEvent(VisitDetailViewModel.UiEvent.HouseholderNameChanged("Edited Name"))
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.householder.isDraft)

        // Act — calendar permission is mocked false, but there are no pending visits requiring it
        //       after we mark the single visit done to avoid the calendar rationale path.
        val visit = viewModel.uiState.value.visitList.first()
        viewModel.onEvent(VisitDetailViewModel.UiEvent.VisitDoneChanged(value = true, visit = visit))
        viewModel.onEvent(VisitDetailViewModel.UiEvent.SaveClicked)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        // Assert
        val snapshotRepository = requireNotNull(snapshotRepositoryRef.value)
        verifyBlocking(snapshotRepository) { deleteHouseholderSnapshot(HOUSEHOLDER_ID) }
        verifyBlocking(snapshotRepository) { deleteVisitSnapshots(HOUSEHOLDER_ID) }
        assertFalse(viewModel.uiState.value.householder.isDraft)
    }
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.visit.VisitDetailViewModelTest"`
Expected: FAIL — `performSave` never deletes snapshots and leaves `householder.isDraft = true`.

- [ ] **Step 3: Add a householder `finalized()` and apply it in `performSave`**

Add the mapper next to the existing visit `finalized()`:

```kotlin
    private fun HouseholderState.finalized() = copy(isDraft = false)
```

In `performSave`, finalize the householder before saving, and delete snapshots after the writes. Update the body:

```kotlin
    private fun performSave() {
        newState {
            copy(
                eventState = UiEventState.Saving
            )
        }
        viewModelScope.launch(dispatchers.io) {
            val householderModel = _uiState.value.householder.finalized().asModel
            addOrUpdateHouseholder(householderModel)
            val householderId = householderModel.id
            val houseHolder = householderModel.asState
            deleteRemovedVisits(_uiState.value.visitList)
            val visitList = addOrUpdateVisits(
                householderId = householderId,
                householderName = householderModel.name,
                visitList = _uiState.value.visitList.map { it.finalized() }
            )

            snapshotRepository.deleteHouseholderSnapshot(householderId)
            snapshotRepository.deleteVisitSnapshots(householderId)

            newState {
                copy(
                    householder = houseHolder,
                    visitList = visitList,
                    eventState = UiEventState.SaveSucceeded
                )
            }
            // Rebase the autosave baseline to the committed state so a debounced emission landing
            // after this save does not re-dirty (and re-snapshot) the just-committed record.
            initialEditableData = _uiState.value.getEditableDataSnapshot()
        }
    }
```

(`householderModel.asState` produces a `HouseholderState` with `isDraft = false` because the model was finalized before mapping, so the emitted UI state clears the draft flag.)

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.visit.VisitDetailViewModelTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/visit/VisitDetailViewModel.kt \
        app/src/test/java/com/msmobile/visitas/visit/VisitDetailViewModelTest.kt
git commit -m "Delete snapshots and finalize draft flags on manual save"
```

---

## Task 7: Full-suite verification

**Files:** none (verification only)

- [ ] **Step 1: Run the complete unit-test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 2: Assemble the debug APK to confirm Room's schema validation and Hilt graph are intact**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Manual smoke checklist (device/emulator)**

Confirm end-to-end (the runtime surface this touches):
1. Open an existing householder, edit a field → the discard button enables (draft).
2. Press discard → confirm dialog → confirm → fields revert to the committed values, screen stays open.
3. Edit again and press Save → reopen → data persisted, discard disabled (no drafts).
4. Create a new householder, type a name (auto-saved as draft) → press discard → confirm → form resets to empty and stays open.
5. Open existing householder, add a new visit, edit the existing visit, then discard → the added visit disappears and the existing visit reverts.

---

## Self-Review

**Spec coverage:**
- Decision 1 (capture at transition) → Task 4.
- Decision 2 (aggregate `isDraft` on householder) → Tasks 1, 3.
- Decision 3 (draft = dirty, no birth drafts) → Task 3 Step 5 (`newHouseholder` stays `isDraft = false`).
- Decision 4 (discard new record = delete + reset form) → Task 5 (`reinitializeEmptyForm`).
- Operation ① (save snapshot) → Task 4; ② (restore) → Task 5; ③ (delete on save) → Task 6.
- Confirmation-gated discard → unchanged event flow; restore runs in `undoChangesConfirmed` (Task 5), dialog states left intact.
- Migration (10→11, both tables) → Task 1.
- `SnapshotRepository` exposure + `getByIdOrNull` → Task 2.
- Calendar untouched by drafts → no calendar code added in discard (verified: `saveDraftSilently` never syncs calendar).

**Type consistency:**
- `getByIdOrNull(id: UUID): Householder?` / `Visit?` used identically in Tasks 2 and 4.
- `captureSnapshotsAndMarkDrafts`, `markHouseholderDraft`, `markVisitDraft`, `reinitializeEmptyForm`, `rebuildUiFromDb`, `HouseholderState.finalized()` are each defined once and called with matching signatures.
- `HouseholderSnapshot(householder)` / `VisitSnapshot(visit)` constructors match the entities read in Task 1 and the DAO signatures.
- `hasDrafts = householder.isDraft || visitList.hasDrafts()` is the single aggregate rule, used in `viewCreated`, `markHouseholderDraft`/`markVisitDraft`, and `rebuildUiFromDb`.
