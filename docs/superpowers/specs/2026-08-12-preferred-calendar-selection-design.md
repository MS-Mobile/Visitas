# Preferred calendar selection

## Problem

`CalendarEventManager.saveEvent` writes every visit event to whatever calendar
`getFirstCalendar()` scores highest (Google + primary > Google > primary > other). The user has no
say in it. A user with several writable calendars — a work account, a shared household calendar, a
dedicated ministry calendar — cannot direct visits anywhere but the one the app guesses.

## Goal

Add a calendar dropdown to the Settings screen, grouped with the existing "add visits to calendar"
checkbox, listing the device's writable calendars. Persist the choice as a stable calendar identity and
use it on the write path. Drop the app's custom event color so events take the color of whichever
calendar the user picked.

## Scope

**In scope:** selecting among calendars that already exist on the device.

**Out of scope:** creating a calendar from the app. The app cannot create a calendar that reaches
Google Calendar on the web — that needs the Calendar REST API (OAuth, Play Services, network), not
`CalendarContract`. Writing a row with `ACCOUNT_TYPE = com.google` does not work either: only
Google's sync adapter may create those, and the next full sync typically deletes anything injected.
The only thing the app could create is an `ACCOUNT_TYPE_LOCAL` calendar — device-only, hidden by the
Google Calendar app, lost on uninstall. Since events today already land in a real synced Google
calendar, offering that would be a downgrade. Users create calendars in their calendar app; this
feature only picks among them.

## Behavior

**Fallback is silent and automatic.** The stored identity is nullable. Null means "auto-pick" and
keeps today's scoring. A stored id that is no longer writable (calendar deleted, account removed)
also falls back to auto-pick, so events never silently stop being written. The dead id is cleared
lazily by the Settings screen when it loads the list — never on the write path, so a save never
blocks on a preference write.

**The dropdown is always visible, and disabled while the checkbox is off.** This keeps the section's
layout stable. The dropdown can legitimately be visible-but-disabled *and* populated: the list loads
whenever `READ_CALENDAR` is granted, regardless of the checkbox.

**The field shows the resolved calendar,** not the raw stored id, so it never reads as "nothing
selected" while events are in fact being written somewhere. When the list is empty — no permission
yet, or no writable calendars — it shows a "No calendar available" placeholder. One string covers
both cases honestly.

**A backup carries the choice safely.** Because the identity is account-based rather than a row id,
`BackupHandler.restoreDataFrom` copies it verbatim. On a device signed into the same account it
names the same calendar, so the user's choice survives moving to a new phone. Anywhere else it
simply fails to match and the app falls back to the automatic pick. No special-casing is needed in
the backup path — the identity is what makes that true.

## Design

### Persistence

**The chosen calendar is identified by account, not by row id.** `CalendarContract.Calendars._ID` is
assigned by the device's calendar provider: it is reused after a calendar is deleted, and the same
integer names an unrelated calendar on another device. Remembering one risks writing visits into a
calendar the user never picked — and worse, that failure is undetectable, because a reused id
resolves *successfully* to the wrong calendar rather than failing to resolve.

Querying a real device settled what a stable identity looks like:

- `OWNER_ACCOUNT` is populated for every calendar and is globally meaningful — an account's primary
  calendar owns its own address, a shared one looks like `family1753…@group.calendar.google.com`.
- `OWNER_ACCOUNT` alone is **not** unique. That device had two rows sharing
  `pt.brazilian#holiday@group.v.calendar.google.com` — one public holiday calendar subscribed under
  two different Google accounts. `ACCOUNT_NAME` is what separates them.
- `_SYNC_ID` was NULL on every Google calendar, so it is useless here.

So the identity is the triple `(ACCOUNT_TYPE, ACCOUNT_NAME, OWNER_ACCOUNT)`, modelled as
`CalendarIdentity` and stored as three nullable columns. Room bumps to schema **v15** with
`Migration_14_15`:

```sql
ALTER TABLE `preference` ADD COLUMN `preferredCalendarAccountType` TEXT
ALTER TABLE `preference` ADD COLUMN `preferredCalendarAccountName` TEXT
ALTER TABLE `preference` ADD COLUMN `preferredCalendarOwnerAccount` TEXT
```

All nullable with no default — null is the meaningful "auto-pick" value, not a placeholder. The
three columns are an implementation detail: `Preference.preferredCalendar` and
`Preference.withPreferredCalendar(…)` are how the rest of the app reads and writes the choice.

A calendar whose provider omits any part of the triple has no identity and is simply never matched
against a stored preference. It can still be written to as the automatic pick.

### `CalendarEventManager`

`getFirstCalendar()` currently filters and scores in one pass. The dropdown needs the full list and
the write path needs one winner, so split them:

