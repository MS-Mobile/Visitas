# Conversation Drafts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Auto-save conversation edits as drafts, snapshot the last committed version of each row, and let the user Undo back to it. This mirrors the visit drafts feature.

**Architecture:** `isDraft` is added as a column on `conversation`. A `conversation_snapshot` table embeds `Conversation`, and its `id` cascade-deletes with the live row. `ConversationDetailViewModel` gets the same debounced auto-save loop as `VisitDetailViewModel`:
- The first time a committed row is dirtied, it is snapshotted and marked draft.
- Undo deletes the draft rows that have no snapshot, then restores the snapshots.
- Save deletes the snapshots and clears the draft flags.

The list shows draft groups first, always visible, with a "Draft" label.

**Tech Stack:** Kotlin, Room, Hilt, coroutines/Flow, Jetpack Compose (Material 3), JUnit4 + mockito-kotlin, `MainDispatcherRule` with `advanceUntilIdle()`.

**Reference spec:** `docs/superpowers/specs/2026-11-22-conversation-drafts-design.md`

**Conventions:**
- Unit tests: `./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.conversation.*"`
- Compile only: `./gradlew :app:compileDebugKotlin`
- Schemas: `sh scripts/verify-room-schemas.sh --export-only`, then `sh scripts/verify-room-schemas.sh`
- In tests, mocks are configured **only** inside `createViewModel(...)` (no `whenever` in `@Test` methods), and every test class has `MainDispatcherRule`.
- Screenshot tests: add new variants at the **end** of a provider, never edit the shared preview state. After a UI change, dispatch the **Regenerate Screenshots** workflow for the branch.
- Staging `VisitasDatabase.kt` triggers the pre-commit `BackupHandlerTest` hook, which needs a connected device or emulator.

**PR split:** PR 1 = Tasks 1–10 (data layer, detail screen, suggestions, backup). PR 2 = Tasks 11–12 (list). Task 13 is a follow-up.

---

## File Structure

| File | Responsibility | Change |
|------|----------------|--------|
| `app/src/main/res/values*/strings.xml` (3 locales) | Strings | Rename `visit_draft` to `draft` |
| `visit/VisitDetailScreen.kt`, `visit/VisitListScreen.kt` | Visit UI | Use `R.string.draft` |
| `conversation/Conversation.kt` | Entity | Add `isDraft` |
| `conversation/ConversationSnapshot.kt` | Snapshot entity | Create |
| `migration/Migration_18_19.kt` | Migration | Create |
| `VisitasDatabase.kt` | DB config | Register entity, DAO, migration; version 19 |
| `app/schemas/com.msmobile.visitas.VisitasDatabase/19.json` | Exported schema | Generated |
| `conversation/ConversationDao.kt`, `ConversationRepository.kt` | Data access | `getByIdOrNull`, `deleteBulk`, `listForSuggestions` |
| `conversation/ConversationSnapshotDao.kt`, `ConversationSnapshotRepository.kt` | Snapshot access | Create |
| `di/ApplicationModule.kt` | DI | Provide snapshot DAO and repository |
| `conversation/ConversationDetailViewModel.kt` | Draft logic | Auto-save, snapshot, undo, save finalize |
| `conversation/ConversationDetailScreen.kt` | Detail UI | Undo dialog, Draft subtitle |
| `conversation/ConversationDetailPreviewConfigProvider.kt` | Previews | Append a "Draft" variant |
| `visit/VisitDetailViewModel.kt` | Suggestions | Use `listForSuggestions()` |
| `util/BackupHandler.kt` | Restore | Copy `conversation_snapshot` |
| `conversation/ConversationListViewModel.kt`, `ConversationListScreen.kt`, `ConversationListPreviewConfigProvider.kt` | List | Draft label, always shown, sorted first |
| Tests: `conversation/ConversationDetailViewModelTest.kt`, `ConversationListViewModelTest.kt`, `visit/VisitDetailViewModelTest.kt`, `androidTest/.../util/BackupHandlerTest.kt` | | Extend |

---

# PR 1: Data layer, detail screen, suggestions, backup

## Task 1: Rename `visit_draft` to `draft`

**Files:**
- Modify: `app/src/main/res/values/strings.xml:39`
- Modify: `app/src/main/res/values-b+es+419/strings.xml:38`
- Modify: `app/src/main/res/values-pt-rBR/strings.xml:37`
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitDetailScreen.kt:1356`
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitListScreen.kt:1041`

- [ ] **Step 1: Rename the key in all three locales**

```xml
<!-- values/strings.xml -->
<string name="draft">Draft</string>
<!-- values-b+es+419/strings.xml -->
<string name="draft">Borrador</string>
<!-- values-pt-rBR/strings.xml -->
<string name="draft">Rascunho</string>
```

- [ ] **Step 2: Update both usages**

In `VisitDetailScreen.kt`, replace `stringResource(R.string.visit_draft)` with `stringResource(R.string.draft)`. In `VisitListScreen.kt`, replace `stringResource(id = R.string.visit_draft)` with `stringResource(id = R.string.draft)`.

- [ ] **Step 3: Verify that no references remain and that it compiles**

Run: `git grep -n visit_draft -- app/src` → expected: no output.
Run: `./gradlew :app:compileDebugKotlin` → expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res app/src/main/java/com/msmobile/visitas/visit/VisitDetailScreen.kt app/src/main/java/com/msmobile/visitas/visit/VisitListScreen.kt
git commit -m "Rename visit_draft string to draft for reuse on conversations"
```

## Task 2: `isDraft` column, snapshot entity, migration 18→19

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/conversation/Conversation.kt`
- Create: `app/src/main/java/com/msmobile/visitas/conversation/ConversationSnapshot.kt`
- Create: `app/src/main/java/com/msmobile/visitas/migration/Migration_18_19.kt`
- Modify: `app/src/main/java/com/msmobile/visitas/VisitasDatabase.kt`
- Generated: `app/schemas/com.msmobile.visitas.VisitasDatabase/19.json`

- [ ] **Step 1: Add the column to the entity**

```kotlin
@Entity(tableName = "conversation")
data class Conversation(
    @PrimaryKey val id: UUID,
    val question: String,
    val response: String,
    val orderIndex: Int,
    val conversationGroupId: UUID?,
    val isDraft: Boolean = false
)
```

