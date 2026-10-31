# Visit Search by Neighborhood and City Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The visit list search box also matches the householder's neighborhood and city.

**Architecture:** Expose `householder.neighborhood` / `householder.city` through the
`visit_householder` database view (migration 17→18 recreates it), carry them into
`VisitListViewModel.VisitHouseholderState`, and match the search against name + neighborhood + city
combined. A search match keeps overriding the date/distance filters. No UI change.

**Tech Stack:** Kotlin, Room (`@DatabaseView`, KSP schema export), JUnit4 + mockito-kotlin.

Spec: `docs/superpowers/specs/2026-10-31-visit-search-neighborhood-city-design.md`

---

### Task 1: Expose neighborhood and city in the view (migration 17→18)

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitHouseholder.kt`
- Create: `app/src/main/java/com/msmobile/visitas/migration/Migration_17_18.kt`
- Modify: `app/src/main/java/com/msmobile/visitas/VisitasDatabase.kt` (import, `version = 18`, `MIGRATIONS`)
- Create (generated): `app/schemas/com.msmobile.visitas.VisitasDatabase/18.json`

- [ ] **Step 1: Add the columns to the view.** In `VisitHouseholder.kt`, after
  `h.addressLongitude as householderLongitude` add (note the comma on the preceding line):

```sql
        h.addressLongitude as householderLongitude,
        h.neighborhood as householderNeighborhood,
        h.city as householderCity
```

  and append to the data class (defaults keep existing constructors compiling):

```kotlin
    val householderLongitude: Double?,
    val householderNeighborhood: String? = null,
    val householderCity: String? = null,
)
```

- [ ] **Step 2: Create `Migration_17_18.kt`.** The SQL must match the annotation exactly (Room
  compares view SQL text).

```kotlin
package com.msmobile.visitas.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Recreates the `visit_householder` view to expose the householder's `neighborhood` and `city`
 * (added in [MIGRATION_16_17]) so the visit list search can match them.
 *
 * Room does not recreate views automatically during a migration, and it compares the view SQL
 * character-by-character with the @DatabaseView annotation, so this must match VisitHouseholder.kt
 * exactly. We use trimMargin() to keep the code readable while preserving that format.
 */
val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP VIEW IF EXISTS visit_householder")
        db.execSQL(
            """
            |CREATE VIEW `visit_householder` AS SELECT
            |        v.id as visitId,
            |        v.subject as subject,
            |        v.date as date,
            |        v.isDone as isDone,
            |        (h.isDraft OR EXISTS(SELECT 1 FROM visit vd WHERE vd.householderId = v.householderId AND vd.isDraft)) as hasDrafts,
            |        v.householderId as householderId,
            |        v.visitType as type,
            |        h.name as householderName,
            |        h.address as householderAddress,
            |        h.addressLatitude as householderLatitude,
            |        h.addressLongitude as householderLongitude,
            |        h.neighborhood as householderNeighborhood,
            |        h.city as householderCity
            |    FROM visit v
            |    JOIN householder h ON v.householderId = h.id
            |    INNER JOIN (
            |        SELECT householderId, MAX(date) as max_date
            |        FROM visit
            |        GROUP BY householderId
            |    ) latest ON v.householderId = latest.householderId
            |        AND v.date = latest.max_date
            |    ORDER BY v.householderId, v.date DESC
            |    """.trimMargin()
        )
    }
}
```

- [ ] **Step 3: Register it.** In `VisitasDatabase.kt`: add
  `import com.msmobile.visitas.migration.MIGRATION_17_18`, set `version = 18`, append
  `MIGRATION_17_18` after `MIGRATION_16_17` in `MIGRATIONS`.

- [ ] **Step 4: Export the schema.**

Run: `sh scripts/verify-room-schemas.sh --export-only`
Expected: `app/schemas/com.msmobile.visitas.VisitasDatabase/18.json` created; its view SQL contains
`householderNeighborhood` and `householderCity`.

- [ ] **Step 5: Verify the migrated DB opens on the emulator.** Install the current `master` debug
  build first (DB v17, with some visits), then install this build over it:

Run: `./gradlew installDebug` then launch the app and open the visit list.
Expected: no crash (`adb logcat` has no `IllegalStateException: Migration didn't properly handle`).

- [ ] **Step 6: Commit** (the pre-commit hook runs `BackupHandlerTest` on the connected emulator):

```bash
git add app/src/main/java/com/msmobile/visitas/visit/VisitHouseholder.kt app/src/main/java/com/msmobile/visitas/migration/Migration_17_18.kt app/src/main/java/com/msmobile/visitas/VisitasDatabase.kt app/schemas/com.msmobile.visitas.VisitasDatabase/18.json
git commit -m "Expose householder neighborhood and city in the visit_householder view"
```

Expected: hook's `BackupHandlerTest` passes.

---

