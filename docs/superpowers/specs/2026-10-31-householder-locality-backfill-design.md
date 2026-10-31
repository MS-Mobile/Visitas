# Householder neighborhood and city backfill — design

## Goal
Householders saved before #405, and those restored from older backups, have coordinates and a street
address but null `neighborhood`/`city`, so the visit list search (#407) cannot find them by either.
Reverse-geocode them in the background until every one has been looked up successfully once.

## Decisions
- **No schema change.** No column on `householder` or `preference`. Progress lives in one
  `SharedPreferences` boolean ("backfill done") outside the database.
- **The flag only flips on a clean run.** A run sets it only if every lookup got an answer from the
  geocoder. Any failed lookup leaves it unset, so the next app launch goes through the remaining
  entries. A process kill mid-run also leaves it unset.
- **A householder is picked while either field is null.** The geocoder answering without a
  neighborhood leaves that field null, so the householder is looked up again on later runs — but
  runs only repeat while some lookup keeps failing. The first fully answered run sets the flag and
  ends it.
- **Empty answer = failure.** Every candidate already has a street address, so an empty result list
  most likely means no connectivity, not "nothing here". Counting it as a failure keeps an offline
  run from setting the flag.
- **Only null fields are filled, and only for the coordinates that were looked up.** A value already
  present is never overwritten, and if the user moves the pin while a run is in progress the old
  coordinates' locality is not written onto the new location.
- **Runs outside any screen.** Triggered at app start and after a successful restore, in an
  application-wide coroutine scope, so leaving the backup sheet does not cancel it. No WorkManager.

## Changes
1. **Lookup** — `AddressProvider.lookupLocality(latitude, longitude): LocalityLookup`, a sealed
   result nested in `AddressProvider`:
   - `Found(neighborhood: String?, city: String?)` when the geocoder returns at least one address,
     whether or not it has a street (`thoroughfare`), mapped with the same rules as
     `toAddressSpecs` (`subLocality`; `locality ?: subAdminArea`).
   - `Failed` on an exception, a timeout, or an empty list.

   The existing `getAddressFromLatLong` is unchanged; it collapses all of these into `NoData`, which
   the backfill must tell apart. `AddressProvider.isGeocoderAvailable()` wraps the static
   `Geocoder.isPresent()` so the backfill never calls it directly and tests can mock it.
2. **DAO** — `HouseholderDao`:
   - `listMissingLocality()`: householders with non-null latitude and longitude and a null
     neighborhood or city.
   - `fillLocality(id, latitude, longitude, neighborhood, city)`:
     `UPDATE householder SET neighborhood = COALESCE(neighborhood, :neighborhood),
     city = COALESCE(city, :city) WHERE id = :id AND addressLatitude = :latitude
     AND addressLongitude = :longitude`.
3. **Flag** — `util/LocalityBackfillFlag`, a `@Singleton` wrapping one `SharedPreferences` file with
   `isDone()`, `markDone()`, `clear()`. Provided in `ApplicationModule`.
4. **Application scope** — `ApplicationModule` provides a `@Singleton` `CoroutineScope`
   (`SupervisorJob() + dispatcherProvider.io`) under an `@ApplicationScope` qualifier.
5. **Backfill** — `householder/HouseholderLocalityBackfill`, a `@Singleton` taking the DAO,
   `AddressProvider`, the flag, the application scope and `Logger`:
   - `start(): Job`: launches a run unless one is already active, and returns the active run.
   - `restart(): Job`: launches a job that cancels and joins the active run, clears the flag, then
     runs. Used after a restore, whose data the in-flight run did not see; skipping instead would
     let the stale run set the flag and leave the restored householders untouched.
   - Both return the `Job` so the instrumented test can `join()` it; production callers ignore it.
   - A run: returns if the flag is set; returns without setting it when
     `addressProvider.isGeocoderAvailable()` is false; otherwise looks up each householder from `listMissingLocality()` one at a time, calls
     `fillLocality` for each `Found`, and calls `markDone()` only if none was `Failed`. Failures are
     logged, not thrown.
6. **Triggers**
   - `VisitasApp.onCreate` calls `start()`.
   - `BackupViewModel.handleRestoreBackup` calls `restart()` on success.

## Testing
`HouseholderLocalityBackfillTest` (unit, mocks built in a factory per `AGENTS.md`):
- flag already set → no lookup;
- geocoder not present → no lookup, flag not set;
- all lookups `Found` → each filled, flag set;
- one lookup `Failed` → the others still filled, flag not set;
- `Found` with null fields → `fillLocality` called with nulls, flag set;
- `start()` while a run is active → no second run;
- `restart()` while a run is active → first run cancelled, flag cleared, a new run performed.

`BackupViewModelTest`: a successful restore calls `restart()`; a failed one does not.

`HouseholderLocalityBackfillTest` (instrumented, `app/src/androidTest/.../householder/`), in the
style of `BackupHandlerTest`: `@HiltAndroidTest`, the real `VisitasDatabase`, `AddressProvider`
(real `Geocoder`), flag and backfill injected, the database deleted and the flag cleared in
`@After`. It exercises the real geocoder, so it needs network:
- **Precondition:** each test first calls `addressProvider.lookupLocality` for its coordinates and
  `assumeTrue(it is Found)`, so an offline emulator or a device without a geocoder *skips* the test
  instead of failing it.
- **Fills a householder:** one householder at a fixed point in Criciúma (center), null
  neighborhood/city → `restart().join()` → city is `Criciúma`, neighborhood is non-null, the flag is
  set.
- **Keeps existing values:** a householder at the same point with `neighborhood = "Manual"` and null
  city → after the run, neighborhood is still `Manual` and city is filled.
- **Skips householders without coordinates:** one with null latitude/longitude is untouched.
- **After a restore:** create a backup holding a householder with null locality, restore it through
  `BackupHandler`, `restart().join()` → the restored householder is filled.

The assertions on the exact city depend on Google's geocoder data for that point; the test pins the
expected value and a change in that data would surface as a failure there.

Manual check on the emulator: a householder with null locality gets it filled after launch; an
offline launch leaves the flag unset and the next online launch completes it.