- [ ] **Step 2: Create the snapshot entity**

```kotlin
package com.msmobile.visitas.conversation

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey

/**
 * Last-committed copy of a conversation row, taken the first time an auto-save dirties it.
 * Undo restores from it and Save deletes it. The foreign key on the row's own id makes every
 * delete of the live row (trash, removed-row cleanup, undo of added rows) clean the snapshot up.
 * The live row must only ever be written with @Upsert: a REPLACE insert deletes first and would
 * cascade-wipe the snapshot.
 * The primary key is declared at table level because @PrimaryKey inside an @Embedded class is not
 * honored by Room.
 */
@Entity(
    tableName = "conversation_snapshot",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(
            entity = Conversation::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ConversationSnapshot(
    @Embedded val conversation: Conversation
)
```

- [ ] **Step 3: Create the migration**

```kotlin
package com.msmobile.visitas.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds conversation drafts: the `isDraft` flag on `conversation` and the `conversation_snapshot`
 * table that holds the last-committed copy of each dirtied row. The CREATE statement must match
 * the schema Room generates for [com.msmobile.visitas.conversation.ConversationSnapshot] exactly,
 * including the embedded `isDraft` column, or migration validation fails.
 */
val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `conversation` ADD COLUMN `isDraft` INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            """
            |CREATE TABLE IF NOT EXISTS `conversation_snapshot` (
            |    `id` TEXT NOT NULL,
            |    `question` TEXT NOT NULL,
            |    `response` TEXT NOT NULL,
            |    `orderIndex` INTEGER NOT NULL,
            |    `conversationGroupId` TEXT,
            |    `isDraft` INTEGER NOT NULL,
            |    PRIMARY KEY(`id`),
            |    FOREIGN KEY(`id`) REFERENCES `conversation`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            |)
            """.trimMargin()
        )
    }
}
```

- [ ] **Step 4: Register the migration in `VisitasDatabase.kt`**

- Add `ConversationSnapshot::class` to `entities`.
- Set `version = 19`.
- Add the import `com.msmobile.visitas.migration.MIGRATION_18_19` and append `MIGRATION_18_19` to `MIGRATIONS`.
- Add `abstract fun conversationSnapshotDao(): ConversationSnapshotDao`. That DAO is created in Task 3, so do Step 4 together with Task 3 Step 2 before compiling.

- [ ] **Step 5: Export and compare the schema** (after Task 3 compiles)

Run: `sh scripts/verify-room-schemas.sh --export-only` → expected: `19.json` is created.
In `19.json`, check that the `conversation_snapshot.createSql` columns, column order and foreign key match Step 3. If they don't, change the migration SQL, not the JSON.
Run: `sh scripts/verify-room-schemas.sh` → expected: pass.

- [ ] **Step 6: Commit** (Task 3 lands in the same commit, because the database references its DAO)

## Task 3: DAOs, repositories, DI

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/conversation/ConversationDao.kt`
- Modify: `app/src/main/java/com/msmobile/visitas/conversation/ConversationRepository.kt`
- Create: `app/src/main/java/com/msmobile/visitas/conversation/ConversationSnapshotDao.kt`
- Create: `app/src/main/java/com/msmobile/visitas/conversation/ConversationSnapshotRepository.kt`
- Modify: `app/src/main/java/com/msmobile/visitas/di/ApplicationModule.kt`

- [ ] **Step 1: Add the conversation queries**

```kotlin
// ConversationDao
@Query("SELECT * FROM conversation WHERE id = :id")
suspend fun getByIdOrNull(id: UUID): Conversation?

@Query("DELETE FROM conversation WHERE id IN (:idList)")
suspend fun deleteBulk(idList: List<UUID>)

/** Deletes the parent and every child. The snapshot FK cascades. */
@Query("DELETE FROM conversation WHERE id = :groupId OR conversationGroupId = :groupId")
suspend fun deleteGroup(groupId: UUID)

/** Everything except never-committed rows (draft with no snapshot), for visit suggestions. */
@Query(
    "SELECT * FROM conversation " +
        "WHERE isDraft = 0 OR id IN (SELECT id FROM conversation_snapshot) " +
        "ORDER BY question ASC"
)
suspend fun listForSuggestions(): List<Conversation>
```

```kotlin
// ConversationRepository
suspend fun getByIdOrNull(id: UUID): Conversation? = conversationDao.getByIdOrNull(id)

suspend fun deleteBulk(idList: List<UUID>) = conversationDao.deleteBulk(idList)

suspend fun deleteGroup(groupId: UUID) = conversationDao.deleteGroup(groupId)

suspend fun listForSuggestions(): List<Conversation> = conversationDao.listForSuggestions()
```

- [ ] **Step 2: Create the snapshot DAO and repository**

```kotlin
package com.msmobile.visitas.conversation

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import java.util.UUID

@Dao
interface ConversationSnapshotDao {
    @Upsert
    suspend fun save(snapshot: ConversationSnapshot)

    @Query("SELECT * FROM conversation_snapshot WHERE id = :groupId OR conversationGroupId = :groupId")
    suspend fun listByGroup(groupId: UUID): List<ConversationSnapshot>

    @Query("DELETE FROM conversation_snapshot WHERE id = :groupId OR conversationGroupId = :groupId")
    suspend fun deleteByGroup(groupId: UUID)

    @Query("SELECT * FROM conversation_snapshot")
    suspend fun listAll(): List<ConversationSnapshot>
}
```

```kotlin
package com.msmobile.visitas.conversation

import java.util.UUID

class ConversationSnapshotRepository(private val snapshotDao: ConversationSnapshotDao) {
    suspend fun save(snapshot: ConversationSnapshot) = snapshotDao.save(snapshot)

    suspend fun listByGroup(groupId: UUID): List<ConversationSnapshot> =
        snapshotDao.listByGroup(groupId)

    suspend fun deleteByGroup(groupId: UUID) = snapshotDao.deleteByGroup(groupId)
}
```

- [ ] **Step 3: Add the DI providers in `ApplicationModule.kt`**

Put the repository provider next to `conversationRepository` and the DAO provider next to `conversationDao`.

```kotlin
@Singleton
@Provides
fun conversationSnapshotRepository(
    conversationSnapshotDao: ConversationSnapshotDao
): ConversationSnapshotRepository {
    return ConversationSnapshotRepository(snapshotDao = conversationSnapshotDao)
}

