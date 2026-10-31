# Visit list search by neighborhood and city — design

## Goal
The visit list search box matches the householder's neighborhood and city (saved from the
geocoder since #405), not only the name.

## Decisions
- **Same weight as a name match.** A neighborhood/city match is always shown and overrides the
  date and distance filters, exactly like a name match today.
- **Words may span fields.** All search words must appear somewhere in name + neighborhood + city
  combined, so `Ana Centro` finds Ana living in Centro. Accepted side effect: `Maria José` also
  finds a Maria living in São José.
- **No UI change.** The card does not show neighborhood/city. Highlighting what matched is a
  follow-up PR.
- **No backfill.** Householders saved before #405 have null neighborhood/city and are not found by
  them until their address is looked up again. Reverse-geocoding existing householders is a
  follow-up PR.

## Changes
1. **View** — `VisitHouseholder` selects `h.neighborhood as householderNeighborhood` and
   `h.city as householderCity`, both `String?`.
2. **Migration 17→18** — drop and recreate `visit_householder` with the new query (pattern of
   `Migration_11_12`). Bump `@Database` version, register in `VisitasDatabase.MIGRATIONS`, commit
   `app/schemas/.../18.json`.
3. **State** — `VisitListViewModel.VisitHouseholderState` carries both fields from the view.
4. **Filter** — in `VisitListViewModel.filterBy`, `matchesName` becomes `matchesSearch`:
   `listOfNotNull(name, neighborhood, city).joinToString(" ").containsAllWords(search)`. The
   invariant comment above `show` is updated to say "search match" instead of "name match".

## Testing
- `VisitListViewModelTest`: neighborhood match, city match, words across fields, a match overriding
  the date filter, null neighborhood/city still matching by name.
- `BackupHandlerTest` (pre-commit hook, connected emulator) after the `VisitasDatabase` change.
- Manual check on the emulator: migrated database opens, search by neighborhood/city works.
