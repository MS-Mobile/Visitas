# Preferred Calendar Selection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the user choose which device calendar visit events are written to, via a dropdown in the Settings screen grouped with the existing "add visits to calendar" checkbox.

**Architecture:** `Preference` gains a nullable `preferredCalendarId`; null means "auto-pick", preserving today's scoring. `CalendarEventManager` splits its single `getFirstCalendar()` query into a list query plus a resolver, and the resolution rule lives in one shared extension used by both the write path and the Settings ViewModel. The app's hardcoded event color is removed so events take their calendar's own color.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Room, Hilt, Mockito-Kotlin 6.3.0, Compose Preview screenshot tests.

**Design spec:** `docs/superpowers/specs/2026-08-12-preferred-calendar-selection-design.md`

---

## Orientation for someone new to this codebase

Read these before starting:

- `AGENTS.md` — architecture, the ViewModel/UiState/UiEvent pattern, testing conventions, and the Room-schema and screenshot workflows. Non-negotiable house rules live here.
- Existing reference implementations you will mirror rather than invent:
  - `SettingsScreen.kt:319` `MapEngineDropdown` — the exact dropdown shape to copy.
  - `SettingsDetailViewModel.kt:72` `mapEngineSelected` — the exact "persist + update state" shape to copy.
  - `migration/Migration_13_14.kt` — the migration shape to copy.

Three things that will bite you if you skip them:

1. **Room schemas are gated.** Bumping the DB version requires a regenerated `app/schemas/…/15.json` committed in the same PR. A plain `assembleDebug` does **not** regenerate it — use the script in Task 4.
2. **Screenshot baselines are gated.** This feature adds an always-visible dropdown to the Settings screen, so **every** Settings reference PNG changes, not just the new one. See Task 8.
3. **A pre-commit hook fires on `VisitasDatabase.kt`.** It runs `BackupHandlerTest`, which needs a connected device/emulator. Have one running before the Task 4 commit.

**Test command form:** `./gradlew test` runs the whole unit-test suite. To run a single test class you must use the per-variant task — `./gradlew testDebugUnitTest --tests "fully.qualified.ClassName"` — because `test` is an aggregate lifecycle task and rejects `--tests` with "Unknown command-line option". In PowerShell use `.\gradlew.bat` instead of `./gradlew`. If `./gradlew` fails at *"Downloading gradle-…-bin.zip … 403 Forbidden"*, you are in a Claude Code web/remote session — do not work around it; push the branch and read results off the PR Build check run (see `AGENTS.md` § "No Gradle in Claude Code web/remote sessions").

**Branch:** work on `preferred-calendar-selection`, which already holds the design spec.

---

## File Structure

| File | Responsibility | Change |
|---|---|---|
| `app/src/main/java/com/msmobile/visitas/util/CalendarSelection.kt` | The `CalendarInfo` type and the one calendar-resolution rule | **Create** |
| `app/src/test/java/com/msmobile/visitas/util/CalendarSelectionTest.kt` | Tests for the resolution rule | **Create** |
| `app/src/main/java/com/msmobile/visitas/util/CalendarEventManager.kt` | `CalendarContract` wrapper: query, resolve, write, delete | Modify |
| `app/src/main/java/com/msmobile/visitas/util/SyncVisitCalendarEventUseCase.kt` | Visit → calendar event mapping | Modify |
| `app/src/main/java/com/msmobile/visitas/preference/Preference.kt` | Preferences entity | Modify |
| `app/src/main/java/com/msmobile/visitas/migration/Migration_14_15.kt` | Adds `preferredCalendarId` column | **Create** |
| `app/src/main/java/com/msmobile/visitas/VisitasDatabase.kt` | DB version + migration registry | Modify |
| `app/schemas/com.msmobile.visitas.VisitasDatabase/15.json` | Exported Room schema (generated) | **Generated** |
| `app/src/main/java/com/msmobile/visitas/settings/SettingsDetailViewModel.kt` | Settings state + events | Modify |
| `app/src/main/java/com/msmobile/visitas/settings/SettingsScreen.kt` | Settings UI | Modify |
| `app/src/main/java/com/msmobile/visitas/settings/SettingsPreviewConfigProvider.kt` | Preview/screenshot variants | Modify |
| `app/src/main/java/com/msmobile/visitas/visit/VisitDetailViewModel.kt` | Passes the preference through | Modify |
| `app/src/main/java/com/msmobile/visitas/visit/VisitListViewModel.kt` | Passes the preference through | Modify |
| `app/src/main/res/values/strings.xml` + `values-pt-rBR` + `values-b+es+419` | Dropdown label and empty placeholder | Modify |
| `app/src/test/java/com/msmobile/visitas/settings/SettingsDetailViewModelTest.kt` | Settings VM tests | Modify |
| `app/src/test/java/com/msmobile/visitas/visit/VisitDetailViewModelTest.kt` | Fix arity of two verify blocks | Modify |

---

### Task 1: Remove event color support

The app writes one hardcoded green (`EVENT_COLOR_KEY = "2"`) to every event. Color palettes are per-account, so once the user can pick a calendar on another account this code would either need a per-save palette query or would silently drop the key. Removing it lets each event take its calendar's own color.

