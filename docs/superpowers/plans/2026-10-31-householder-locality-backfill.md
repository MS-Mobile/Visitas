# Householder Neighborhood and City Backfill Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reverse-geocode householders that have coordinates but no neighborhood/city, in the
background, at app start and after a backup restore, until one run gets an answer for every one.

**Architecture:** A `@Singleton` `HouseholderLocalityBackfill` runs in an application-wide coroutine
scope. It asks `AddressProvider.lookupLocality` for each candidate from
`HouseholderDao.listMissingLocality()`, fills only null fields through a coordinate-guarded
`fillLocality` update, and marks a `SharedPreferences` flag done only when no lookup failed.
`VisitasApp` calls `start()`; `BackupViewModel` calls `restart()` after a successful restore.

**Tech Stack:** Kotlin coroutines, Room, Hilt, `SharedPreferences` (core-ktx `edit`), JUnit4 +
mockito-kotlin, Hilt instrumented tests with the real `Geocoder`.

Spec: `docs/superpowers/specs/2026-10-31-householder-locality-backfill-design.md`

---

### Task 1: Locality lookup in `AddressProvider`

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/util/AddressProvider.kt`

No unit test: `Geocoder` is a platform stub on the JVM. Covered by the instrumented test (Task 6).

- [ ] **Step 1: Add the public API** below `getAddressFromLatLong`:

```kotlin
    /** Wraps the static [Geocoder.isPresent] so callers stay mockable. */
    fun isGeocoderAvailable(): Boolean = Geocoder.isPresent()

    /**
     * Looks up only the neighborhood and city of a point. Unlike [getAddressFromLatLong], an answer
     * without a street still counts as [LocalityLookup.Found], and a failure is reported apart from
     * an empty answer so callers can retry it. An empty result list is a failure: the points looked
     * up already carry a street address, so "nothing here" most likely means no connectivity.
     */
    suspend fun lookupLocality(latitude: Double, longitude: Double): LocalityLookup {
        return try {
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getLocalityAddressesAsync(latitude, longitude)
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)
            }
            val address = addresses?.firstOrNull() ?: return LocalityLookup.Failed
            LocalityLookup.Found(neighborhood = address.neighborhood, city = address.city)
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            logger.error(TAG, "Failed to look up locality", error)
            LocalityLookup.Failed
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun getLocalityAddressesAsync(
        latitude: Double,
        longitude: Double
    ): List<Address>? {
        return withTimeoutOrNull(LOCALITY_LOOKUP_TIMEOUT) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(
                    latitude,
                    longitude,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            cont.resumeIfActive(addresses)
                        }

                        override fun onError(errorMessage: String?) {
                            logger.error(TAG, "Failed to look up locality: $errorMessage")
                            cont.resumeIfActive(null)
                        }
                    }
                )
            }
        }
    }
```

- [ ] **Step 2: Share the mapping.** Replace `toAddressSpecs` and its KDoc with:

```kotlin
    /**
     * The neighborhood (Brazilian "bairro") is the geocoder's sub-locality. Brazilian results
     * often leave [Address.getLocality] empty and carry the city in the sub-admin area instead.
     */
    private val Address.neighborhood: String? get() = subLocality
    private val Address.city: String? get() = locality ?: subAdminArea

    private fun Address.toAddressSpecs(): AddressSpecs.Data =
        AddressSpecs.Data(
            address = toStreetAddressString(),
            latitude = latitude,
            longitude = longitude,
            neighborhood = neighborhood,
            city = city
        )
```

- [ ] **Step 3: Add the result type** after `AddressSpecs`:

```kotlin
    sealed class LocalityLookup {
        data class Found(val neighborhood: String?, val city: String?) : LocalityLookup()
        data object Failed : LocalityLookup()
    }
```

  and the constant `private const val LOCALITY_LOOKUP_TIMEOUT = 10_000L` in the companion.

- [ ] **Step 4: Commit** — `Add a locality-only lookup to AddressProvider`.

### Task 2: DAO queries

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/householder/HouseholderDao.kt`
- Test: `app/src/androidTest/java/com/msmobile/visitas/householder/HouseholderDaoTest.kt`