@Singleton
@Provides
fun conversationSnapshotDao(roomDatabase: VisitasDatabase): ConversationSnapshotDao {
    return roomDatabase.conversationSnapshotDao()
}
```

- [ ] **Step 4: Compile, then finish Task 2 Step 5**

Run: `./gradlew :app:compileDebugKotlin` → expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit Tasks 2 and 3 together** (a connected device is needed for the hook)

```bash
git add app/src/main/java/com/msmobile/visitas/conversation app/src/main/java/com/msmobile/visitas/migration/Migration_18_19.kt app/src/main/java/com/msmobile/visitas/VisitasDatabase.kt app/src/main/java/com/msmobile/visitas/di/ApplicationModule.kt app/schemas/com.msmobile.visitas.VisitasDatabase/19.json
git commit -m "Add conversation isDraft column and conversation_snapshot table (DB 19)"
```

## Task 4: Carry `isDraft` through the detail state and make `hasDrafts` computed

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/conversation/ConversationDetailViewModel.kt`
- Test: `app/src/test/java/com/msmobile/visitas/conversation/ConversationDetailViewModelTest.kt`

- [ ] **Step 1: Replace the test factory**

The new factory takes the snapshot repository and per-test data. It replaces the existing `createViewModel` and keeps the two existing tests passing.

```kotlin
private fun createViewModel(
    conversationRepositoryRef: MockReferenceHolder<ConversationRepository>? = null,
    snapshotRepositoryRef: MockReferenceHolder<ConversationSnapshotRepository>? = null,
    // Consecutive results of listByIdOrGroupId: [load, undo live rows, undo remaining rows].
    listByGroupResults: List<List<Conversation>> = listOf(createConversationList()),
    committedConversations: List<Conversation> = createConversationList(),
    snapshots: List<ConversationSnapshot> = emptyList()
): ConversationDetailViewModel {
    val dispatchers = DispatcherProvider(io = mainDispatcherRule.dispatcher)
    val conversationRepository = mock<ConversationRepository> {
        on { listByIdOrGroupId(any()) }.doReturnConsecutively(listByGroupResults)
        on { getByIdOrNull(any()) } doAnswer { invocation ->
            val id = invocation.getArgument<UUID>(0)
            committedConversations.firstOrNull { conversation -> conversation.id == id }
        }
    }
    val snapshotRepository = mock<ConversationSnapshotRepository> {
        on { listByGroup(any()) } doReturn snapshots
    }
    val uuidProvider = mock<IdProvider> {
        on { generateId() }.doReturn(NEW_CONVERSATION_ID, NEW_CHILD_CONVERSATION_ID)
    }
    conversationRepositoryRef?.value = conversationRepository
    snapshotRepositoryRef?.value = snapshotRepository

    return ConversationDetailViewModel(
        dispatchers = dispatchers,
        conversationRepository = conversationRepository,
        snapshotRepository = snapshotRepository,
        uuidProvider = uuidProvider
    )
}
```

Add to `companion object`:

```kotlin
private val NEW_CHILD_CONVERSATION_ID = UUID.fromString("9e8d7c6b-5a4f-4e3d-8c2b-1a0f9e8d7c6b")
private val ADDED_CONVERSATION_ID = UUID.fromString("0a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d")
```

New imports: `org.mockito.kotlin.doAnswer`, `org.mockito.kotlin.doReturnConsecutively`, `org.mockito.kotlin.argThat`, `org.mockito.kotlin.never`, `org.mockito.kotlin.times`, `org.mockito.kotlin.verify`, `junit.framework.TestCase.assertTrue`, `junit.framework.TestCase.assertFalse`.

- [ ] **Step 2: Write the failing test**

```kotlin
@Test
fun `hasDrafts is true when a loaded conversation is a draft`() {
    // Arrange
    val loaded = createConversationList().mapIndexed { index, conversation ->
        if (index == 1) conversation.copy(isDraft = true) else conversation
    }
    val viewModel = createViewModel(listByGroupResults = listOf(loaded))

    // Act
    viewModel.onEvent(ConversationDetailViewModel.UiEvent.ViewCreated(FIRST_CONVERSATION_ID))

    // Assert
    assertTrue(viewModel.uiState.value.hasDrafts)
    assertTrue(viewModel.uiState.value.conversationList[1].isDraft)
}
```

- [ ] **Step 3: Run it and confirm it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.conversation.ConversationDetailViewModelTest"`
Expected: compilation fails because `snapshotRepository` and `isDraft` are unresolved.

- [ ] **Step 4: Implement**

- Add the constructor parameter `private val snapshotRepository: ConversationSnapshotRepository`, after `conversationRepository`.
- Add `val isDraft: Boolean = false` as the last property of `ConversationState`. The default keeps the preview providers compiling.
- Set `isDraft = isDraft` in both `Conversation.asState` and `ConversationState.asModel`.
- Replace `UiState`:

```kotlin
data class UiState(
    val conversationList: List<ConversationState>,
    val eventState: UiEventState
) {
    val hasDrafts: Boolean
        get() = conversationList.any { conversation -> !conversation.wasRemoved && conversation.isDraft }
}
```

- [ ] **Step 5: Run the tests**

