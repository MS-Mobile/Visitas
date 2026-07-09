# Address Options Sheet Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the direct Google-Maps launch on each visit row with a bottom sheet offering "Google Maps" and "Uber" (deep-link ride request) options.

**Architecture:** A new `Context.launchUber` intent extension mirrors the existing `launchGoogleMaps`. The list ViewModel gains a nullable `addressOptionsSheet` field (holding the tapped row's address), driven by two new `UiEvent`s — following the existing screen-level `VisitMapSheet` pattern. The row's icon dispatches an event instead of launching Maps directly; a new `AddressOptionsSheet` composable (a copy of `PhoneOptionsSheet`) renders the two actions.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, JUnit + Mockito (kotlin), Android intents.

---

## File Structure

- **Modify** `app/src/main/java/com/msmobile/visitas/extension/ContextExtension.kt` — add `launchUber`.
- **Modify** `app/src/main/java/com/msmobile/visitas/visit/VisitListViewModel.kt` — `addressOptionsSheet` state field, two `UiEvent`s, dispatch entries, two handlers.
- **Modify** `app/src/main/java/com/msmobile/visitas/visit/VisitListScreen.kt` — row `onClick`, `AddressOptionsSheet` + `AddressOptionItem` composables, imports.
- **Modify** `app/src/main/res/values/strings.xml` (+ `values-b+es+419/strings.xml`, `values-pt-rBR/strings.xml`) — two new strings.
- **Modify** `app/src/test/java/com/msmobile/visitas/visit/VisitListViewModelTest.kt` — tests for the new events.

---

## Task 1: Uber deep-link extension

Follows the untested `launchGoogleMaps` precedent (no unit test — the extension is a thin intent builder, consistent with the rest of `ContextExtension.kt`).

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/extension/ContextExtension.kt`

- [ ] **Step 1: Add `launchUber` after `launchGoogleMaps`**

Insert this function immediately after the closing brace of `launchGoogleMaps` (after line 24):

```kotlin
/**
 * Requests an Uber ride to ([latitude], [longitude]) with [label] as the dropoff nickname.
 * Pickup defaults to the rider's current location (resolved by Uber). Opens the Uber app via
 * a `uber://` deep link when installed; otherwise falls back to the `m.uber.com` universal
 * link in a browser.
 */
fun Context.launchUber(label: String, latitude: Double, longitude: Double) {
    val encodedLabel = Uri.encode(label)
    val deepLinkParams =
        "action=setPickup&pickup=my_location" +
            "&dropoff[latitude]=$latitude&dropoff[longitude]=$longitude&dropoff[nickname]=$encodedLabel"

    val uberUri = Uri.parse("uber://?$deepLinkParams")
    val uberIntent = Intent(Intent.ACTION_VIEW, uberUri)
    uberIntent.setPackage("com.ubercab")

    if (uberIntent.resolveActivity(packageManager) != null) {
        startActivity(uberIntent)
    } else {
        // Fallback to the Uber universal web link if the app isn't installed
        val fallbackUri = Uri.parse("https://m.uber.com/ul/?$deepLinkParams")
        val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri)
        startActivity(fallbackIntent)
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/extension/ContextExtension.kt
git commit -m "Add launchUber intent extension for ride requests"
```

---

## Task 2: ViewModel state, events, and handlers (TDD)

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitListViewModel.kt`
- Test: `app/src/test/java/com/msmobile/visitas/visit/VisitListViewModelTest.kt`

- [ ] **Step 1: Write the failing tests**

In `VisitListViewModelTest.kt`, add these imports near the other `junit.framework.TestCase` imports (after line 18):

```kotlin
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
```

Then add these three tests after the `VisitMapSheetDismissed` test (after line 219). `SECOND_VISIT_ID` (coordinates set → `Data`) and `FIRST_VISIT_ID` (null coordinates → `NoData`) are the fixtures already built by `createVisitHouseholderList()`:

```kotlin
@Test
fun `onEvent with AddressOptionsClicked shows sheet with the visit's address data`() {
    // Arrange
    val viewModel = createViewModel()
    viewModel.onEvent(VisitListViewModel.UiEvent.ViewCreated)
    val visitWithAddress = requireNotNull(
        viewModel.uiState.value.visitList.find { it.visitId == SECOND_VISIT_ID }
    )

    // Act
    viewModel.onEvent(VisitListViewModel.UiEvent.AddressOptionsClicked(visitWithAddress))

    // Assert
    val sheet = viewModel.uiState.value.addressOptionsSheet
    assertNotNull(sheet)
    assertEquals("Address 2", sheet?.address)
    assertEquals(40.7128, sheet?.latitude)
    assertEquals(-74.0060, sheet?.longitude)
}

@Test
fun `onEvent with AddressOptionsClicked ignores visit without address data`() {
    // Arrange
    val viewModel = createViewModel()
    viewModel.onEvent(VisitListViewModel.UiEvent.ViewCreated)
    val visitWithoutAddress = requireNotNull(
        viewModel.uiState.value.visitList.find { it.visitId == FIRST_VISIT_ID }
    )

    // Act
    viewModel.onEvent(VisitListViewModel.UiEvent.AddressOptionsClicked(visitWithoutAddress))

    // Assert
    assertNull(viewModel.uiState.value.addressOptionsSheet)
}

@Test
fun `onEvent with AddressOptionsDismissed hides the sheet`() {
    // Arrange
    val viewModel = createViewModel()
    viewModel.onEvent(VisitListViewModel.UiEvent.ViewCreated)
    val visitWithAddress = requireNotNull(
        viewModel.uiState.value.visitList.find { it.visitId == SECOND_VISIT_ID }
    )
    viewModel.onEvent(VisitListViewModel.UiEvent.AddressOptionsClicked(visitWithAddress))
    assertNotNull(viewModel.uiState.value.addressOptionsSheet)

    // Act
    viewModel.onEvent(VisitListViewModel.UiEvent.AddressOptionsDismissed)

    // Assert
    assertNull(viewModel.uiState.value.addressOptionsSheet)
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.visit.VisitListViewModelTest"`
Expected: FAIL — compilation error / unresolved reference `AddressOptionsClicked`, `AddressOptionsDismissed`, and `addressOptionsSheet`.

- [ ] **Step 3: Add the `addressOptionsSheet` field to `UiState`**

In `VisitListViewModel.kt`, in the `UiState` data class (around line 899), add the field as the new last parameter (with a default so the state initializer needs no change), after `visitMapEngine`:

```kotlin
        val visitMapEngine: VisitMapEngineOption = VisitMapEngineOption.MapLibre,
        val addressOptionsSheet: HouseholderAddressState.Data? = null
    )
```

- [ ] **Step 4: Add the two `UiEvent`s**

In the `sealed class UiEvent` block, add after `PendingVisitMenuClicked` (line 819):

```kotlin
        data class AddressOptionsClicked(val visit: VisitHouseholderState) : UiEvent()
        data object AddressOptionsDismissed : UiEvent()
```

- [ ] **Step 5: Add dispatch entries in `onEvent`**

In the `onEvent` `when` block, add after the `VisitMapSheetDismissed` line (line 142):

```kotlin
            is UiEvent.AddressOptionsClicked -> handleAddressOptionsClicked(uiEvent.visit)
            is UiEvent.AddressOptionsDismissed -> handleAddressOptionsDismissed()
```

- [ ] **Step 6: Add the two handlers**

Add these functions immediately after `handleVisitMapSheetDismissed()` (after line 445):

```kotlin
    private fun handleAddressOptionsClicked(visit: VisitHouseholderState) {
        val address = visit.householderAddressState as? HouseholderAddressState.Data ?: return
        newState {
            copy(addressOptionsSheet = address)
        }
    }

    private fun handleAddressOptionsDismissed() {
        newState {
            copy(addressOptionsSheet = null)
        }
    }
```

- [ ] **Step 7: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.visit.VisitListViewModelTest"`
Expected: PASS (all tests, including the three new ones).

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/visit/VisitListViewModel.kt app/src/test/java/com/msmobile/visitas/visit/VisitListViewModelTest.kt
git commit -m "Add address options sheet state to visit list ViewModel"
```

---

## Task 3: String resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-b+es+419/strings.xml`
- Modify: `app/src/main/res/values-pt-rBR/strings.xml`

The header reuses the existing `householder_address` string. Only two new brand-name strings are needed; as proper nouns they are identical across locales.

- [ ] **Step 1: Add strings to the default locale**

In `app/src/main/res/values/strings.xml`, add near the other `phone_action_*` strings (after line 9):

```xml
    <string name="address_action_google_maps">Google Maps</string>
    <string name="address_action_uber">Uber</string>
```

- [ ] **Step 2: Add the same strings to `values-b+es+419/strings.xml`**

Add after the `phone_action_call` string (after line 8):

```xml
    <string name="address_action_google_maps">Google Maps</string>
    <string name="address_action_uber">Uber</string>
```

- [ ] **Step 3: Add the same strings to `values-pt-rBR/strings.xml`**

Add after the `phone_action_call` string (after line 7):

```xml
    <string name="address_action_google_maps">Google Maps</string>
    <string name="address_action_uber">Uber</string>
```

- [ ] **Step 4: Verify resources compile**

Run: `./gradlew :app:processDebugResources`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-b+es+419/strings.xml app/src/main/res/values-pt-rBR/strings.xml
git commit -m "Add address action strings for Google Maps and Uber options"
```

---

## Task 4: Row wiring and the sheet composable

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitListScreen.kt`

- [ ] **Step 1: Add the required imports**

In `VisitListScreen.kt`, add these imports in the correct alphabetical positions among the existing imports:

```kotlin
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.rounded.LocalTaxi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import com.msmobile.visitas.extension.bottomSheetListItemColors
import com.msmobile.visitas.extension.launchUber
```

(`clickable`, `Column`, `Explore`, `ExperimentalMaterial3Api`, `HorizontalDivider`, `Icon`, `LocalContext`, `launchGoogleMaps`, `rememberModalBottomSheetState`, `stringResource`, `Text`, and `PreviewCompatModalSheet` are already imported.)

- [ ] **Step 2: Change the row icon to open the sheet**

Replace the `IconButton`'s `onClick` at `VisitListScreen.kt:880-886` — currently:

```kotlin
                            onClick = {
                                context.launchGoogleMaps(
                                    addressState.address,
                                    addressState.latitude,
                                    addressState.longitude
                                )
                            }) {
```

with:

```kotlin
                            onClick = {
                                onEvent(VisitListViewModel.UiEvent.AddressOptionsClicked(visit))
                            }) {
```

(`onEvent` and `visit` are already in scope in this row composable.)

- [ ] **Step 3: Render the sheet at screen level**

In the screen-level composable, immediately after the `VisitMapSheet(...)` call block (which ends at line 287), add:

```kotlin
        visitListUiState.addressOptionsSheet?.let { address ->
            AddressOptionsSheet(
                address = address,
                onEvent = onVisitListEvent
            )
        }
```

- [ ] **Step 4: Add the `AddressOptionsSheet` and `AddressOptionItem` composables**

Add these two private composables at the end of the file (after the last top-level composable, e.g. after the `VisitMapSheet` composable that ends the file region around line 966+). Place them at file top level (not nested):

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressOptionsSheet(
    address: VisitListViewModel.HouseholderAddressState.Data,
    onEvent: (VisitListViewModel.UiEvent) -> Unit
) {
    val context = LocalContext.current
    val onDismiss = { onEvent(VisitListViewModel.UiEvent.AddressOptionsDismissed) }
    PreviewCompatModalSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            ListItem(
                modifier = Modifier,
                leadingContent = null,
                trailingContent = null,
                overlineContent = { Text(text = stringResource(id = R.string.householder_address)) },
                supportingContent = null,
                colors = ListItemDefaults.bottomSheetListItemColors(),
                elevation = ListItemDefaults.elevation(),
                content = { Text(text = address.address) },
            )
            HorizontalDivider()
            AddressOptionItem(
                icon = Icons.Rounded.Explore,
                label = stringResource(id = R.string.address_action_google_maps)
            ) {
                context.launchGoogleMaps(address.address, address.latitude, address.longitude)
                onDismiss()
            }
            AddressOptionItem(
                icon = Icons.Rounded.LocalTaxi,
                label = stringResource(id = R.string.address_action_uber)
            ) {
                context.launchUber(address.address, address.latitude, address.longitude)
                onDismiss()
            }
        }
    }
}

