# Address Options Sheet (Google Maps + Uber) — Design

**Date:** 2026-07-09
**Status:** Approved (pending spec review)

## Summary

Today, the address button on each visit row in `VisitListScreen` launches Google Maps
directly. This change replaces that single action with a bottom sheet offering two options:

- **Google Maps** — current behavior (directions to the householder's address).
- **Uber** — deep-links into the Uber app with the householder's location pre-filled as
  the dropoff, requesting a ride.

The sheet mirrors the existing `PhoneOptionsSheet` in `VisitDetailScreen`, and the sheet
state follows the existing screen-level sheet pattern (`VisitMapSheet`) in the list
ViewModel.

## Current behavior (baseline)

`VisitListScreen.kt:878-892` — the row's `IconButton` (a `Icons.Rounded.Explore` icon,
visible only when `householderAddressState is HouseholderAddressState.Data`) calls:

```kotlin
context.launchGoogleMaps(addressState.address, addressState.latitude, addressState.longitude)
```

`ContextExtension.kt:9` — `launchGoogleMaps` builds a `geo:` URI targeting
`com.google.android.apps.maps`, and falls back to a `https://www.google.com/maps/...`
browser URL via `resolveActivity` when Maps isn't installed.

## Design

### 1. Uber deep-link extension

Add `Context.launchUber(label, latitude, longitude)` to `ContextExtension.kt`, mirroring
`launchGoogleMaps`:

- Primary intent — Uber app deep link:
  `uber://?action=setPickup&pickup=my_location&dropoff[latitude]=$latitude&dropoff[longitude]=$longitude&dropoff[nickname]=$encodedLabel`,
  package `com.ubercab`. `label` is URI-encoded with `Uri.encode`, same as `launchGoogleMaps`.
- Fallback — if `resolveActivity(packageManager)` is null, open the universal web link in a
  browser: `https://m.uber.com/ul/?action=setPickup&pickup=my_location&dropoff[latitude]=$latitude&dropoff[longitude]=$longitude&dropoff[nickname]=$encodedLabel`.

Same structure and error-handling shape as `launchGoogleMaps`, so it is a drop-in sibling.
Pickup defaults to `my_location` (the rider's current location, resolved by Uber).

### 2. Screen-level sheet state (VisitListViewModel)

Follow the existing screen-level single-sheet pattern (`VisitMapSheet`) rather than a
per-row flag — the sheet renders once at the screen level and holds the tapped row's
address. A per-row boolean was rejected: it would render one sheet per row and is
inconsistent with how other modal sheets work here.

**`UiState`** — add one nullable field (null = hidden; no extra boolean needed):

```kotlin
val addressOptionsSheet: HouseholderAddressState.Data? = null
```

Add `addressOptionsSheet = null` to the preview/default `UiState` initializer alongside the
other sheet defaults (e.g. next to `showVisitMapSheet = false`).

**`UiEvent`** — two new events:

```kotlin
data class AddressOptionsClicked(val visit: VisitHouseholderState) : UiEvent()
data object AddressOptionsDismissed : UiEvent()
```

**Handlers** (dispatched from `onEvent`):

- `handleAddressOptionsClicked(visit)` — if `visit.householderAddressState` is
  `HouseholderAddressState.Data`, set `addressOptionsSheet` to that `Data`; otherwise no-op.
- `handleAddressOptionsDismissed()` — set `addressOptionsSheet = null`.

### 3. Row wiring (VisitListScreen)

The row `IconButton` (`:878-892`) keeps the same `Icons.Rounded.Explore` icon and
`AnimatedVisibility` gating. Its `onClick` changes from calling `context.launchGoogleMaps(...)`
directly to:

```kotlin
onClick = { onEvent(VisitListViewModel.UiEvent.AddressOptionsClicked(visit)) }
```

`onEvent` and `visit` are already in scope at that row.

### 4. The sheet composable

Add `AddressOptionsSheet(address, onEvent)` at screen level (beside `VisitMapSheet`, around
`VisitListScreen.kt:278`), driven by `uiState.addressOptionsSheet`:

```kotlin
uiState.addressOptionsSheet?.let { address ->
    AddressOptionsSheet(address = address, onEvent = onVisitListEvent)
}
```

Built the same way as `PhoneOptionsSheet` (`VisitDetailScreen.kt:299`):

- `PreviewCompatModalSheet` with `rememberModalBottomSheetState(skipPartiallyExpanded = true)`,
  `onDismissRequest` → `onEvent(AddressOptionsDismissed)`.
- `Column` with `navigationBarsPadding()`.
- Header `ListItem` (bottom-sheet colors): `overlineContent` = `stringResource(R.string.householder_address)`,
  `content` = the address text.
- `HorizontalDivider`.
- Two rows via a local `AddressOptionItem(icon, label, onClick)` helper (a copy of
  `PhoneOptionItem`):
  - **Google Maps** — `Icons.Rounded.Explore`, label `R.string.address_action_google_maps`
    → `context.launchGoogleMaps(address.address, address.latitude, address.longitude)` then dismiss.
  - **Uber** — `Icons.Rounded.LocalTaxi`, label `R.string.address_action_uber`
    → `context.launchUber(address.address, address.latitude, address.longitude)` then dismiss.

`context` comes from `LocalContext.current` inside the composable, same as `PhoneOptionsSheet`.
Dismiss is `onEvent(AddressOptionsDismissed)`.

### 5. Strings

Reuse the existing `householder_address` string for the header. Add two new brand-name
strings to all three locale files (`values/`, `values-b+es+419/`, `values-pt-rBR/`). As
proper nouns they are identical across locales:

- `address_action_google_maps` → `Google Maps`
- `address_action_uber` → `Uber`

### 6. Testing

- `VisitListViewModel` unit test: `AddressOptionsClicked` on a row whose
  `householderAddressState` is `Data` sets `addressOptionsSheet` to that `Data`; on a `NoData`
  row it stays null; `AddressOptionsDismissed` clears it back to null.
- The `Context` extensions and composables follow existing untested UI-helper precedent
  (`launchGoogleMaps` has no test), so no intent/instrumentation tests are added.

## Files touched

- `app/src/main/java/com/msmobile/visitas/extension/ContextExtension.kt` — add `launchUber`.
- `app/src/main/java/com/msmobile/visitas/visit/VisitListViewModel.kt` — state field, events, handlers.
- `app/src/main/java/com/msmobile/visitas/visit/VisitListScreen.kt` — row onClick, `AddressOptionsSheet`, `AddressOptionItem`.
- `app/src/main/res/values/strings.xml` (+ `values-b+es+419/`, `values-pt-rBR/`) — two new strings.
- ViewModel test file for `VisitListViewModel`.

## Out of scope (YAGNI)

- Additional address actions (Waze, copy address, share). Only Google Maps + Uber.
- Uber SDK / account integration or ride-status tracking — deep link only.
- Changing the detail-screen phone sheet.