- `private fun queryWritableCalendars(): List<CalendarInfo>` — the existing selection
  (`CALENDAR_ACCESS_LEVEL >= CAL_ACCESS_CONTRIBUTOR AND VISIBLE = 1 AND SYNC_EVENTS = 1`) unchanged,
  plus `CALENDAR_DISPLAY_NAME` and `OWNER_ACCOUNT` in the projection. It maps the cursor and
  delegates ranking to `orderedByAutoPickPreference()`, so the auto-pick candidate is always first.
- `private fun resolveCalendar(preferred: CalendarIdentity?): CalendarInfo?` — delegates to
  `resolvePreferred` (below). Replaces `getFirstCalendar()`, which is removed.
- `suspend fun getAvailableCalendars(): List<CalendarInfo>` — public; returns `emptyList()` without
  `READ_CALENDAR`, and also on a provider error, which is logged.
- `saveEvent(…, calendar: CalendarIdentity? = null)` routes through `resolveCalendar`. The default
  argument keeps existing call sites compiling with today's behavior.

`CalendarInfo` becomes public and gains a display name, the owner account, and the primary flag:

```kotlin
data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String?,
    val ownerAccount: String?,
    val accountType: String?,
    val isPrimary: Boolean
)
```

`accountType` and `isPrimary` are the inputs to the automatic ranking; `accountName` doubles as the
dropdown's secondary label and as part of the identity; `ownerAccount` completes the identity. `id`
is still carried because the provider needs it to write the event — it is simply never persisted.

`calculateCalendarScore` and `GOOGLE_ACCOUNT_TYPE` leave `CalendarEventManager` entirely — the
ranking moves to `CalendarSelection.kt` (below), which leaves this class holding only what genuinely
needs `ContentResolver`.

### Removing event colors

Custom event colors are removed rather than carried across the change. Event color palettes are
per-account (`queryEventColors` filters on `ACCOUNT_NAME`/`ACCOUNT_TYPE`), so letting the user pick a
calendar on a different account would mean querying the right palette per save or silently dropping
the color key — complexity in service of one hardcoded green. An event with no `EVENT_COLOR_KEY`
renders in its calendar's own color, which is the better default anyway: visits look like they belong
to the calendar the user chose.

Every reference is internal to `CalendarEventManager` — `getAvailableColors`, `getDefaultColorKey`,
`ColorKey`, and `EventColor` are public API with no callers anywhere in the repo, tests included. So
this is a pure deletion, roughly 60 lines, with no call-site churn:

- the `color` parameter on `saveEvent`
- the palette-check block in the event's `ContentValues`, including the `EVENT_COLOR_KEY` column
  itself — it is not written at all, not written as null
- `getAvailableColors`, `getDefaultColorKey`, `queryEventColors`
- `ColorKey`, `EventColor`, `DEFAULT_COLOR_KEY`

**Events already created keep their sage green.** Nothing rewrites them, and no cleanup pass is
added. Events created from now on inherit their calendar's color, so older visits stay green
indefinitely. This is accepted deliberately: a one-time pass over every stored `calendarEventId` is
not worth the write traffic for a cosmetic difference.

### One copy of the selection rule

Both the write path and the Settings screen need the resolved calendar. Rather than duplicating the
rule, `CalendarSelection.kt` in the `util` package owns the whole of it — the ranking as well as the
lookup, because the fallback *is* the ranking and splitting them would leave the half that can
actually be wrong sitting untested inside a `ContentResolver` wrapper:

```kotlin
fun List<CalendarInfo>.orderedByAutoPickPreference(): List<CalendarInfo> =
    sortedByDescending { it.autoPickScore() }

fun List<CalendarInfo>.resolvePreferred(preferred: CalendarIdentity?): CalendarInfo? =
    preferred?.let { wanted -> firstOrNull { it.identity == wanted } } ?: firstOrNull()
```

The `preferred?.let { }` wrapper is load-bearing. A plain `firstOrNull { it.identity == preferred }`
would, when no calendar is chosen, match the first calendar that happens to *have* no identity —
returning an arbitrary calendar instead of falling back to the best-ranked one.

`CalendarEventManager.resolveCalendar` and `SettingsDetailViewModel` both call `resolvePreferred`;
`queryWritableCalendars` is the sole caller of `orderedByAutoPickPreference`. One rule, one test
target.

**The sort must stay stable and must not gain a tie-break.** `getFirstCalendar` picked with
`if (score > bestScore)` — strict `>`, so equal-scoring calendars resolved to the first cursor row.
`sortedByDescending` is stable and preserves exactly that. Adding a secondary sort key (by display
name, say) would move the automatic pick for any user whose best-scoring band holds two or more
calendars, relocating their events on upgrade with no warning.

### Write path