- [ ] **Step 1: Add the queries.**

```kotlin
    @Query(
        "SELECT * FROM Householder WHERE addressLatitude IS NOT NULL AND addressLongitude IS NOT NULL " +
            "AND (neighborhood IS NULL OR city IS NULL)"
    )
    suspend fun listMissingLocality(): List<Householder>

    /**
     * Fills only the null locality fields, and only while the householder still sits at the looked
     * up coordinates, so a pin moved during a backfill never receives the old point's locality.
     */
    @Query(
        "UPDATE Householder SET neighborhood = COALESCE(neighborhood, :neighborhood), " +
            "city = COALESCE(city, :city) WHERE id = :id AND addressLatitude = :latitude " +
            "AND addressLongitude = :longitude"
    )
    suspend fun fillLocality(
        id: UUID,
        latitude: Double,
        longitude: Double,
        neighborhood: String?,
        city: String?
    )
```

- [ ] **Step 2: Add DAO tests** (deterministic, no geocoder): `listMissingLocality` returns only
  householders with coordinates and a null field; `fillLocality` keeps an existing neighborhood,
  fills a null city, and does nothing when the coordinates differ.
- [ ] **Step 3: Commit** — `Add householder queries for the locality backfill`.

### Task 3: Flag and application scope

**Files:**
- Create: `app/src/main/java/com/msmobile/visitas/util/LocalityBackfillFlag.kt`
- Create: `app/src/main/java/com/msmobile/visitas/di/ApplicationScope.kt`
- Modify: `app/src/main/java/com/msmobile/visitas/di/ApplicationModule.kt`

- [ ] **Step 1: Flag.**

```kotlin
package com.msmobile.visitas.util

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Remembers that the householder locality backfill finished a run with no failed lookup. Kept
 * outside the database on purpose: the backfill needs no schema change.
 */
class LocalityBackfillFlag(private val preferences: SharedPreferences) {
    fun isDone(): Boolean = preferences.getBoolean(KEY_DONE, false)

    fun markDone() = preferences.edit { putBoolean(KEY_DONE, true) }

    fun clear() = preferences.edit { remove(KEY_DONE) }

    private companion object {
        private const val KEY_DONE = "householder_locality_backfill_done"
    }
}
```

- [ ] **Step 2: Qualifier.**

```kotlin
package com.msmobile.visitas.di

import javax.inject.Qualifier

/** A scope that lives as long as the process, for work no screen should cancel. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
```

- [ ] **Step 3: Providers** in `ApplicationModule`:

```kotlin
    @Singleton
    @Provides
    @ApplicationScope
    fun applicationScope(dispatcherProvider: DispatcherProvider): CoroutineScope {
        return CoroutineScope(SupervisorJob() + dispatcherProvider.io)
    }

    @Singleton
    @Provides
    fun localityBackfillFlag(@ApplicationContext context: Context): LocalityBackfillFlag {
        return LocalityBackfillFlag(
            context.getSharedPreferences("locality_backfill", Context.MODE_PRIVATE)
        )
    }
```

- [ ] **Step 4: Commit** — `Add the backfill flag and an application coroutine scope`.

### Task 4: `HouseholderLocalityBackfill` (TDD)

**Files:**
- Create: `app/src/main/java/com/msmobile/visitas/householder/HouseholderLocalityBackfill.kt`
- Test: `app/src/test/java/com/msmobile/visitas/householder/HouseholderLocalityBackfillTest.kt`