`getAvailableColors`, `getDefaultColorKey`, `ColorKey`, and `EventColor` are public but have **zero callers anywhere in the repo, tests included** — verified by grep. There is no test to write first because there is no behavior under test and no caller to protect; the safety net is that the suite still compiles and passes.

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/util/CalendarEventManager.kt`

- [ ] **Step 1: Confirm the deleted API really has no callers**

Run:
```bash
git grep -n "ColorKey\|EventColor\|getAvailableColors\|getDefaultColorKey\|EVENT_COLOR"
```

Expected: every hit is inside `app/src/main/java/com/msmobile/visitas/util/CalendarEventManager.kt`. If a hit appears in any other file, stop — the spec's assumption is wrong and the removal needs rethinking.

- [ ] **Step 2: Drop the `color` parameter from `saveEvent`**

Replace the `saveEvent` signature (currently lines 27-35) with:

```kotlin
    suspend fun saveEvent(
        eventId: Long? = null,
        title: String,
        description: String,
        startTime: LocalDateTime,
        duration: Duration = DEFAULT_DURATION,
        isDone: Boolean = false
    ): Long? = withContext(Dispatchers.IO) {
```

- [ ] **Step 3: Drop the color columns from the event values**

Replace the `values` block (currently lines 45-61) with:

```kotlin
        // EVENT_COLOR_KEY is deliberately not written: an event with no color of its own renders
        // in the color of the calendar it belongs to, which is what the user picked in Settings.
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendar.id)
            put(CalendarContract.Events.TITLE, eventTitle)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, ZoneId.systemDefault().id)
        }
```

- [ ] **Step 4: Delete the rest of the color API**

Delete these members entirely from `CalendarEventManager`:

- `getAvailableColors()` (with its KDoc, currently lines 70-81)
- `getDefaultColorKey()` (currently line 83)
- `queryEventColors(calendar: CalendarInfo)` (currently lines 181-226)
- `value class ColorKey` and `data class EventColor` (currently lines 261-264)
- `DEFAULT_COLOR_KEY` and its two-line comment from the companion object (currently lines 271-273)

Keep `TAG`, `CHECKMARK`, `GOOGLE_ACCOUNT_TYPE`, and `DEFAULT_DURATION` — all still used.

- [ ] **Step 5: Verify it compiles and the suite is green**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL. If the compiler flags an unused import (`kotlin.coroutines.cancellation.CancellationException` is still used by `deleteEvent`, so it stays), remove only what it names.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/util/CalendarEventManager.kt
git commit -m "Let calendar events use their calendar's color

Event color palettes are per-account, so a user-picked calendar on another
account would need a per-save palette query or would silently drop the key.
The color API had no callers, so this is a straight removal."
```

---

### Task 2: The calendar resolution rule

One rule, used by both the write path and the Settings screen. Extracting it is what makes the fallback behavior testable at all — `CalendarEventManager` itself is `ContentResolver` all the way down and has no test file.

**Files:**
- Create: `app/src/main/java/com/msmobile/visitas/util/CalendarSelection.kt`
- Test: `app/src/test/java/com/msmobile/visitas/util/CalendarSelectionTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/msmobile/visitas/util/CalendarSelectionTest.kt`:

```kotlin
package com.msmobile.visitas.util

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test

class CalendarSelectionTest {

    @Test
    fun `resolvePreferred returns the preferred calendar when it is available`() {
        val calendars = listOf(GOOGLE_PRIMARY, WORK, LOCAL)

        val resolved = calendars.resolvePreferred(preferredCalendarId = WORK.id)

        assertEquals(WORK, resolved)
    }

    @Test
    fun `resolvePreferred falls back to the first calendar when the preferred one is gone`() {
        val calendars = listOf(GOOGLE_PRIMARY, WORK)

        val resolved = calendars.resolvePreferred(preferredCalendarId = 999L)

        assertEquals(GOOGLE_PRIMARY, resolved)
    }

    @Test
    fun `resolvePreferred falls back to the first calendar when no preference is set`() {
        val calendars = listOf(GOOGLE_PRIMARY, WORK)

        val resolved = calendars.resolvePreferred(preferredCalendarId = null)

        assertEquals(GOOGLE_PRIMARY, resolved)
    }

    @Test
    fun `resolvePreferred returns null when no calendars are available`() {
        val resolved = emptyList<CalendarInfo>().resolvePreferred(preferredCalendarId = 1L)

        assertNull(resolved)
    }

    private companion object {
        val GOOGLE_PRIMARY = CalendarInfo(
            id = 1L,
            displayName = "Personal",
            accountName = "user@gmail.com",
            accountType = "com.google"
        )
        val WORK = CalendarInfo(
            id = 2L,
            displayName = "Work",
            accountName = "user@work.com",
            accountType = "com.google"
        )
        val LOCAL = CalendarInfo(
            id = 3L,
            displayName = "Offline",
            accountName = "Local",
            accountType = "LOCAL"
        )
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.msmobile.visitas.util.CalendarSelectionTest"`
Expected: FAIL — compilation error, `Unresolved reference: CalendarInfo`.

- [ ] **Step 3: Write the implementation**

Create `app/src/main/java/com/msmobile/visitas/util/CalendarSelection.kt`:

```kotlin
package com.msmobile.visitas.util

/**
 * A calendar the app may write events to, as returned by
 * [CalendarEventManager.getAvailableCalendars]. [accountType] drives the automatic preference for
 * Google calendars; [accountName] is what tells two calendars with the same [displayName] apart in
 * the Settings dropdown.
 */
data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String?,
    val accountType: String?
)

/**
 * Picks the calendar events are written to.
 *
 * The receiver is ordered best-candidate-first, so falling back to the first entry reproduces the
 * automatic choice the app made before calendar selection existed. A [preferredCalendarId] that is
 * no longer in the list — the calendar was deleted, or its account was removed — falls back the
 * same way, so events keep being written instead of silently stopping.
 *
 * Callers must not reorder the list before calling this: the fallback *is* the ordering.
 */
fun List<CalendarInfo>.resolvePreferred(preferredCalendarId: Long?): CalendarInfo? =
    firstOrNull { it.id == preferredCalendarId } ?: firstOrNull()
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.msmobile.visitas.util.CalendarSelectionTest"`
Expected: PASS, 4 tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/util/CalendarSelection.kt app/src/test/java/com/msmobile/visitas/util/CalendarSelectionTest.kt
git commit -m "Add the calendar resolution rule