Same command. Expected: all 3 pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/conversation/ConversationDetailViewModel.kt app/src/test/java/com/msmobile/visitas/conversation/ConversationDetailViewModelTest.kt
git commit -m "Carry isDraft through conversation detail state"
```

## Task 5: Auto-save with snapshot on the committed→draft transition

**Files:** the same two files as Task 4.

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test
fun `auto save marks an edited committed conversation as draft and snapshots it`() {
    // Arrange
    val snapshotRepositoryRef = MockReferenceHolder<ConversationSnapshotRepository>()
    val viewModel = createViewModel(snapshotRepositoryRef = snapshotRepositoryRef)
    viewModel.onEvent(ConversationDetailViewModel.UiEvent.ViewCreated(FIRST_CONVERSATION_ID))
    val second = viewModel.uiState.value.conversationList[1]

    // Act
    viewModel.onEvent(ConversationDetailViewModel.UiEvent.QuestionChanged(second, "Edited"))
    mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    // Assert
    val conversations = viewModel.uiState.value.conversationList
    assertFalse(conversations[0].isDraft)
    assertTrue(conversations[1].isDraft)
    val snapshotRepository = requireNotNull(snapshotRepositoryRef.value)
    verifyBlocking(snapshotRepository) {
        save(ConversationSnapshot(createConversationList()[1]))
    }
}

@Test
fun `auto save snapshots an edited conversation only once across passes`() {
    // Arrange
    val snapshotRepositoryRef = MockReferenceHolder<ConversationSnapshotRepository>()
    val viewModel = createViewModel(snapshotRepositoryRef = snapshotRepositoryRef)
    viewModel.onEvent(ConversationDetailViewModel.UiEvent.ViewCreated(FIRST_CONVERSATION_ID))

    // Act
    viewModel.onEvent(
        ConversationDetailViewModel.UiEvent.QuestionChanged(
            viewModel.uiState.value.conversationList[1], "Edit 1"
        )
    )
    mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
    viewModel.onEvent(
        ConversationDetailViewModel.UiEvent.QuestionChanged(
            viewModel.uiState.value.conversationList[1], "Edit 2"
        )
    )
    mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    // Assert
    val snapshotRepository = requireNotNull(snapshotRepositoryRef.value)
    verifyBlocking(snapshotRepository, times(1)) { save(any()) }
}

@Test
fun `auto save on a new group marks the parent draft, links children and sets no snapshot`() {
    // Arrange
    val conversationRepositoryRef = MockReferenceHolder<ConversationRepository>()
    val snapshotRepositoryRef = MockReferenceHolder<ConversationSnapshotRepository>()
    val viewModel = createViewModel(
        conversationRepositoryRef = conversationRepositoryRef,
        snapshotRepositoryRef = snapshotRepositoryRef,
        committedConversations = emptyList()
    )
    viewModel.onEvent(ConversationDetailViewModel.UiEvent.ViewCreated(null))
    viewModel.onEvent(ConversationDetailViewModel.UiEvent.AddClicked)
    val child = viewModel.uiState.value.conversationList[1]

    // Act: only the child changes
    viewModel.onEvent(ConversationDetailViewModel.UiEvent.QuestionChanged(child, "Child"))
    mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    // Assert
    val conversations = viewModel.uiState.value.conversationList
    assertTrue(conversations[0].isDraft)
    assertTrue(conversations[1].isDraft)
    val conversationRepository = requireNotNull(conversationRepositoryRef.value)
    verifyBlocking(conversationRepository) {
        save(argThat {
            id == NEW_CHILD_CONVERSATION_ID &&
                conversationGroupId == NEW_CONVERSATION_ID &&
                isDraft
        })
    }
    val snapshotRepository = requireNotNull(snapshotRepositoryRef.value)
    verifyBlocking(snapshotRepository, never()) { save(any()) }
}

@Test
fun `delete cancels a pending auto save`() {
    // Arrange
    val conversationRepositoryRef = MockReferenceHolder<ConversationRepository>()
    val viewModel = createViewModel(conversationRepositoryRef = conversationRepositoryRef)
    viewModel.onEvent(ConversationDetailViewModel.UiEvent.ViewCreated(FIRST_CONVERSATION_ID))
    viewModel.onEvent(
        ConversationDetailViewModel.UiEvent.QuestionChanged(
            viewModel.uiState.value.conversationList[1], "Edited"
        )
    )

    // Act: delete before the 250 ms debounce elapses
    viewModel.onEvent(ConversationDetailViewModel.UiEvent.DeleteAccepted)
    mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    // Assert
    val conversationRepository = requireNotNull(conversationRepositoryRef.value)
    verifyBlocking(conversationRepository, never()) { save(any()) }
}

@Test
fun `delete of an auto-saved new group deletes the whole group`() {
    // Arrange: before this feature a new group had no id until Save. Auto-save gives it one, and
    // deleting only the parent would orphan the draft children.
    val conversationRepositoryRef = MockReferenceHolder<ConversationRepository>()
    val viewModel = createViewModel(
        conversationRepositoryRef = conversationRepositoryRef,
        committedConversations = emptyList()
    )
    viewModel.onEvent(ConversationDetailViewModel.UiEvent.ViewCreated(null))
    viewModel.onEvent(ConversationDetailViewModel.UiEvent.AddClicked)
    viewModel.onEvent(
        ConversationDetailViewModel.UiEvent.QuestionChanged(
            viewModel.uiState.value.conversationList[1], "Child"
        )
    )
    mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    // Act
    viewModel.onEvent(ConversationDetailViewModel.UiEvent.DeleteAccepted)
    mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    // Assert
    val conversationRepository = requireNotNull(conversationRepositoryRef.value)
    verifyBlocking(conversationRepository) { deleteGroup(NEW_CONVERSATION_ID) }
    verifyBlocking(conversationRepository, never()) { deleteById(any()) }
    assertEquals(
        ConversationDetailViewModel.UiEventState.Deleted,
        viewModel.uiState.value.eventState
    )
}
```

- [ ] **Step 2: Run them and confirm they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.conversation.ConversationDetailViewModelTest"`
Expected: the 5 new tests FAIL (no auto-save or group delete yet).

- [ ] **Step 3: Implement**

Fields (next to `conversationGroupId`, which also becomes `@Volatile` because auto-save writes it on the IO dispatcher):

```kotlin
@Volatile private var conversationGroupId: UUID? = null
@Volatile private var initialEditableData: Map<UUID, EditableConversationData>? = null
private var autoSaveJob: Job? = null
```

Replace `viewCreated`:

```kotlin
private fun viewCreated(id: UUID?) {
    if (autoSaveJob != null || conversationGroupId != null) return
    if (id == null) {
        initialEditableData = _uiState.value.editableSnapshot()
        startAutoSave()
        return
    }
    conversationGroupId = id
    viewModelScope.launch(dispatchers.io) {
        val conversations = conversationRepository.listByIdOrGroupId(id).map { conversation ->
            conversation.asState
        }
        newState { copy(conversationList = conversations) }
        initialEditableData = _uiState.value.editableSnapshot()
        startAutoSave()
    }
}
```

Auto-save:

```kotlin
private fun startAutoSave() {
    if (autoSaveJob != null) return
    autoSaveJob = _uiState
        .debounce(250)
        .onEach { state ->
            val baseline = initialEditableData ?: return@onEach
            if (state.editableSnapshot() == baseline) return@onEach

            captureSnapshotsAndMarkDrafts(baseline)

            val updatedState = _uiState.value
            saveDraftSilently(updatedState)
            // Rebase from the marked state so the draft flags just set don't count as a change.
            initialEditableData = updatedState.editableSnapshot()
        }
        .flowOn(dispatchers.io)
        .launchIn(viewModelScope)
}

/**
 * Marks each dirtied row a draft, first snapshotting its committed DB row (once: rows already
 * draft are skipped). Row 0 of a never-persisted group is marked draft even when only a child
 * changed, so "every row draft with no snapshot" reliably means "never committed" on Undo.
 */
private suspend fun captureSnapshotsAndMarkDrafts(baseline: Map<UUID, EditableConversationData>) {
    _uiState.value.conversationList.forEachIndexed { index, conversation ->
        if (conversation.isDraft || conversation.wasRemoved) return@forEachIndexed
        val isNewOrChanged = baseline[conversation.id] != conversation.editable
        if (!isNewOrChanged && index != 0) return@forEachIndexed

        val committed = conversationRepository.getByIdOrNull(conversation.id)
        when {
            committed == null -> markDraft(conversation.id)   // never persisted: nothing to snapshot
            !isNewOrChanged -> Unit                           // committed parent, untouched
            else -> {
                if (!committed.isDraft) {
                    snapshotRepository.save(ConversationSnapshot(committed))
                }
                markDraft(conversation.id)
            }
        }
    }
}

private fun markDraft(conversationId: UUID) {
    newState {
        copy(conversationList = conversationList.map { conversation ->
            if (conversation.id == conversationId) conversation.copy(isDraft = true) else conversation
        })
    }
}

private suspend fun saveDraftSilently(state: UiState) {
    val groupId = state.conversationList.first().id
    state.conversationList.forEachIndexed { index, conversation ->
        if (conversation.wasRemoved) return@forEachIndexed
        conversationRepository.save(conversation.inGroup(index, groupId).asModel)
    }
    conversationGroupId = groupId
}
```

Helpers, placed with the other private extensions:

```kotlin
private val ConversationState.editable: EditableConversationData
    get() = EditableConversationData(question = question, response = response, orderIndex = orderIndex)

private fun UiState.editableSnapshot(): Map<UUID, EditableConversationData> =
    conversationList.associate { conversation -> conversation.id to conversation.editable }

private fun ConversationState.inGroup(index: Int, groupId: UUID): ConversationState =
    if (index == 0) this else copy(conversationGroupId = groupId)
```

At the bottom of the file, with the other nested classes:

```kotlin
private data class EditableConversationData(
    val question: String,
    val response: String,
    val orderIndex: Int
)
```

`addOrUpdateConversations` now delegates to the same helper:

```kotlin
private suspend fun addOrUpdateConversations(conversationList: List<ConversationState>) {
    val groupId = conversationList.first().id
    conversationList.forEachIndexed { index, conversation ->
        conversationRepository.save(conversation.inGroup(index, groupId).asModel)
    }
    conversationGroupId = groupId
}
```

In `deleteAccepted`, cancel auto-save first and delete the whole group:

```kotlin
private fun deleteAccepted() {
    autoSaveJob?.cancel()
    newState { copy(eventState = UiEventState.Deleting) }
    viewModelScope.launch(dispatchers.io) {
        val id = conversationGroupId ?: return@launch
        conversationRepository.deleteGroup(id)
        conversationGroupId = null
        newState { copy(eventState = UiEventState.Deleted) }
    }
}
```

Imports: `kotlinx.coroutines.Job`, `kotlinx.coroutines.flow.debounce`, `kotlinx.coroutines.flow.flowOn`, `kotlinx.coroutines.flow.launchIn`, `kotlinx.coroutines.flow.onEach`. If the compiler reports `debounce` as a `FlowPreview` API, copy the opt-in `VisitDetailViewModel` uses.

- [ ] **Step 4: Run the tests**