- [ ] **Step 1: Write the unit tests** (factory-built mocks, `MainDispatcherRule`, `runTest` with
  the test's own scope passed in): flag done → no lookup; geocoder absent → no lookup, flag not set;
  all found → each filled, flag set; one failed → others filled, flag not set; found with nulls →
  filled with nulls, flag set; DAO throws → error logged, flag not set; `start()` while active →
  same job, one lookup; `restart()` while active → first cancelled, flag cleared, new run completes.
- [ ] **Step 2: Run, expect compile failure.**
  `./gradlew testDebugUnitTest --tests "com.msmobile.visitas.householder.HouseholderLocalityBackfillTest"`
- [ ] **Step 3: Implement.**

```kotlin
@Singleton
class HouseholderLocalityBackfill @Inject constructor(
    private val householderDao: HouseholderDao,
    private val addressProvider: AddressProvider,
    private val backfillFlag: LocalityBackfillFlag,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
    private val logger: Logger
) {
    private val lock = Any()
    private var activeRun: Job? = null

    fun start(): Job = synchronized(lock) {
        activeRun?.takeIf { it.isActive } ?: launchRun(previousRun = null, clearFlag = false)
    }

    fun restart(): Job = synchronized(lock) {
        launchRun(previousRun = activeRun, clearFlag = true)
    }

    private fun launchRun(previousRun: Job?, clearFlag: Boolean): Job {
        return applicationScope.launch {
            previousRun?.cancelAndJoin()
            if (clearFlag) backfillFlag.clear()
            backfill()
        }.also { activeRun = it }
    }

    private suspend fun backfill() {
        try {
            if (backfillFlag.isDone()) return
            if (!addressProvider.isGeocoderAvailable()) return
            var anyLookupFailed = false
            householderDao.listMissingLocality().forEach { householder ->
                val latitude = householder.addressLatitude ?: return@forEach
                val longitude = householder.addressLongitude ?: return@forEach
                when (val lookup = addressProvider.lookupLocality(latitude, longitude)) {
                    is LocalityLookup.Found -> householderDao.fillLocality(
                        id = householder.id,
                        latitude = latitude,
                        longitude = longitude,
                        neighborhood = lookup.neighborhood,
                        city = lookup.city
                    )
                    LocalityLookup.Failed -> anyLookupFailed = true
                }
            }
            if (!anyLookupFailed) backfillFlag.markDone()
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            logger.error(TAG, "Householder locality backfill failed", error)
        }
    }
}
```

- [ ] **Step 4: Run the tests, expect PASS.**
- [ ] **Step 5: Commit** — `Backfill householder neighborhood and city in the background`.

### Task 5: Triggers

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/VisitasApp.kt`
- Modify: `app/src/main/java/com/msmobile/visitas/backup/BackupViewModel.kt`
- Test: `app/src/test/java/com/msmobile/visitas/backup/BackupViewModelTest.kt`

- [ ] **Step 1:** `VisitasApp` injects `HouseholderLocalityBackfill` and calls `start()` at the end
  of `onCreate`.
- [ ] **Step 2:** `BackupViewModel` takes `householderLocalityBackfill` and calls `restart()` in the
  restore `onSuccess`. Factory gains `backfillRef`; tests: success → `restart()` once, failure →
  never.
- [ ] **Step 3:** `./gradlew testDebugUnitTest --tests "com.msmobile.visitas.backup.BackupViewModelTest"` → PASS.
- [ ] **Step 4: Commit** — `Start the locality backfill at launch and after a restore`.

### Task 6: Instrumented integration test (real geocoder)

**Files:**
- Create: `app/src/androidTest/java/com/msmobile/visitas/householder/HouseholderLocalityBackfillTest.kt`

- [ ] **Step 1:** Write the test per the spec's Testing section (`@HiltAndroidTest`, real database,
  real `AddressProvider`; `assumeTrue(lookup is Found)` precondition; Criciúma center
  `-28.6775, -49.3697`; cases: fills, keeps existing values, skips no-coordinate householders,
  after a restore).
- [ ] **Step 2:** Run on an emulator with network:
  `./gradlew connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.msmobile.visitas.householder.HouseholderLocalityBackfillTest"`
- [ ] **Step 3: Commit** — `Add an instrumented test for the locality backfill`.

### Task 7: Ship

- [ ] Full `./gradlew testDebugUnitTest`, then open the PR with the `add-pr` skill and release
  with the `release-pipeline` skill.