One shared rule for preferred-or-fallback, so the write path and the Settings
screen cannot drift apart. This is the first test coverage the fallback has had."
```

---

### Task 3: Split `CalendarEventManager`'s query from its choice

`getFirstCalendar()` filters, scores, and picks a winner in one pass. Three responsibilities, one method, none of them testable. Split it: `queryWritableCalendars()` maps the cursor and delegates ranking to `orderedByAutoPickPreference()`, and `resolveCalendar()` applies `resolvePreferred`.

The scoring moves wholesale to `CalendarSelection.kt` (Task 2), so `calculateCalendarScore` and `GOOGLE_ACCOUNT_TYPE` leave this class. What remains here is only what genuinely needs `ContentResolver`: the query and the column mapping. That is why there is no unit test in this task — the part worth testing is now in Task 2's suite. The provider behavior is verified on an emulator in Task 9.

**Do not reintroduce a tie-break.** `getFirstCalendar` picked with `if (score > bestScore)`, so equal-scoring calendars resolved to the first cursor row. `orderedByAutoPickPreference` uses a stable sort on score alone, preserving that. Adding a secondary sort key here (by display name, say) would move the automatic pick for any user whose best-scoring band holds two or more calendars, silently relocating their events on upgrade.

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/util/CalendarEventManager.kt`

- [ ] **Step 1: Give `saveEvent` a calendar id**

Replace the `saveEvent` signature and its calendar lookup (the signature written in Task 1, plus the `val calendar = getFirstCalendar() ?: return@withContext null` line) with:

```kotlin
    suspend fun saveEvent(
        eventId: Long? = null,
        calendarId: Long? = null,
        title: String,
        description: String,
        startTime: LocalDateTime,
        duration: Duration = DEFAULT_DURATION,
        isDone: Boolean = false
    ): Long? = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) {
            return@withContext null
        }

        val calendar = resolveCalendar(calendarId) ?: return@withContext null
```

The `calendarId = null` default keeps every existing call site compiling with today's behavior.

- [ ] **Step 2: Add the public list accessor**

Add this immediately after `saveEvent` (it replaces the deleted `getAvailableColors`, which sat in the same spot):

```kotlin
    /**
     * The calendars the app may write to, ordered best-candidate-first — see [resolvePreferred],
     * which relies on that ordering. Empty without calendar permission.
     */
    suspend fun getAvailableCalendars(): List<CalendarInfo> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) {
            return@withContext emptyList()
        }
        queryWritableCalendars()
    }
```

- [ ] **Step 3: Replace `getFirstCalendar` with the query + resolver pair**

Delete `getFirstCalendar()` entirely (currently lines 124-179) and the `private data class CalendarInfo(...)` nested at the bottom of the class (currently lines 255-259) — `CalendarInfo` now comes from `CalendarSelection.kt` in the same package, so no import is needed. Put this in `getFirstCalendar`'s place:

```kotlin
    private fun resolveCalendar(preferredCalendarId: Long?): CalendarInfo? =
        queryWritableCalendars().resolvePreferred(preferredCalendarId)

    private fun queryWritableCalendars(): List<CalendarInfo> {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE
        )

        val selection = """
            ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ? AND
            ${CalendarContract.Calendars.VISIBLE} = ? AND
            ${CalendarContract.Calendars.SYNC_EVENTS} = ?
        """.trimIndent()
        val selectionArgs = arrayOf(
            CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString(),
            "1",  // Visible
            "1"   // Sync events enabled
        )

        return try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                if (idIndex < 0) return@use emptyList()

                val displayNameIndex =
                    cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val isPrimaryIndex = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)
                val accountNameIndex =
                    cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
                val accountTypeIndex =
                    cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE)

                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            CalendarInfo(
                                id = cursor.getLong(idIndex),
                                displayName = if (displayNameIndex >= 0) {
                                    cursor.getString(displayNameIndex).orEmpty()
                                } else {
                                    ""
                                },
                                accountName = if (accountNameIndex >= 0) {
                                    cursor.getString(accountNameIndex)
                                } else {
                                    null
                                },
                                accountType = if (accountTypeIndex >= 0) {
                                    cursor.getString(accountTypeIndex)
                                } else {
                                    null
                                },
                                isPrimary = isPrimaryIndex >= 0 && cursor.getInt(isPrimaryIndex) == 1
                            )
                        )
                    }
                }.orderedByAutoPickPreference()
            } ?: emptyList()
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }
            logger.error(TAG, "Failed to query calendars", e)
            emptyList()
        }
    }
```

This method now only maps the cursor — the ranking lives in `orderedByAutoPickPreference()` in `CalendarSelection.kt`, where it is unit-tested. The `try`/`catch` is new: the old `getFirstCalendar` had none, while every other query in this class does.

- [ ] **Step 4: Delete the scoring that moved out**

`calculateCalendarScore` and the `GOOGLE_ACCOUNT_TYPE` constant are now dead — `orderedByAutoPickPreference` owns that logic and `CalendarSelection.kt` has its own private copy of the account-type constant. Delete both from `CalendarEventManager`, keeping `TAG`, `CHECKMARK`, and `DEFAULT_DURATION` in the companion object.

- [ ] **Step 5: Verify it compiles and the suite is green**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/util/CalendarEventManager.kt
git commit -m "Split calendar querying from calendar choice

getFirstCalendar filtered and scored in one pass and returned a single winner.
The Settings dropdown needs the whole list, so query and choice are now separate
and the choice defers to the shared resolvePreferred rule."
```

---

### Task 4: Persist `preferredCalendarId`

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/preference/Preference.kt`
- Create: `app/src/main/java/com/msmobile/visitas/migration/Migration_14_15.kt`
- Modify: `app/src/main/java/com/msmobile/visitas/VisitasDatabase.kt:52,80` and its imports
- Generated: `app/schemas/com.msmobile.visitas.VisitasDatabase/15.json`