`SyncVisitCalendarEventUseCase.invoke` gains `preferredCalendar: CalendarIdentity?` and forwards it
to `saveEvent`. Both call sites — `VisitDetailViewModel.kt:1039` and `VisitListViewModel.kt:261` —
already read `Preference` before invoking the use case, so they pass `preference.preferredCalendar`
with no extra query.

### Settings UI

`CalendarSelectionDropdown`, modeled on the existing `MapEngineDropdown`, sits inside the
`settings_section_calendar` block below the checkbox with an 8.dp spacer.

`ExposedDropdownMenuBox` has no `enabled` parameter. The disabled state needs both:

- `enabled = uiState.addVisitsToCalendar` on the anchor `OutlinedTextField`, which gives the Material
  greyed-out treatment;
- `onExpandedChange = { if (enabled) expanded = it }`, or the menu still opens on tap.

Menu items show the display name as primary text with the account name beneath — two calendars named
"Personal" on different accounts are otherwise indistinguishable. The collapsed field shows the
display name only.

### `SettingsDetailViewModel`

`UiState` gains `availableCalendars: List<CalendarInfo> = emptyList()` and
`preferredCalendar: CalendarIdentity? = null`. A new `UiEvent.CalendarSelected(calendar: CalendarInfo)`
persists `preference.withPreferredCalendar(calendar.identity)` and updates state, following the shape
of `mapEngineSelected`.

The list loads in `viewCreated()` when `hasCalendarPermission()` is true, and again in
`calendarPermissionGranted()` after `saveAddVisitsToCalendar` — so enabling the checkbox populates
the dropdown in the same pass. Both load paths perform the lazy stale-choice clear: if a stored
identity matches nothing in the freshly loaded list, persist null.

## Testing

`CalendarEventManager` has no test file — it is `ContentResolver` all the way down, which is why
neither the ranking nor the fallback has ever been directly tested. Moving both into
`CalendarSelection.kt` makes the whole selection policy testable without a provider.

**`resolvePreferred`** (new test): preferred identity present → returns it; absent → returns first;
preferred null → returns first; empty list → null; a case pinning that it trusts the receiver's
order rather than re-ranking, so slipping a sort in at a call site fails loudly instead of showing
one calendar while writing to another; and the two cases that justify identity at all — the same
shared calendar subscribed under two accounts stays distinguishable, and a calendar whose row id
changed still matches. Plus the null-preference trap: a calendar with no identity must not be
matched when nothing is chosen.

**`orderedByAutoPickPreference`** (new test): Google primary ranks first; a secondary Google calendar
outranks a non-Google primary; equal-ranked calendars keep provider order (the stability guarantee
above); an empty list comes back empty.

**`SettingsDetailViewModelTest`** (existing Mockito + `createViewModel(...)` + `MockReferenceHolder`
pattern; `createViewModel` gains `availableCalendars` and `savedPreferredCalendar` parameters):

- `ViewCreated` with permission granted loads the calendars into state
- `ViewCreated` without permission leaves the list empty and never calls `getAvailableCalendars`
  (`verifyBlocking(…, never())`)
- `ViewCreated` with a stored identity matching nothing in the list saves the preference with the
  choice cleared (`argThat`)
- `CalendarSelected` updates state and persists the identity
- `CalendarPermissionGranted` loads the list

**`BackupHandlerTest`** (instrumented): a restored backup keeps the chosen calendar, since the
identity stays meaningful across the trip — the case that a row id could not have satisfied.

**Expected fallout:** `SyncVisitCalendarEventUseCase.invoke` gains a parameter, so verification
blocks in `VisitDetailViewModelTest` and `VisitListViewModelTest` need the extra argument. A `= null`
default keeps production call sites compiling but does not spare the mock verifications.

**Manual verification** (via the `verify` skill, on an emulator with two accounts) — neither is
worth faking:

- an event actually lands in a non-default, cross-account calendar, rendered in that calendar's
  color;
- the dropdown is inert while the checkbox is off;
- a chosen calendar is still chosen after the app is force-stopped and reopened, proving the
  identity round-trips through the provider rather than merely through memory.

## Gated artifacts

Regenerate and commit before opening the PR; do not hand-edit (see the `add-pr` skill):

1. **Room schema** — `app/schemas/com.msmobile.visitas.VisitasDatabase/15.json`, generated by
   building after the entity and migration land.
2. **Screenshot baselines** — a new *"Calendar Selected"* config in `SettingsPreviewConfigProvider`
   (checkbox on, populated list, one selected), plus the existing *"Add Visits To Calendar Enabled"*
   config, which now also renders the dropdown. One new baseline, one changed baseline.
3. **Strings in all three locales** — `values`, `values-pt-rBR`, `values-b+es+419`: the dropdown
   label ("Calendar") and the empty placeholder ("No calendar available").