@Composable
private fun AddressOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Icon(imageVector = icon, contentDescription = null) },
        trailingContent = null,
        overlineContent = null,
        supportingContent = null,
        colors = ListItemDefaults.bottomSheetListItemColors(),
        elevation = ListItemDefaults.elevation(),
        content = { Text(text = label) },
    )
}
```

- [ ] **Step 5: Verify the app compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/msmobile/visitas/visit/VisitListScreen.kt
git commit -m "Show address options sheet with Google Maps and Uber actions"
```

---

## Task 5: Full verification

- [ ] **Step 1: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 2: Assemble a debug build**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Manual smoke check (device/emulator)**

Use the `verify` or `run` skill to launch the app, open the visit list, tap a row's map icon, and confirm:
- The sheet shows the address header + "Google Maps" and "Uber" rows.
- "Google Maps" opens Maps to the householder location (existing behavior).
- "Uber" opens the Uber app (or `m.uber.com` if not installed) with the dropoff pre-filled.

---

## Notes

- The `AddressOptionItem` composable is an intentional copy of `PhoneOptionItem` (in `VisitDetailScreen.kt`) rather than a shared helper — the two screens keep their own private option-item composables, matching the existing per-screen convention. If a future task consolidates them, that is out of scope here.
- No instrumentation/intent tests are added for `launchUber` or the composables, matching the existing untested precedent for `launchGoogleMaps` and `PhoneOptionsSheet`.