- [ ] **Step 1: Add the field**

In `Preference.kt`, add the property to the end of the entity:

```kotlin
@Entity(tableName = "preference")
data class Preference(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val visitListDateFilterOption: VisitListDateFilterOption,
    val visitListDistanceFilterOption: VisitListDistanceFilterOption,
    val visitMapEngineOption: VisitMapEngineOption = VisitMapEngineOption.MapLibre,
    val addVisitsToCalendar: Boolean = false,
    val hasSeenAddVisitToCalendarMessage: Boolean = false,
    /** Null means "let the app pick" — see `resolvePreferred`. */
    val preferredCalendarId: Long? = null
)
```

No type converter is needed; Room maps `Long?` to a nullable `INTEGER` natively.

- [ ] **Step 2: Write the migration**

Create `app/src/main/java/com/msmobile/visitas/migration/Migration_14_15.kt`:

```kotlin
package com.msmobile.visitas.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds `preferredCalendarId` to `preference` — the calendar the user picked for visit events.
 *
 * Nullable with no default on purpose: null is the meaningful "let the app pick" value, so existing
 * installs keep the automatic choice they have been getting until the user changes it.
 */
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `preference` ADD COLUMN `preferredCalendarId` INTEGER")
    }
}
```

- [ ] **Step 3: Register the migration and bump the version**

In `VisitasDatabase.kt`, add the import next to `MIGRATION_13_14` (line 24):

```kotlin
import com.msmobile.visitas.migration.MIGRATION_14_15
```

Change line 52 from `version = 14` to:

```kotlin
    version = 15
```

Change the last entry of `MIGRATIONS` (line 80) from `MIGRATION_13_14` to:

```kotlin
            MIGRATION_13_14,
            MIGRATION_14_15
```

- [ ] **Step 4: Export the schema**

Run: `sh scripts/verify-room-schemas.sh --export-only`
Expected: `app/schemas/com.msmobile.visitas.VisitasDatabase/15.json` is written. Confirm with `git status` that the file is new and untracked.

A plain `assembleDebug` will not do this — see `AGENTS.md` § Database Migrations for why.

- [ ] **Step 5: Verify the exported schema matches the entity**

Run: `sh scripts/verify-room-schemas.sh`
Expected: exits 0. This is the same check the PR build runs.

- [ ] **Step 6: Commit**

This commit touches `VisitasDatabase.kt`, so the pre-commit hook runs `BackupHandlerTest` and needs a connected device or running emulator. Start one first.

```bash
git add app/src/main/java/com/msmobile/visitas/preference/Preference.kt app/src/main/java/com/msmobile/visitas/migration/Migration_14_15.kt app/src/main/java/com/msmobile/visitas/VisitasDatabase.kt app/schemas/com.msmobile.visitas.VisitasDatabase/15.json
git commit -m "Persist the user's preferred calendar

Nullable, because null is the meaningful 'let the app pick' value rather than a
placeholder — existing installs keep the automatic choice until they change it."
```

---

### Task 5: Pass the preference down the write path

Both call sites already read `Preference` before invoking the use case, so this costs no extra query.

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/util/SyncVisitCalendarEventUseCase.kt`
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitDetailViewModel.kt:1031-1058`
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitListViewModel.kt:259-276`
- Test: `app/src/test/java/com/msmobile/visitas/visit/VisitDetailViewModelTest.kt:390,410`

- [ ] **Step 1: Update the two existing verify blocks to the new arity**

`invoke` is about to take a seventh parameter, and these two blocks match parameters positionally. In `VisitDetailViewModelTest.kt`, change **both** line 390 and line 410 from

```kotlin
            invoke(anyOrNull(), any(), any(), any(), any(), any())
```

to

```kotlin
            invoke(anyOrNull(), anyOrNull(), any(), any(), any(), any(), any())
```

The second `anyOrNull()` is the new nullable `preferredCalendarId`.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.msmobile.visitas.visit.VisitDetailViewModelTest"`
Expected: FAIL — compilation error, `Too many arguments for public open suspend operator fun invoke(...)`.

- [ ] **Step 3: Add the parameter to the use case**

Replace the body of `SyncVisitCalendarEventUseCase.kt`:

```kotlin
@Singleton
class SyncVisitCalendarEventUseCase @Inject constructor(
    private val calendarEventManager: CalendarEventManager
) {
    suspend operator fun invoke(
        calendarEventId: Long?,
        preferredCalendarId: Long?,
        visitType: VisitType,
        subject: String,
        date: LocalDateTime,
        isDone: Boolean,
        householderName: String
    ): Long? {
        if (!calendarEventManager.hasCalendarPermission()) return calendarEventId
        if (visitType == VisitType.FIRST_VISIT) return null
        val title = if (subject.isNotBlank()) {
            "$householderName - ${subject.lines().firstOrNull() ?: ""}"
        } else {
            householderName
        }
        return calendarEventManager.saveEvent(
            eventId = calendarEventId,
            calendarId = preferredCalendarId,
            title = title,
            description = subject,
            startTime = date,
            isDone = isDone
        )
    }
}
```

- [ ] **Step 4: Update the `VisitDetailViewModel` call site**

Replace `addOrUpdateVisits` (lines 1031-1058) with:

```kotlin
    private suspend fun addOrUpdateVisits(
        householderId: UUID,
        householderName: String,
        visitList: List<VisitState>
    ): List<VisitState> {
        // Read once for the whole batch rather than per visit. When calendar sync is off the
        // existing event id is kept, so events already on the calendar are left alone and
        // re-enabling the setting keeps updating them instead of creating duplicates.
        val preference = preferenceRepository.get()
        return visitList.map { visitState ->
            val calendarEventId = if (preference.addVisitsToCalendar) {
                syncVisitCalendarEvent(
                    calendarEventId = visitState.calendarEventId,
                    preferredCalendarId = preference.preferredCalendarId,
                    visitType = visitState.visitType.type,
                    subject = visitState.subject,
                    date = visitState.date,
                    isDone = visitState.isDone,
                    householderName = householderName
                )
            } else {
                visitState.calendarEventId
            }
            val updatedVisitState = visitState.copy(calendarEventId = calendarEventId)
            val visitModel = updatedVisitState.asModel(householderId)
            visitRepository.save(visitModel)
            visitModel.asState(visitState.nextConversationSuggestion)
        }
    }
```

