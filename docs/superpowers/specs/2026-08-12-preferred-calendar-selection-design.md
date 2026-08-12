# Preferred calendar selection

## Problem

`CalendarEventManager.saveEvent` writes every visit event to whatever calendar
`getFirstCalendar()` scores highest (Google + primary > Google > primary > other). The user has no
say in it. A user with several writable calendars — a work account, a shared household calendar, a
dedicated ministry calendar — cannot direct visits anywhere but the one the app guesses.

## Goal

Add a calendar dropdown to the Settings screen, grouped with the existing "add visits to calendar"
checkbox, listing the device's writable calendars. Persist the choice as `preferredCalendarId` and
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

**Fallback is silent and automatic.** `preferredCalendarId` is nullable. Null means "auto-pick" and
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

## Design

### Persistence

`Preference` gains `val preferredCalendarId: Long? = null`. Room bumps to schema **v15** with
`Migration_14_15`:

```sql
ALTER TABLE `preference` ADD COLUMN `preferredCalendarId` INTEGER
```

Nullable with no default — null is the meaningful "auto-pick" value, not a placeholder.

### `CalendarEventManager`

`getFirstCalendar()` currently filters and scores in one pass. The dropdown needs the full list and
the write path needs one winner, so split them:

- `private fun queryWritableCalendars(): List<CalendarInfo>` — the existing selection
  (`CALENDAR_ACCESS_LEVEL >= CAL_ACCESS_CONTRIBUTOR AND VISIBLE = 1 AND SYNC_EVENTS = 1`) unchanged,
  plus `CALENDAR_DISPLAY_NAME` in the projection. It maps the cursor and delegates ranking to
  `orderedByAutoPickPreference()`, so the auto-pick candidate is always first.
- `private fun resolveCalendar(preferredCalendarId: Long?): CalendarInfo?` — delegates to
  `resolvePreferred` (below). Replaces `getFirstCalendar()`, which is removed.
- `suspend fun getAvailableCalendars(): List<CalendarInfo>` — public; returns `emptyList()` without
  `READ_CALENDAR`.
- `saveEvent(…, calendarId: Long? = null)` routes through `resolveCalendar`. The default argument
  keeps existing call sites compiling with today's behavior.

`CalendarInfo` becomes public and gains a display name and the primary flag:

```kotlin
data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String?,
    val accountType: String?,
    val isPrimary: Boolean
)
```

`accountType` and `isPrimary` stay as the inputs to the automatic ranking; `accountName` stays as the
dropdown's secondary label.

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

fun List<CalendarInfo>.resolvePreferred(preferredCalendarId: Long?): CalendarInfo? =
    firstOrNull { it.id == preferredCalendarId } ?: firstOrNull()
```

`CalendarEventManager.resolveCalendar` and `SettingsDetailViewModel` both call `resolvePreferred`;
`queryWritableCalendars` is the sole caller of `orderedByAutoPickPreference`. One rule, one test
target.

**The sort must stay stable and must not gain a tie-break.** `getFirstCalendar` picked with
`if (score > bestScore)` — strict `>`, so equal-scoring calendars resolved to the first cursor row.
`sortedByDescending` is stable and preserves exactly that. Adding a secondary sort key (by display
name, say) would move the automatic pick for any user whose best-scoring band holds two or more
calendars, relocating their events on upgrade with no warning.

### Write path

`SyncVisitCalendarEventUseCase.invoke` gains `preferredCalendarId: Long?` and forwards it to
`saveEvent`. Both call sites — `VisitDetailViewModel.kt:1039` and `VisitListViewModel.kt:261` —
already read `Preference` before invoking the use case, so they pass `preference.preferredCalendarId`
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
`preferredCalendarId: Long? = null`. A new `UiEvent.CalendarSelected(calendarId: Long)` persists
`preference.copy(preferredCalendarId = …)` and updates state, following the shape of
`mapEngineSelected`.

The list loads in `viewCreated()` when `hasCalendarPermission()` is true, and again in
`calendarPermissionGranted()` after `saveAddVisitsToCalendar` — so enabling the checkbox populates
the dropdown in the same pass. Both load paths perform the lazy stale-id clear: if a stored id is
absent from the freshly loaded list, persist null.

## Testing

`CalendarEventManager` has no test file — it is `ContentResolver` all the way down, which is why
neither the ranking nor the fallback has ever been directly tested. Moving both into
`CalendarSelection.kt` makes the whole selection policy testable without a provider.

**`resolvePreferred`** (new test): preferred id present → returns it; preferred id absent (stale) →
returns first; preferred null → returns first; empty list → null; and a case pinning that it trusts
the receiver's order rather than re-ranking, so slipping a sort in at a call site fails loudly
instead of showing one calendar while writing to another.

**`orderedByAutoPickPreference`** (new test): Google primary ranks first; a secondary Google calendar
outranks a non-Google primary; equal-ranked calendars keep provider order (the stability guarantee
above); an empty list comes back empty.

**`SettingsDetailViewModelTest`** (existing Mockito + `createViewModel(...)` + `MockReferenceHolder`
pattern; `createViewModel` gains `availableCalendars` and `savedPreferredCalendarId` parameters):

- `ViewCreated` with permission granted loads the calendars into state
- `ViewCreated` without permission leaves the list empty and never calls `getAvailableCalendars`
  (`verifyBlocking(…, never())`)
- `ViewCreated` with a stored id absent from the list saves the preference with
  `preferredCalendarId = null` (`argThat`)
- `CalendarSelected` updates state and persists the id
- `CalendarPermissionGranted` loads the list

**Expected fallout:** `SyncVisitCalendarEventUseCase.invoke` gains a parameter, so verification
blocks in `VisitDetailViewModelTest` and `VisitListViewModelTest` need the extra argument. A `= null`
default keeps production call sites compiling but does not spare the mock verifications.

**Manual verification** (via the `verify` skill, on an emulator with two accounts) — neither is
worth faking:

- an event actually lands in a non-default, cross-account calendar, rendered in that calendar's
  color;
- the dropdown is inert while the checkbox is off.

## Gated artifacts

Regenerate and commit before opening the PR; do not hand-edit (see the `add-pr` skill):

1. **Room schema** — `app/schemas/com.msmobile.visitas.VisitasDatabase/15.json`, generated by
   building after the entity and migration land.
2. **Screenshot baselines** — a new *"Calendar Selected"* config in `SettingsPreviewConfigProvider`
   (checkbox on, populated list, one selected), plus the existing *"Add Visits To Calendar Enabled"*
   config, which now also renders the dropdown. One new baseline, one changed baseline.
3. **Strings in all three locales** — `values`, `values-pt-rBR`, `values-b+es+419`: the dropdown
   label ("Calendar") and the empty placeholder ("No calendar available").