Same command. Expected: all pass, including the 2 original tests. The "does nothing when firstConversationId is null" test still passes because nothing changes, so no auto-save runs.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/conversation/ConversationDetailViewModel.kt app/src/test/java/com/msmobile/visitas/conversation/ConversationDetailViewModelTest.kt
git commit -m "Auto-save conversation drafts with snapshot on first edit"
```

## Task 6: Save clears drafts and deletes snapshots

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun `save finalizes drafts and deletes the group's snapshots`() {
    // Arrange
    val conversationRepositoryRef = MockReferenceHolder<ConversationRepository>()
    val snapshotRepositoryRef = MockReferenceHolder<ConversationSnapshotRepository>()
    val viewModel = createViewModel(
        conversationRepositoryRef = conversationRepositoryRef,
        snapshotRepositoryRef = snapshotRepositoryRef
    )
    viewModel.onEvent(ConversationDetailViewModel.UiEvent.ViewCreated(FIRST_CONVERSATION_ID))
    viewModel.onEvent(
        ConversationDetailViewModel.UiEvent.QuestionChanged(
            viewModel.uiState.value.conversationList[1], "Edited"
        )
    )
    mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    // Act
    viewModel.onEvent(ConversationDetailViewModel.UiEvent.SaveClicked)
    mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    // Assert
    assertFalse(viewModel.uiState.value.hasDrafts)
    val conversationRepository = requireNotNull(conversationRepositoryRef.value)
    verifyBlocking(conversationRepository) {
        save(argThat { id == SECOND_CONVERSATION_ID && question == "Edited" && !isDraft })
    }
    val snapshotRepository = requireNotNull(snapshotRepositoryRef.value)
    verifyBlocking(snapshotRepository) { deleteByGroup(FIRST_CONVERSATION_ID) }
}
```

- [ ] **Step 2: Run it** → expected: FAIL (`deleteByGroup` is never called, and the saved row is still draft).

- [ ] **Step 3: Implement.** Replace `saveClicked`:

```kotlin
private fun saveClicked() {
    newState { copy(eventState = UiEventState.Saving) }
    viewModelScope.launch(dispatchers.io) {
        deleteRemovedConversations(uiState.value.conversationList)   // cascades their snapshots
        val finalized = uiState.value.conversationList.map { conversation ->
            conversation.copy(isDraft = false)
        }
        addOrUpdateConversations(finalized)
        conversationGroupId?.let { groupId -> snapshotRepository.deleteByGroup(groupId) }
        newState { copy(conversationList = finalized, eventState = UiEventState.SaveComplete) }
        // Rebase so a debounced pass landing after the save doesn't re-dirty the committed rows.
        initialEditableData = _uiState.value.editableSnapshot()
    }
}
```

- [ ] **Step 4: Run the tests** → expected: all pass.

- [ ] **Step 5: Commit** with the message `"Finalize conversation drafts and delete snapshots on save"`.

## Task 7: Undo (confirmation, restore, reset)

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test
fun `undo changes clicked asks for confirmation`() {
    // Arrange
    val viewModel = createViewModel()

    // Act
    viewModel.onEvent(ConversationDetailViewModel.UiEvent.UndoChangesClicked)

    // Assert
    assertEquals(
        ConversationDetailViewModel.UiEventState.UndoChangesConfirmation,
        viewModel.uiState.value.eventState
    )
}

@Test
fun `undo changes confirmed restores edited rows from snapshot and deletes added rows`() {
    // Arrange
    val committed = createConversationList()
    val added = Conversation(
        id = ADDED_CONVERSATION_ID,
        question = "Added",
        response = "",
        orderIndex = 3,
        conversationGroupId = FIRST_CONVERSATION_ID,
        isDraft = true
    )
    val live = committed.mapIndexed { index, conversation ->
        if (index == 1) conversation.copy(question = "Edited", isDraft = true) else conversation
    } + added
    val conversationRepositoryRef = MockReferenceHolder<ConversationRepository>()
    val snapshotRepositoryRef = MockReferenceHolder<ConversationSnapshotRepository>()
    val viewModel = createViewModel(
        conversationRepositoryRef = conversationRepositoryRef,
        snapshotRepositoryRef = snapshotRepositoryRef,
        listByGroupResults = listOf(live, live, committed),
        snapshots = listOf(ConversationSnapshot(committed[1]))
    )
    viewModel.onEvent(ConversationDetailViewModel.UiEvent.ViewCreated(FIRST_CONVERSATION_ID))

    // Act
    viewModel.onEvent(ConversationDetailViewModel.UiEvent.UndoChangesConfirmed)
    mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    // Assert
    val conversationRepository = requireNotNull(conversationRepositoryRef.value)
    verifyBlocking(conversationRepository) { deleteBulk(listOf(ADDED_CONVERSATION_ID)) }
    verifyBlocking(conversationRepository) { save(committed[1]) }
    val snapshotRepository = requireNotNull(snapshotRepositoryRef.value)
    verifyBlocking(snapshotRepository) { deleteByGroup(FIRST_CONVERSATION_ID) }
    val state = viewModel.uiState.value
    assertEquals(3, state.conversationList.size)
    assertEquals("Question 2", state.conversationList[1].question)
    assertFalse(state.hasDrafts)
}

@Test
fun `undo changes confirmed on a never-committed group deletes it and resets the form`() {
    // Arrange
    val draftParent = Conversation(
        id = NEW_CONVERSATION_ID,
        question = "Typed",
        response = "",
        orderIndex = 0,
        conversationGroupId = null,
        isDraft = true
    )
    val conversationRepositoryRef = MockReferenceHolder<ConversationRepository>()
    val viewModel = createViewModel(
        conversationRepositoryRef = conversationRepositoryRef,
        listByGroupResults = listOf(listOf(draftParent), emptyList()),
        committedConversations = emptyList()
    )
    viewModel.onEvent(ConversationDetailViewModel.UiEvent.ViewCreated(null))
    viewModel.onEvent(
        ConversationDetailViewModel.UiEvent.QuestionChanged(
            viewModel.uiState.value.conversationList[0], "Typed"
        )
    )
    mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    // Act
    viewModel.onEvent(ConversationDetailViewModel.UiEvent.UndoChangesConfirmed)
    mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    // Assert
    val conversationRepository = requireNotNull(conversationRepositoryRef.value)
    verifyBlocking(conversationRepository) { deleteBulk(listOf(NEW_CONVERSATION_ID)) }
    val state = viewModel.uiState.value
    assertEquals(1, state.conversationList.size)
    assertEquals("", state.conversationList[0].question)
    assertFalse(state.hasDrafts)
}
```

- [ ] **Step 2: Run them** → expected: compilation fails because the `UndoChanges*` types don't exist yet.

- [ ] **Step 3: Implement**

Events and states:
- `UiEvent`: remove `CancelClicked` and add `UndoChangesClicked`, `UndoChangesConfirmed` and `UndoChangesConfirmationDismissed`, all `data object`.
- `UiEventState`: remove `Canceled` and add `data object UndoChangesConfirmation`.
- `onEvent`: remove the `CancelClicked` branch and the `cancelClicked()` function, and add:

```kotlin
is UiEvent.UndoChangesClicked -> undoChangesClicked()
is UiEvent.UndoChangesConfirmed -> undoChangesConfirmed()
is UiEvent.UndoChangesConfirmationDismissed -> undoChangesConfirmationDismissed()
```

Handlers:

```kotlin
private fun undoChangesClicked() {
    newState { copy(eventState = UiEventState.UndoChangesConfirmation) }
}

private fun undoChangesConfirmationDismissed() {
    newState { copy(eventState = UiEventState.Idle) }
}

/**
 * One data path for both cases: delete draft rows that have no snapshot (added this session, or
 * the whole group if it was never committed), then restore the snapshots. Only the UI step
 * branches: nothing left means the group never existed, so reset to an empty form.
 */
private fun undoChangesConfirmed() {
    newState { copy(eventState = UiEventState.Idle) }
    val groupId = conversationGroupId ?: return
    viewModelScope.launch(dispatchers.io) {
        val snapshots = snapshotRepository.listByGroup(groupId)
        val committedIds = snapshots.map { snapshot -> snapshot.conversation.id }.toSet()
        val addedIds = conversationRepository.listByIdOrGroupId(groupId)
            .filter { conversation -> conversation.isDraft && conversation.id !in committedIds }
            .map { conversation -> conversation.id }
        if (addedIds.isNotEmpty()) conversationRepository.deleteBulk(addedIds)
        snapshots.forEach { snapshot -> conversationRepository.save(snapshot.conversation) }
        snapshotRepository.deleteByGroup(groupId)

        val remaining = conversationRepository.listByIdOrGroupId(groupId)
        if (remaining.isEmpty()) {
            conversationGroupId = null
            newState { copy(conversationList = listOf(newConversation(0))) }
        } else {
            newState { copy(conversationList = remaining.map { conversation -> conversation.asState }) }
        }
        initialEditableData = _uiState.value.editableSnapshot()
    }
}
```