- [ ] **Step 5: Update the `VisitListViewModel` call site**

Replace lines 259-272 with:

```kotlin
        viewModelScope.launch(dispatchers.io) {
            val visitModel = visitRepository.getById(visit.visitId).copy(date = date)
            val preference = preferenceRepository.get()
            val calendarEventId = if (preference.addVisitsToCalendar) {
                syncVisitCalendarEvent(
                    calendarEventId = visitModel.calendarEventId,
                    preferredCalendarId = preference.preferredCalendarId,
                    visitType = visitModel.visitType,
                    subject = visitModel.subject,
                    date = visitModel.date,
                    isDone = visitModel.isDone,
                    householderName = visit.householderName
                )
            } else {
                visitModel.calendarEventId
            }
```

Leave the lines after it (`val updatedVisitModel = …` onward) untouched.

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL, whole suite green.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/util/SyncVisitCalendarEventUseCase.kt app/src/main/java/com/msmobile/visitas/visit/VisitDetailViewModel.kt app/src/main/java/com/msmobile/visitas/visit/VisitListViewModel.kt app/src/test/java/com/msmobile/visitas/visit/VisitDetailViewModelTest.kt
git commit -m "Write visit events to the preferred calendar

Both call sites already read Preference before syncing, so threading the id
through costs no extra query."
```

---

### Task 6: Settings ViewModel

The screen loads the calendar list whenever permission is granted — regardless of the checkbox — so the dropdown can legitimately be populated while disabled. This is also where a stale id gets cleared, lazily, so the write path never blocks on a preference write.

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/settings/SettingsDetailViewModel.kt`
- Test: `app/src/test/java/com/msmobile/visitas/settings/SettingsDetailViewModelTest.kt`

- [ ] **Step 1: Extend the test factory**

Per `AGENTS.md` § Testing Conventions, all mock configuration lives in `createViewModel`. Replace it (lines 181-208) with:

```kotlin
    private fun createViewModel(
        savedMapEngine: VisitMapEngineOption = VisitMapEngineOption.MapLibre,
        savedAddVisitsToCalendar: Boolean = false,
        savedPreferredCalendarId: Long? = null,
        availableCalendars: List<CalendarInfo> = emptyList(),
        hasCalendarPermission: Boolean = true,
        preferenceRepositoryRef: MockReferenceHolder<PreferenceRepository>? = null
    ): SettingsDetailViewModel {
        val dispatchers = DispatcherProvider(io = mainDispatcherRule.dispatcher)
        val backupHandler = mock<BackupHandler>()
        val preferenceRepository = mock<PreferenceRepository> {
            on { get() } doReturn Preference(
                visitListDateFilterOption = VisitListDateFilterOption.All,
                visitListDistanceFilterOption = VisitListDistanceFilterOption.All,
                visitMapEngineOption = savedMapEngine,
                addVisitsToCalendar = savedAddVisitsToCalendar,
                preferredCalendarId = savedPreferredCalendarId
            )
        }
        preferenceRepositoryRef?.value = preferenceRepository
        val calendarEventManager = mock<CalendarEventManager> {
            on { hasCalendarPermission() } doReturn hasCalendarPermission
            on { getAvailableCalendars() } doReturn availableCalendars
        }
        return SettingsDetailViewModel(
            preferenceRepository = preferenceRepository,
            calendarEventManager = calendarEventManager,
            backupHandler = backupHandler,
            dispatchers = dispatchers,
            appVersionProvider = AppVersionProvider
        )
    }
```

`getAvailableCalendars()` is a suspend function, and `on { … }` is the correct stubbing form for it here — the file already stubs the suspend `preferenceRepository.get()` exactly this way, and there is no `onBlocking` anywhere in this codebase.

Add these imports to the file:

```kotlin
import com.msmobile.visitas.util.CalendarInfo
```

- [ ] **Step 2: Write the failing tests**

Add these to `SettingsDetailViewModelTest`, after the existing `CalendarPermissionDenied` test (line 179) and before `createViewModel`:

```kotlin
    @Test
    fun `onEvent with ViewCreated loads the available calendars when permission is granted`() {
        val viewModel = createViewModel(
            hasCalendarPermission = true,
            availableCalendars = listOf(PERSONAL_CALENDAR, MINISTRY_CALENDAR)
        )

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.ViewCreated)

        assertEquals(
            listOf(PERSONAL_CALENDAR, MINISTRY_CALENDAR),
            viewModel.uiState.value.availableCalendars
        )
    }

    @Test
    fun `onEvent with ViewCreated does not query calendars without permission`() {
        val calendarEventManagerRef = MockReferenceHolder<CalendarEventManager>()
        val viewModel = createViewModel(
            hasCalendarPermission = false,
            availableCalendars = listOf(PERSONAL_CALENDAR),
            calendarEventManagerRef = calendarEventManagerRef
        )

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.ViewCreated)

        assertTrue(viewModel.uiState.value.availableCalendars.isEmpty())
        verifyBlocking(requireNotNull(calendarEventManagerRef.value), never()) {
            getAvailableCalendars()
        }
    }

    @Test
    fun `onEvent with ViewCreated keeps a preferred calendar that is still available`() {
        val viewModel = createViewModel(
            savedPreferredCalendarId = MINISTRY_CALENDAR.id,
            availableCalendars = listOf(PERSONAL_CALENDAR, MINISTRY_CALENDAR)
        )

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.ViewCreated)

        assertEquals(MINISTRY_CALENDAR.id, viewModel.uiState.value.preferredCalendarId)
    }

    @Test
    fun `onEvent with ViewCreated clears a preferred calendar that no longer exists`() {
        val preferenceRepositoryRef = MockReferenceHolder<PreferenceRepository>()
        val viewModel = createViewModel(
            savedPreferredCalendarId = 999L,
            availableCalendars = listOf(PERSONAL_CALENDAR),
            preferenceRepositoryRef = preferenceRepositoryRef
        )

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.ViewCreated)

        assertNull(viewModel.uiState.value.preferredCalendarId)
        verifyBlocking(requireNotNull(preferenceRepositoryRef.value)) {
            save(argThat { preferredCalendarId == null })
        }
    }

    @Test
    fun `onEvent with CalendarSelected updates state to the selected calendar`() {
        val viewModel = createViewModel(
            availableCalendars = listOf(PERSONAL_CALENDAR, MINISTRY_CALENDAR)
        )

        viewModel.onEvent(
            SettingsDetailViewModel.UiEvent.CalendarSelected(MINISTRY_CALENDAR.id)
        )

        assertEquals(MINISTRY_CALENDAR.id, viewModel.uiState.value.preferredCalendarId)
    }

    @Test
    fun `onEvent with CalendarSelected saves the preference`() {
        val preferenceRepositoryRef = MockReferenceHolder<PreferenceRepository>()
        val viewModel = createViewModel(
            availableCalendars = listOf(PERSONAL_CALENDAR, MINISTRY_CALENDAR),
            preferenceRepositoryRef = preferenceRepositoryRef
        )

        viewModel.onEvent(
            SettingsDetailViewModel.UiEvent.CalendarSelected(MINISTRY_CALENDAR.id)
        )

        verifyBlocking(requireNotNull(preferenceRepositoryRef.value)) {
            save(argThat { preferredCalendarId == MINISTRY_CALENDAR.id })
        }
    }

    @Test
    fun `onEvent with CalendarPermissionGranted loads the available calendars`() {
        val viewModel = createViewModel(
            hasCalendarPermission = true,
            availableCalendars = listOf(PERSONAL_CALENDAR, MINISTRY_CALENDAR)
        )

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.CalendarPermissionGranted)

        assertEquals(
            listOf(PERSONAL_CALENDAR, MINISTRY_CALENDAR),
            viewModel.uiState.value.availableCalendars
        )
    }
```

The `does not query calendars without permission` test needs a handle on the calendar mock, so add that parameter to `createViewModel` as well — the full signature line becomes:

```kotlin
    private fun createViewModel(
        savedMapEngine: VisitMapEngineOption = VisitMapEngineOption.MapLibre,
        savedAddVisitsToCalendar: Boolean = false,
        savedPreferredCalendarId: Long? = null,
        availableCalendars: List<CalendarInfo> = emptyList(),
        hasCalendarPermission: Boolean = true,
        preferenceRepositoryRef: MockReferenceHolder<PreferenceRepository>? = null,
        calendarEventManagerRef: MockReferenceHolder<CalendarEventManager>? = null
    ): SettingsDetailViewModel {
```

and add this line directly after the `calendarEventManager` mock is built:

```kotlin
        calendarEventManagerRef?.value = calendarEventManager
```

Add the test constants to the bottom of the class, after `createViewModel`:

```kotlin
    private companion object {
        val PERSONAL_CALENDAR = CalendarInfo(
            id = 1L,
            displayName = "Personal",
            accountName = "user@gmail.com",
            accountType = "com.google"
        )
        val MINISTRY_CALENDAR = CalendarInfo(
            id = 2L,
            displayName = "Ministry",
            accountName = "user@gmail.com",
            accountType = "com.google"
        )
    }
```

Add the remaining import:

```kotlin
import junit.framework.TestCase.assertNull
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.msmobile.visitas.settings.SettingsDetailViewModelTest"`
Expected: FAIL — compilation errors, `Unresolved reference: availableCalendars` and `Unresolved reference: CalendarSelected`.

- [ ] **Step 4: Add the state and event**

In `SettingsDetailViewModel.kt`, add to `UiState` (line 249-258):

```kotlin
    data class UiState(
        val isLoading: Boolean = false,
        val backupResult: BackupResult? = null,
        val eventState: UiEventState = UiEventState.Idle,
        val selectedMapEngine: VisitMapEngineOption = VisitMapEngineOption.MapLibre,
        val addVisitsToCalendar: Boolean = false,
        val availableCalendars: List<CalendarInfo> = emptyList(),
        val preferredCalendarId: Long? = null,
        val showCalendarRationale: Boolean = false,
        val showCalendarPermissionDialog: Boolean = false,
        val versionName: String = ""
    )
```

Add the event next to `AddVisitsToCalendarToggled` in the `UiEvent` sealed class:

```kotlin
        data class CalendarSelected(val calendarId: Long) : UiEvent()
```

Add the dispatch branch to `onEvent`, after the `AddVisitsToCalendarToggled` line:

```kotlin
            is UiEvent.CalendarSelected -> calendarSelected(event.calendarId)
```

Add the import:

```kotlin
import com.msmobile.visitas.util.CalendarInfo
```

- [ ] **Step 5: Load the calendars and handle selection**

Replace `viewCreated()` (lines 60-70) with:

```kotlin
    private fun viewCreated() {
        viewModelScope.launch(dispatchers.io) {
            val preference = preferenceRepository.get()
            _uiState.update {
                it.copy(
                    selectedMapEngine = preference.visitMapEngineOption,
                    addVisitsToCalendar = preference.addVisitsToCalendar,
                    preferredCalendarId = preference.preferredCalendarId
                )
            }
            loadAvailableCalendars()
        }
    }

    /**
     * Loads the calendars the dropdown offers, and drops a stored id that no longer resolves — the
     * calendar was deleted, or its account was removed. Clearing it here rather than on the write
     * path keeps saving a visit free of preference writes; the write path falls back on its own.
     *
     * This runs whenever calendar permission is granted, not only while the checkbox is on, so the
     * dropdown can be populated even while it is disabled.
     */
    private suspend fun loadAvailableCalendars() {
        if (!calendarEventManager.hasCalendarPermission()) return
        val calendars = calendarEventManager.getAvailableCalendars()
        val preference = preferenceRepository.get()
        val storedId = preference.preferredCalendarId
        val isStale = storedId != null && calendars.none { it.id == storedId }
        if (isStale) {
            preferenceRepository.save(preference.copy(preferredCalendarId = null))
        }
        _uiState.update {
            it.copy(
                availableCalendars = calendars,
                preferredCalendarId = if (isStale) null else storedId
            )
        }
    }

    private fun calendarSelected(calendarId: Long) {
        viewModelScope.launch(dispatchers.io) {
            val preference = preferenceRepository.get().copy(preferredCalendarId = calendarId)
            preferenceRepository.save(preference)
        }
        _uiState.update { it.copy(preferredCalendarId = calendarId) }
    }
```

Replace `calendarPermissionGranted()` (lines 116-119) with:

```kotlin
    private fun calendarPermissionGranted() {
        _uiState.update { it.copy(showCalendarPermissionDialog = false) }
        saveAddVisitsToCalendar(enabled = true)
        viewModelScope.launch(dispatchers.io) { loadAvailableCalendars() }
    }
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.msmobile.visitas.settings.SettingsDetailViewModelTest"`
Expected: PASS — the 7 new tests plus the 11 that already existed.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/settings/SettingsDetailViewModel.kt app/src/test/java/com/msmobile/visitas/settings/SettingsDetailViewModelTest.kt
git commit -m "Load and persist the calendar choice in Settings

The list loads whenever permission is granted rather than only while the
checkbox is on, and a stored id that no longer resolves is dropped here rather
than on the write path, so saving a visit never waits on a preference write."
```

---

### Task 7: Settings dropdown UI

**Files:**
- Modify: `app/src/main/res/values/strings.xml:93`
- Modify: `app/src/main/res/values-pt-rBR/strings.xml:91`
- Modify: `app/src/main/res/values-b+es+419/strings.xml:92`
- Modify: `app/src/main/java/com/msmobile/visitas/settings/SettingsScreen.kt`

- [ ] **Step 1: Add the strings in all three locales**

The section header is already "Calendar", so the dropdown label says where events go instead of repeating it.

In `app/src/main/res/values/strings.xml`, after the `map_engine_label` line:

```xml
    <string name="settings_calendar_selection_label">Save events to</string>
    <string name="settings_calendar_selection_empty">No calendar available</string>
```

In `app/src/main/res/values-pt-rBR/strings.xml`, after its `map_engine_label` line:

```xml
    <string name="settings_calendar_selection_label">Salvar eventos em</string>
    <string name="settings_calendar_selection_empty">Nenhuma agenda disponível</string>
```

In `app/src/main/res/values-b+es+419/strings.xml`, after its `map_engine_label` line:

```xml
    <string name="settings_calendar_selection_label">Guardar eventos en</string>
    <string name="settings_calendar_selection_empty">No hay calendarios disponibles</string>
```

- [ ] **Step 2: Add the dropdown composable**

In `SettingsScreen.kt`, add after `MapEngineDropdown` (which ends at line 354):

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarSelectionDropdown(
    calendars: List<CalendarInfo>,
    preferredCalendarId: Long?,
    enabled: Boolean,
    onCalendarSelected: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    // The field shows the calendar events actually go to, not the raw stored id, so it never reads
    // as "nothing selected" while events are being written to the automatic choice.
    val selectedCalendar = calendars.resolvePreferred(preferredCalendarId)
    ExposedDropdownMenuBox(
        expanded = expanded,
        // ExposedDropdownMenuBox has no `enabled` flag of its own: without this guard the menu
        // still opens on tap while the checkbox is off.
        onExpandedChange = { if (enabled) expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedCalendar?.displayName
                ?: stringResource(R.string.settings_calendar_selection_empty),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(text = stringResource(R.string.settings_calendar_selection_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            calendars.forEach { calendar ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(text = calendar.displayName)
                            // Two calendars on different accounts can share a display name.
                            if (!calendar.accountName.isNullOrBlank()) {
                                Text(
                                    text = calendar.accountName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    onClick = {
                        onCalendarSelected(calendar.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
```

Add these imports:

```kotlin
import com.msmobile.visitas.util.CalendarInfo
import com.msmobile.visitas.util.resolvePreferred
```

`Column`, `MaterialTheme`, `remember`, `mutableStateOf`, and the `ExposedDropdownMenu*` symbols are already imported for `MapEngineDropdown`.

- [ ] **Step 3: Place it in the Calendar section**

Replace the calendar `SettingsSection` block (lines 168-175) with:

```kotlin
        SettingsSection(title = stringResource(R.string.settings_section_calendar)) {
            AddVisitsToCalendarCheckbox(
                checked = uiState.addVisitsToCalendar,
                onCheckedChange = { enabled ->
                    onEvent(SettingsDetailViewModel.UiEvent.AddVisitsToCalendarToggled(enabled))
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            CalendarSelectionDropdown(
                calendars = uiState.availableCalendars,
                preferredCalendarId = uiState.preferredCalendarId,
                enabled = uiState.addVisitsToCalendar,
                onCalendarSelected = { calendarId ->
                    onEvent(SettingsDetailViewModel.UiEvent.CalendarSelected(calendarId))
                }
            )
        }
```

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/settings/SettingsScreen.kt app/src/main/res/values/strings.xml app/src/main/res/values-pt-rBR/strings.xml app/src/main/res/values-b+es+419/strings.xml
git commit -m "Add the calendar dropdown to Settings