### Task 2: Match the search against neighborhood and city

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitListViewModel.kt` (`filterBy` ~753–793, `asState` ~821–851, `VisitHouseholderState` ~950)
- Test: `app/src/test/java/com/msmobile/visitas/visit/VisitListViewModelTest.kt`

- [ ] **Step 1: Write the failing tests.** Add after the
  `draft visit stays visible even when search filter does not match its name` test. They use the
  `Done` date filter, which hides the (not done) default visits, so only a search match shows them.

```kotlin
    @Test
    fun `search matches the householder neighborhood and overrides the date filter`() {
        val viewModel = createViewModel(
            visits = listOf(createSearchableVisitHouseholder(neighborhood = "Centro", city = "Criciúma")),
            visitListDateFilterOption = VisitListDateFilterOption.Done
        )
        viewModel.onEvent(VisitListViewModel.UiEvent.ViewCreated)

        viewModel.onEvent(VisitListViewModel.UiEvent.SearchChanged("centro"))

        assertFalse(viewModel.uiState.value.visitList.single().hide)
    }

    @Test
    fun `search matches the householder city ignoring accents`() {
        val viewModel = createViewModel(
            visits = listOf(createSearchableVisitHouseholder(neighborhood = "Centro", city = "Criciúma")),
            visitListDateFilterOption = VisitListDateFilterOption.Done
        )
        viewModel.onEvent(VisitListViewModel.UiEvent.ViewCreated)

        viewModel.onEvent(VisitListViewModel.UiEvent.SearchChanged("criciuma"))

        assertFalse(viewModel.uiState.value.visitList.single().hide)
    }

    @Test
    fun `search words may be spread across name and neighborhood`() {
        val viewModel = createViewModel(
            visits = listOf(createSearchableVisitHouseholder(neighborhood = "Centro", city = "Criciúma")),
            visitListDateFilterOption = VisitListDateFilterOption.Done
        )
        viewModel.onEvent(VisitListViewModel.UiEvent.ViewCreated)

        viewModel.onEvent(VisitListViewModel.UiEvent.SearchChanged("Ana Centro"))

        assertFalse(viewModel.uiState.value.visitList.single().hide)
    }

    @Test
    fun `search hides the visit when a word matches no field`() {
        val viewModel = createViewModel(
            visits = listOf(createSearchableVisitHouseholder(neighborhood = "Centro", city = "Criciúma")),
            visitListDateFilterOption = VisitListDateFilterOption.Done
        )
        viewModel.onEvent(VisitListViewModel.UiEvent.ViewCreated)

        viewModel.onEvent(VisitListViewModel.UiEvent.SearchChanged("Ana Pinheirinho"))

        assertTrue(viewModel.uiState.value.visitList.single().hide)
    }

    @Test
    fun `search still matches the name when neighborhood and city are missing`() {
        val viewModel = createViewModel(
            visits = listOf(createSearchableVisitHouseholder(neighborhood = null, city = null)),
            visitListDateFilterOption = VisitListDateFilterOption.Done
        )
        viewModel.onEvent(VisitListViewModel.UiEvent.ViewCreated)

        viewModel.onEvent(VisitListViewModel.UiEvent.SearchChanged("Ana"))

        assertFalse(viewModel.uiState.value.visitList.single().hide)
    }
```

  And the helper, next to `createVisitHouseholder`:

```kotlin
    private fun createSearchableVisitHouseholder(neighborhood: String?, city: String?): VisitHouseholder {
        return VisitHouseholder(
            visitId = FIRST_VISIT_ID,
            subject = "Subject 1",
            date = LocalDateTime.now(),
            isDone = false,
            householderId = FIRST_HOUSEHOLDER_ID,
            householderName = "Ana Souza",
            householderAddress = "Rua Henrique Lage, 100",
            type = VisitType.FIRST_VISIT,
            householderLatitude = null,
            householderLongitude = null,
            householderNeighborhood = neighborhood,
            householderCity = city
        )
    }
```

- [ ] **Step 2: Run them to verify they fail.**

Run: `./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.visit.VisitListViewModelTest"`
Expected: the neighborhood, city and spread-across tests FAIL (visit hidden); the "matches no
field" and "missing" tests PASS.

- [ ] **Step 3: Carry the fields into the state.** Append to `VisitHouseholderState`:

```kotlin
        val type: VisitType,
        val householderNeighborhood: String? = null,
        val householderCity: String? = null,
    )
```

  and in `VisitHouseholder.asState`, after `type = type,`:

```kotlin
                householderNeighborhood = householderNeighborhood,
                householderCity = householderCity,
```

- [ ] **Step 4: Match the combined text in `filterBy`.** Replace

```kotlin
            val matchesName = visit.householderName.containsAllWords(filter.search)
```

  with

```kotlin
            // All search words must appear somewhere across name, neighborhood and city, so
            // "Ana Centro" finds Ana living in Centro.
            val searchableText = listOfNotNull(
                visit.householderName,
                visit.householderNeighborhood,
                visit.householderCity
            ).joinToString(" ")
            val matchesSearch = searchableText.containsAllWords(search)
```

  and update the invariant comment and `show`:

```kotlin
            //  - A search match is always visible. Typing a name, neighborhood or city is a
            //    direct ask that overrides the browsing filters.
            // Only the default, empty-search view is narrowed by date/distance. A new filter
            // belongs inside the isSearchEmpty branch — never wrap hasDrafts or matchesSearch
            // with it.
            val show = hasDrafts
                    || isSearchEmpty && (matchesDate || matchesDistance)
                    || matchesSearch
```

- [ ] **Step 5: Run the tests to verify they pass.**

Run: `./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.visit.VisitListViewModelTest"`
Expected: all PASS.

- [ ] **Step 6: Run the full unit test suite.**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit.**

```bash
git add app/src/main/java/com/msmobile/visitas/visit/VisitListViewModel.kt app/src/test/java/com/msmobile/visitas/visit/VisitListViewModelTest.kt
git commit -m "Search the visit list by householder neighborhood and city"
```

---

### Task 3: Verify on the emulator

- [ ] **Step 1:** Install the debug build, open a householder, use the address lookup so a
  neighborhood/city is saved, save it.
- [ ] **Step 2:** In the visit list, search that neighborhood, then the city, then
  `<first name> <neighborhood>`. Expected: the visit appears each time, even with a date filter
  that would hide it. Take a screenshot of each.
- [ ] **Step 3:** Search a word that matches nothing. Expected: the visit is hidden.