- [ ] **Step 4: Update the screen in the same step.** `testDebugUnitTest` compiles the main source set, and `ConversationDetailScreen.kt` still references the removed `CancelClicked`/`Canceled`. `StateHandler`'s `when` must also cover the new state, or the build fails.

In `ConversationDetailScreen.kt`, make the footer's Undo action send `ConversationDetailViewModel.UiEvent.UndoChangesClicked`, then add the dialog:

```kotlin
@Composable
private fun DiscardChangesConfirmation(onEvent: (ConversationDetailViewModel.UiEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = {
            onEvent(ConversationDetailViewModel.UiEvent.UndoChangesConfirmationDismissed)
        },
        title = { Text(text = stringResource(id = R.string.discard_changes_title)) },
        text = { Text(text = stringResource(id = R.string.discard_changes_message)) },
        confirmButton = {
            TextButton(onClick = { onEvent(ConversationDetailViewModel.UiEvent.UndoChangesConfirmed) }) {
                Text(stringResource(id = R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onEvent(ConversationDetailViewModel.UiEvent.UndoChangesConfirmationDismissed)
            }) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}
```

In `StateHandler`, remove the `Canceled` case and add:

```kotlin
is ConversationDetailViewModel.UiEventState.UndoChangesConfirmation -> {
    DiscardChangesConfirmation(onEvent)
}
```

- [ ] **Step 5: Run the tests**

Run: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --tests "com.msmobile.visitas.conversation.*"` → expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/conversation app/src/test/java/com/msmobile/visitas/conversation
git commit -m "Undo conversation drafts from snapshot"
```

## Task 8: Draft subtitle and preview variant

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/conversation/ConversationDetailScreen.kt`
- Modify: `app/src/main/java/com/msmobile/visitas/conversation/ConversationDetailPreviewConfigProvider.kt`

- [ ] **Step 1: Add the subtitle**

- In `ConversationDetailScreenContent`, compute the subtitle and re-publish the chrome whenever it changes, the same way `VisitDetailScreen` keys on `subtitle`:

```kotlin
val subtitle = conversationDetailSubtitleString(showSubtitle = uiState.hasDrafts)

// Keyed on subtitle, not Unit: the published actions and subtitle close over draft state, so
// the chrome has to be re-published when that changes or the scaffold keeps stale values.
DisposableEffect(subtitle) {
    appScaffoldState.setUiState(
        owner = chromeOwner,
        uiState = AppScaffoldState.UiState(
            topNavigationActions = topNavigationActions,
            topBarActions = topBarActions,
            detailFooterActions = detailFooterActions,
            floatingActionButtonActions = floatingActionButtonActions,
            subtitle = subtitle
        )
    )
    onDispose { appScaffoldState.clearUiState(chromeOwner) }
}
```

```kotlin
@Composable
private fun conversationDetailSubtitleString(showSubtitle: Boolean): String? {
    return if (showSubtitle) stringResource(R.string.draft) else null
}
```

- In `ConversationDetailScreenPreview`, pass `subtitle = conversationDetailSubtitleString(showSubtitle = config.uiState.hasDrafts)` to `AppScaffold`.

- [ ] **Step 2: Append a preview variant at the end of the `values` sequence**

```kotlin
ConversationDetailPreviewConfig(
    configName = "Draft",
    mainActivityUiState = previewMainActivityUiState,
    showDeleteButton = true,
    uiState = previewConversationDetailUiState.copy(
        conversationList = previewConversationDetailUiState.conversationList
            .mapIndexed { index, conversation ->
                if (index == 1) conversation.copy(isDraft = true) else conversation
            }
    )
)
```

- [ ] **Step 3: Verify**

Run: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --tests "com.msmobile.visitas.conversation.*"` → expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/conversation app/src/test/java/com/msmobile/visitas/conversation
git commit -m "Show Draft subtitle on conversation detail"
```

## Task 9: Hide never-committed conversations from visit suggestions

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitDetailViewModel.kt:1226`
- Test: `app/src/test/java/com/msmobile/visitas/visit/VisitDetailViewModelTest.kt:155,1538`

- [ ] **Step 1: Update the test first**

In `createViewModel`, change the stub to `on { listForSuggestions() } doReturn (conversations ?: createConversationList())`. In `onEvent with ViewCreated without householderId loads conversation list`, change the verification to `verifyBlocking(conversationRepository) { listForSuggestions() }`.

- [ ] **Step 2: Run it** → `./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.visit.VisitDetailViewModelTest"`. Expected: FAIL, because the stub doesn't match and the state has 0 conversations.

- [ ] **Step 3: Implement.** In `viewCreated`, replace `conversations = conversationRepository.listAll()` with `conversations = conversationRepository.listForSuggestions()`.

- [ ] **Step 4: Run it** → expected: PASS.

- [ ] **Step 5: Commit** with the message `"Hide never-committed conversation drafts from visit suggestions"`.

## Task 10: Restore `conversation_snapshot` from backups

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/util/BackupHandler.kt` (`restoreDataFrom`)
- Test: `app/src/androidTest/java/com/msmobile/visitas/util/BackupHandlerTest.kt`

- [ ] **Step 1: Write the failing instrumented test**

```kotlin
@Test
fun restoreBackup_withConversationDraft_shouldRestoreItsSnapshot() = runTest {
    // Arrange
    initDatabase()
    val committed = com.msmobile.visitas.conversation.Conversation(
        id = java.util.UUID.randomUUID(),
        question = "Committed question",
        response = "Committed response",
        orderIndex = 0,
        conversationGroupId = null
    )
    database.conversationDao().save(committed.copy(question = "Draft question", isDraft = true))
    database.conversationSnapshotDao().save(
        com.msmobile.visitas.conversation.ConversationSnapshot(committed)
    )
    val backupUri = backupHandler.createBackupFile().getOrThrow()
    database.clearAllTables()

    // Act
    backupHandler.restoreBackup(backupUri).getOrThrow()

    // Assert
    val restoredSnapshots = database.conversationSnapshotDao().listAll()
    assertEquals(1, restoredSnapshots.size)
    assertEquals(committed, restoredSnapshots.first().conversation)
}
```

- [ ] **Step 2: Run it** on a device: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.msmobile.visitas.util.BackupHandlerTest`. Expected: the new test FAILS with size 0.