Always visible so the section's layout does not jump, and disabled while the
checkbox is off. ExposedDropdownMenuBox has no enabled flag, so the expand
callback is guarded as well as the text field."
```

---

### Task 8: Preview variant and screenshot baselines

**Read `AGENTS.md` § Screenshot Tests before this task.** The rule there is to add a variant at the end of the provider rather than edit shared preview state. Note what is different here: the dropdown is *always visible*, so every existing Settings baseline legitimately changes too. That is the intended UI change, not the noisy-diff failure mode the rule guards against — do not try to avoid it.

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/settings/SettingsPreviewConfigProvider.kt`
- Generated: `app/src/screenshotTestDebug/reference/**` Settings PNGs

- [ ] **Step 1: Add the new preview variant**

In `SettingsPreviewConfigProvider.kt`, add a comma after the closing `)` of the "Add Visits To Calendar Enabled" config (line 61), then add this as the **last** entry of `values`:

```kotlin
        SettingsPreviewConfig(
            configName = "Calendar Selected",
            mainActivityUiState = previewMainActivityUiState,
            uiState = SettingsDetailViewModel.UiState(
                selectedMapEngine = VisitMapEngineOption.MapLibre,
                addVisitsToCalendar = true,
                availableCalendars = PREVIEW_CALENDARS,
                preferredCalendarId = 2L,
                versionName = APP_VERSION
            )
        )
```

Add to the existing `companion object` (lines 68-70):

```kotlin
    companion object {
        private const val APP_VERSION = "1.0.1#710"

        private val PREVIEW_CALENDARS = listOf(
            CalendarInfo(
                id = 1L,
                displayName = "Personal",
                accountName = "user@gmail.com",
                accountType = "com.google"
            ),
            CalendarInfo(
                id = 2L,
                displayName = "Ministry",
                accountName = "user@gmail.com",
                accountType = "com.google"
            )
        )
    }
```

Add the import:

```kotlin
import com.msmobile.visitas.util.CalendarInfo
```

- [ ] **Step 2: See which baselines the change affects**

Run: `./gradlew validateDebugScreenshotTest`
Expected: FAIL. Every Settings variant is reported as differing (the new dropdown row), plus one missing reference for "Calendar Selected". This failure is the expected signal, not a problem to debug — confirm the *only* diffs reported are Settings ones. A diff in any other screen's baseline means something unrelated broke; stop and investigate.

- [ ] **Step 3: Regenerate the baselines**

Locally: `./gradlew updateDebugScreenshotTest`

On a branch without a local Android SDK: push the branch and dispatch the **Regenerate Screenshots** workflow (`.github/workflows/regenerate-screenshots.yml`) for it, which commits the PNGs for you.

- [ ] **Step 4: Verify the baselines now pass**

Run: `./gradlew validateDebugScreenshotTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Inspect the new reference image before committing**

Open the newly written "Calendar Selected" PNG under `app/src/screenshotTestDebug/reference/`. Confirm the dropdown shows "Ministry" (id `2L`, the preferred one — not "Personal", which is first in the list). If it shows "Personal", `resolvePreferred` is not being applied in the composable.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/settings/SettingsPreviewConfigProvider.kt app/src/screenshotTestDebug/reference
git commit -m "Add screenshot coverage for the calendar dropdown

Every Settings baseline changes because the dropdown is always visible; the new
Calendar Selected variant covers a populated, enabled list."
```

---

### Task 9: Full verification and PR

- [ ] **Step 1: Run the whole suite**

Run: `./gradlew test validateDebugScreenshotTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Verify the committed Room schema still matches**

Run: `sh scripts/verify-room-schemas.sh`
Expected: exits 0.

- [ ] **Step 3: Drive the app on an emulator**

Use the `verify` skill. On a device with **two** calendar accounts, check the four things unit tests cannot:

1. With the checkbox off, the dropdown is visible, greyed out, and does not open on tap.
2. Turning the checkbox on grants permission and populates the dropdown in one pass.
3. Picking a non-default calendar on the *other* account and saving a visit puts the event in that calendar, rendered in **that calendar's color** (not the old sage green).
4. Re-opening Settings still shows the picked calendar.

- [ ] **Step 4: Open the PR**

Use the `add-pr` skill — it covers branch/commit/push, the Room-schema and screenshot gates, and the PR template. Include in the PR description:

- that pre-existing events keep their sage green, deliberately, with no cleanup pass;
- the manual verification results from Step 3;
- if any gate was regenerated by workflow dispatch rather than locally, say so.

---

## Self-review notes

**Spec coverage:** persistence + migration → Task 4. Manager split → Task 3. Shared resolution rule → Task 2. Color removal → Task 1. Write path → Task 5. ViewModel + stale-id clearing → Task 6. UI + disabled state + strings → Task 7. Gated artifacts → Tasks 4 and 8. Manual verification → Task 9. No spec section is unimplemented.

**Naming consistency, checked across tasks:** `resolvePreferred` (Tasks 2, 3, 7), `CalendarInfo(id, displayName, accountName, accountType)` (Tasks 2, 3, 6, 7, 8), `getAvailableCalendars` (Tasks 3, 6), `preferredCalendarId` (Tasks 4, 5, 6, 7, 8), `UiEvent.CalendarSelected(calendarId)` (Tasks 6, 7), `settings_calendar_selection_label` / `settings_calendar_selection_empty` (Task 7).

**Ordering constraint:** Task 3 depends on Task 2 (`CalendarInfo` moves out of `CalendarEventManager`). Task 5 depends on Tasks 3 and 4. Tasks 6–8 depend on Task 4. Do not reorder.
