# Contact the Visit Householder

**Date:** 2026-07-09
**Status:** Implemented (PR #284)
**Area:** `VisitDetailScreen` top bar, `Householder` entity, contact intents (`ContextExtension`)

## Problem

The visit detail screen shows a householder (name, address, notes, preferred
day/time) and their visits, but offers no way to actually **reach** the person.
A field worker who wants to call or message a householder has to leave the app,
copy the number from somewhere else, and paste it into their dialer.

We want a one-tap contact affordance on the visit detail screen that lets the
user call, text, or WhatsApp the householder — and, when no number is on file
yet, capture one inline.

## Core insight

Two facts shape the whole design:

1. **The phone number belongs to the householder, not the visit.** A visit is an
   event; the person being visited is the `Householder`. So the number is stored
   on the `Householder` entity and is edited through the same editable-state +
   auto-save machinery as name/address/notes — no new persistence path.

2. **A native `tel:`/`smsto:` chooser can never list WhatsApp.** WhatsApp does not
   register an intent filter for `tel:` URIs, so if we simply fired
   `ACTION_DIAL`/`ACTION_SENDTO` and let Android show its chooser, WhatsApp would
   be absent. To offer WhatsApp we must launch it explicitly via its web link
   (`https://wa.me/<digits>`). That single constraint is why the "number exists"
   path is an **in-app options sheet** we control, not a system chooser.

The contact button therefore has two behaviors, keyed on whether a number is
saved, and the whole feature adds **no new screen or navigation destination** —
just a top-bar action plus a dialog and a bottom sheet driven by transient UI
flags.

## Decisions

| # | Decision | Choice |
|---|----------|--------|
| 1 | Where the number lives | Nullable `phoneNumber` column on `Householder` (and its `householder_snapshot` mirror), round-tripped through `EditableHouseholderData` so existing auto-save persists it |
| 2 | Entry point | A single `Icons.Rounded.Call` action in the visit detail top bar (`visitDetailTopBarActions`) |
| 3 | Tap with **no** number | Open an `AlertDialog` with one phone `TextField` to capture it |
| 4 | Tap with a number | Open an in-app `ModalBottomSheet` of actions — **Call · Message (SMS) · WhatsApp · Edit number** |
| 5 | Why a sheet, not a chooser | WhatsApp doesn't handle `tel:`; an in-app sheet is the only way to include it (Core insight #2) |
| 6 | Calling mechanism | `ACTION_DIAL` (opens dialer prefilled) — **no `CALL_PHONE` permission**, no runtime prompt |
| 7 | WhatsApp mechanism | `ACTION_VIEW https://wa.me/<digits>` — opens the app if installed, else WhatsApp Web; digits sanitized (strip `+`, spaces, dashes) |

## State model

- Add `val phoneNumber: String? = null` to the `Householder` entity (last field,
  mirroring how `isDraft` was added in `Migration_10_11`).
- The field is threaded through the ViewModel's editable state so change-detection,
  undo, and the debounced auto-save pick it up **for free** (they key off
  `householder.editable`):
  - `EditableHouseholderData.phoneNumber: String?`
  - `HouseholderState.phoneNumber` accessor
  - `Householder.asState` maps entity → state; `HouseholderState.asModel` maps
    state → entity as `phoneNumber?.trim()?.ifBlank { null }` (blank ⇒ stored `null`).
- Two transient UI flags on `UiState`, mirroring the existing permission-dialog
  booleans (they are pure view state, never persisted):
  - `showPhoneInputDialog: Boolean = false`
  - `showPhoneOptionsSheet: Boolean = false`

Saving from the dialog dispatches `HouseholderPhoneChanged`, which mutates
`editable.phoneNumber`; the existing 250 ms-debounced auto-save then persists it.
No dedicated save button or repository call is introduced.

## Behavior

### The top-bar action

`visitDetailTopBarActions` gains a third `TopBarAction` (`Icons.Rounded.Call`,
`contentDescription = contact_householder`) that dispatches `PhoneClicked`.
`VisitDetailScreenDestination` is already in `AppScaffold`'s `showTopBar` list, so
nothing else is needed for it to render.

### ① Tap with no saved number → input dialog

`PhoneClicked` checks `householder.phoneNumber.isNullOrBlank()`. If blank, it sets
`showPhoneInputDialog = true`. `VisitDetailScreenContent` renders a `PhoneInputDialog`
(`AlertDialog`) with a single `TextField`
(`KeyboardOptions(keyboardType = KeyboardType.Phone)`), seeded from the current
number, reusing `TextFieldClearButton` + `EditableTextFieldColors`. Confirm →
`HouseholderPhoneChanged(value)` then `PhoneInputDismissed`; cancel/dismiss →
`PhoneInputDismissed`.

### ② Tap with a saved number → options sheet

Otherwise `PhoneClicked` sets `showPhoneOptionsSheet = true`, rendering a
`ModalBottomSheet` (pinned to `rememberModalBottomSheetState(skipPartiallyExpanded = true)`
so a short sheet fully expands) listing the number plus four rows:

| Row | Action |
|-----|--------|
| Call | `context.launchDialer(phone)` |
| Message (SMS) | `context.launchSms(phone)` |
| WhatsApp | `context.launchWhatsApp(phone)` |
| Edit number | `onEvent(EditPhoneClicked)` → close sheet, open input dialog |

Every row dismisses the sheet (`PhoneOptionsDismissed`). The launch helpers run in
the composable (they need an Android `Context` via `LocalContext.current`), not the
ViewModel.

### Intent helpers — `extension/ContextExtension.kt`

Three helpers next to the existing `launchGoogleMaps` / `showShareIntent`, each
wrapped in `try/catch (ActivityNotFoundException)` so a missing handler never
crashes:

- `launchDialer(phone)` → `ACTION_DIAL`, `tel:<phone>`
- `launchSms(phone)` → `ACTION_SENDTO`, `smsto:<phone>`
- `launchWhatsApp(phone)` → `ACTION_VIEW`, `https://wa.me/<digits>` (digits only)

## Edges (confirmed handled)

- **No app can handle the action** — every launch is guarded by
  `try/catch (ActivityNotFoundException)`, so a device with no dialer/SMS app is a
  no-op rather than a crash.
- **WhatsApp not installed** — the `https://wa.me/...` link is a `BROWSABLE` web
  intent, so it falls back to WhatsApp Web in a browser. No install check and no
  Android 11 `<queries>` entry are needed (web intents are exempt from package
  visibility filtering; `ACTION_DIAL`/`ACTION_SENDTO` are likewise exempt).
- **Number formatting** — users type `+55 11 99999-0000`; `launchWhatsApp`
  sanitizes to bare digits because `wa.me` requires an international number with no
  `+`, spaces, or punctuation. Dialer/SMS keep the raw string (they tolerate it).
- **Blank vs. null** — an empty dialog entry is stored as `null` (not `""`) via
  `asModel`, so `PhoneClicked` treats "cleared the number" the same as "never set."
- **Snapshot table** — because `HouseholderSnapshot` `@Embedded`s `Householder`,
  Room expects the new column there too; the migration adds it to both tables
  (same reason `visit_snapshot`/`householder_snapshot` mirror other columns).

## Migration & touched files

**DB version 12 → 13**, new `MIGRATION_12_13`:

- `ALTER TABLE householder ADD COLUMN phoneNumber TEXT`
- `ALTER TABLE householder_snapshot ADD COLUMN phoneNumber TEXT`
- Register `MIGRATION_12_13` in `VisitasDatabase.MIGRATIONS`; bump `version = 13`;
  the Room compiler regenerates `app/schemas/.../13.json`.

| File | Change |
|------|--------|
| `householder/Householder.kt` | add nullable `phoneNumber` |
| `migration/Migration_12_13.kt` | **new** — ALTER TABLE ×2 |
| `VisitasDatabase.kt` | bump to v13, register migration |
| `visit/VisitDetailViewModel.kt` | `phoneNumber` in editable state/mapping; `PhoneClicked` / `HouseholderPhoneChanged` / `EditPhoneClicked` / dialog+sheet dismiss events; `showPhoneInputDialog` / `showPhoneOptionsSheet` flags |
| `visit/VisitDetailScreen.kt` | top-bar `Call` action; `PhoneInputDialog`; `PhoneOptionsSheet` |
| `extension/ContextExtension.kt` | `launchDialer` / `launchSms` / `launchWhatsApp` |
| `res/values*/strings.xml` | `contact_householder`, `householder_phone`, `phone_action_*` (EN, pt-BR, es-419) |
| `visit/VisitDetailPreviewConfigProvider.kt` | "Phone Number Input" + "Phone Options" screenshot configs |
| `androidTest/.../HouseholderDaoTest.kt`, `test/.../VisitDetailViewModelTest.kt` | round-trip + tap-path + persistence tests |

No changes to `HouseholderDao` / `HouseholderRepository` / DI — the DAO uses
`@Upsert` + `SELECT *`, so a new column round-trips without query changes.

## Out of scope

- An always-visible inline phone field on the detail screen — the number is
  reached through the top-bar button, dialog, and sheet only.
- `CALL_PHONE` permission and placing a call directly — we use `ACTION_DIAL`, which
  hands off to the dialer with no permission.
- Surfacing the phone number in the visit list `VisitHouseholder` `@DatabaseView`.
- Any multi-number / contact-picker support — a householder has a single number.