- [ ] **Step 3: Implement.** In `restoreDataFrom`, directly after the conversation copy loop:

```kotlin
// Copy conversation snapshots after their live rows (FK on id). Without them a restored
// draft looks never-committed and Undo would delete it.
val conversationSnapshots = backupDatabase.conversationSnapshotDao().listAll()
conversationSnapshots.forEach { snapshot ->
    this.conversationSnapshotDao().save(snapshot)
}
```

- [ ] **Step 4: Run it again** → expected: all `BackupHandlerTest` tests pass.

- [ ] **Step 5: Commit** with the message `"Restore conversation snapshots from backups"`.

### PR 1 wrap-up
- Run `./gradlew test` and `sh scripts/verify-room-schemas.sh`. Push, then dispatch **Regenerate Screenshots** for the branch so the new "Draft" detail variant gets reference images.
- Open the PR with the `add-pr` skill. Reference the spec, and link #431 in "Out of scope". State that the group delete (Task 5) fixes a regression auto-save would otherwise introduce (orphaned draft children), not an unrelated tech-debt pickup.

---

# PR 2: Conversation list

## Task 11: Show drafts always, sorted first

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/conversation/ConversationListViewModel.kt`
- Test: `app/src/test/java/com/msmobile/visitas/conversation/ConversationListViewModelTest.kt`

- [ ] **Step 1: Make the test data configurable through the factory**

Add `conversations: List<Conversation> = createConversationList()` to `createViewModel` and stub `on { listAll() } doReturn conversations`.

- [ ] **Step 2: Write the failing tests**

```kotlin
@Test
fun `onEvent with ViewCreated flags every row of a group with a draft and sorts them first`() {
    // Arrange
    val loaded = createConversationList().map { conversation ->
        if (conversation.id == THIRD_CONVERSATION_ID) conversation.copy(isDraft = true) else conversation
    } + Conversation(
        id = OTHER_CONVERSATION_ID,
        question = "Another question",
        response = "",
        orderIndex = 0,
        conversationGroupId = null
    )
    val viewModel = createViewModel(conversations = loaded)

    // Act
    viewModel.onEvent(ConversationListViewModel.UiEvent.ViewCreated)

    // Assert
    val conversations = viewModel.uiState.value.conversations
    assertEquals(
        listOf(true, true, true, false),
        conversations.map { conversation -> conversation.hasDrafts }
    )
    assertEquals(OTHER_CONVERSATION_ID, conversations.last().conversationId)
}

@Test
fun `onEvent with SearchChanged never hides draft conversations`() {
    // Arrange
    val loaded = createConversationList().map { conversation ->
        if (conversation.id == SECOND_CONVERSATION_ID) conversation.copy(isDraft = true) else conversation
    }
    val viewModel = createViewModel(conversations = loaded)
    viewModel.onEvent(ConversationListViewModel.UiEvent.ViewCreated)

    // Act
    viewModel.onEvent(ConversationListViewModel.UiEvent.SearchChanged("no match at all"))

    // Assert
    val visible = viewModel.uiState.value.conversations.filter { conversation -> !conversation.hide }
    assertEquals(3, visible.size)   // the whole draft group stays visible
}
```

Add `private val OTHER_CONVERSATION_ID = UUID.fromString("1b2c3d4e-5f6a-4b7c-8d9e-0f1a2b3c4d5e")` to the companion object.

- [ ] **Step 3: Run them** → expected: compilation fails because `hasDrafts` is unresolved.

- [ ] **Step 4: Implement**

- Add `val hasDrafts: Boolean = false` to `ConversationState`.
- In `loadConversations`:

```kotlin
val conversations = conversationRepository.listAll()
val draftGroupIds = conversations
    .filter { conversation -> conversation.isDraft }
    .map { conversation -> conversation.conversationGroupId ?: conversation.id }
    .toSet()
val conversationStates = conversations.map { conversation ->
    val parentId = conversation.conversationGroupId ?: conversation.id
    ConversationState(
        conversationId = conversation.id,
        parentId = parentId,
        question = conversation.question,
        response = conversation.response,
        hasDrafts = parentId in draftGroupIds
    )
}.sortedByDescending { conversation -> conversation.hasDrafts }   // stable: keeps question order
```

- In `filterBy`: `val show = conversation.hasDrafts || isSearchEmpty || matchesQuestion || matchesResponse`.

- [ ] **Step 5: Run the tests** → expected: all pass, including the existing ones.

- [ ] **Step 6: Commit** with the message `"Always show draft conversations first in the list"`.

## Task 12: Draft label on the list card and a preview variant

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/conversation/ConversationListScreen.kt` (`ConversationCard`)
- Modify: `app/src/main/java/com/msmobile/visitas/conversation/ConversationListPreviewConfigProvider.kt`

- [ ] **Step 1: Add the label**, in the same style as `VisitCard`:

```kotlin
Column(modifier = Modifier.padding(cardInnerPadding)) {
    Text(text = conversation.question, fontWeight = FontWeight.Bold)
    if (conversation.hasDrafts) {
        Text(
            text = stringResource(id = R.string.draft),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}
```

Imports: `androidx.compose.ui.res.stringResource`, `com.msmobile.visitas.R`.

- [ ] **Step 2: Append a preview variant at the end of `values`**

```kotlin
PreviewConfig(
    configName = "With draft",
    mainActivityUiState = previewMainActivityUiState,
    conversationUiState = previewConversationUiState.copy(
        conversations = previewConversationUiState.conversations.mapIndexed { index, conversation ->
            if (index == 0) conversation.copy(hasDrafts = true) else conversation
        }
    )
)
```

- [ ] **Step 3: Verify** with `./gradlew :app:compileDebugKotlin` → expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit** with the message `"Show Draft label on conversation list cards"`. Then push, dispatch **Regenerate Screenshots**, and open the PR with `add-pr`.

---

## Task 13: Follow-up (no code)

- [ ] Make sure #431 (householder/visit snapshots lost on backup restore) is fixed with the same approach as Task 10.
