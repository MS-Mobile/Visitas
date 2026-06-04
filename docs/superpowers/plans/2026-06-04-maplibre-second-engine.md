# MapLibre GL Second Map Engine — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add MapLibre GL as the default map engine, with Leaflet kept as a user-selectable fallback via a Settings dropdown, both engines honouring an identical Kotlin↔JS bridge contract.

**Architecture:** A new `VisitMapEngineOption` enum is persisted in the existing `Preference` Room table (migration 7→8). `SettingsDetailViewModel` loads and saves the preference. `VisitListViewModel.UiState` exposes the active engine; `VisitsMap.kt` selects the HTML asset path based on it. Both HTML files live under `assets/map/leaflet/` and `assets/map/maplibre/` and implement the same `initializeMap` / `setMarkers` / `window.Visits.*` contract.

**Tech Stack:** Kotlin, Room (migration), Hilt, Jetpack Compose (ExposedDropdownMenuBox), Android WebView, MapLibre GL JS v5.24.0, Leaflet 2.0.0-alpha.1, JUnit4, Mockito-Kotlin.

---

### Task 1: VisitMapEngineOption enum

**Files:**
- Create: `app/src/main/java/com/msmobile/visitas/visit/VisitMapEngineOption.kt`

- [ ] **Step 1: Create the enum**

```kotlin
package com.msmobile.visitas.visit

enum class VisitMapEngineOption { MapLibre, Leaflet }
```

- [ ] **Step 2: Commit**

```
git add app/src/main/java/com/msmobile/visitas/visit/VisitMapEngineOption.kt
git commit -m "Add VisitMapEngineOption enum"
```

---

### Task 2: Data layer — Preference field, TypeConverters, Migration, DB version

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/preference/Preference.kt`
- Modify: `app/src/main/java/com/msmobile/visitas/preference/PreferenceTypeConverters.kt`
- Create: `app/src/main/java/com/msmobile/visitas/migration/Migration_7_8.kt`
- Modify: `app/src/main/java/com/msmobile/visitas/VisitasDatabase.kt`
- Create: `app/src/test/java/com/msmobile/visitas/preference/PreferenceTypeConvertersTest.kt`

- [ ] **Step 1: Write failing tests for the new TypeConverters**

Create `app/src/test/java/com/msmobile/visitas/preference/PreferenceTypeConvertersTest.kt`:

```kotlin
package com.msmobile.visitas.preference

import com.msmobile.visitas.visit.VisitMapEngineOption
import junit.framework.TestCase.assertEquals
import org.junit.Test

class PreferenceTypeConvertersTest {
    private val converters = PreferenceTypeConverters()

    @Test
    fun `fromMapEngineOption returns enum name`() {
        assertEquals("MapLibre", converters.fromMapEngineOption(VisitMapEngineOption.MapLibre))
        assertEquals("Leaflet", converters.fromMapEngineOption(VisitMapEngineOption.Leaflet))
    }

    @Test
    fun `toMapEngineOption parses known values`() {
        assertEquals(VisitMapEngineOption.MapLibre, converters.toMapEngineOption("MapLibre"))
        assertEquals(VisitMapEngineOption.Leaflet, converters.toMapEngineOption("Leaflet"))
    }

    @Test
    fun `toMapEngineOption falls back to MapLibre for unknown value`() {
        assertEquals(VisitMapEngineOption.MapLibre, converters.toMapEngineOption("unknown"))
    }
}
```

- [ ] **Step 2: Run tests — expect compile failure (converters don't exist yet)**

```
./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.preference.PreferenceTypeConvertersTest"
```

Expected: compilation error — `fromMapEngineOption` / `toMapEngineOption` not found.

- [ ] **Step 3: Add field to Preference entity**

`app/src/main/java/com/msmobile/visitas/preference/Preference.kt`:

```kotlin
package com.msmobile.visitas.preference

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.msmobile.visitas.visit.VisitListDateFilterOption
import com.msmobile.visitas.visit.VisitListDistanceFilterOption
import com.msmobile.visitas.visit.VisitMapEngineOption
import java.util.UUID

@Entity(tableName = "preference")
data class Preference(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val visitListDateFilterOption: VisitListDateFilterOption,
    val visitListDistanceFilterOption: VisitListDistanceFilterOption,
    val visitMapEngineOption: VisitMapEngineOption = VisitMapEngineOption.MapLibre
)
```

- [ ] **Step 4: Add TypeConverters**

`app/src/main/java/com/msmobile/visitas/preference/PreferenceTypeConverters.kt` — append at the end, inside the class:

```kotlin
    @TypeConverter
    fun fromMapEngineOption(value: VisitMapEngineOption): String = value.name

    @TypeConverter
    fun toMapEngineOption(value: String): VisitMapEngineOption =
        runCatching { VisitMapEngineOption.valueOf(value) }.getOrDefault(VisitMapEngineOption.MapLibre)
```

Add the missing import at the top of the file:

```kotlin
import com.msmobile.visitas.visit.VisitMapEngineOption
```

- [ ] **Step 5: Run tests — expect pass**

```
./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.preference.PreferenceTypeConvertersTest"
```

Expected: all 3 tests PASS.

- [ ] **Step 6: Create MIGRATION_7_8**

Create `app/src/main/java/com/msmobile/visitas/migration/Migration_7_8.kt`:

```kotlin
package com.msmobile.visitas.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE preference ADD COLUMN visitMapEngineOption TEXT NOT NULL DEFAULT 'MapLibre'"
        )
    }
}
```

- [ ] **Step 7: Bump database version and register migration**

`app/src/main/java/com/msmobile/visitas/VisitasDatabase.kt`:

Change `version = 7` → `version = 8` and add the import + migration:

```kotlin
import com.msmobile.visitas.migration.MIGRATION_7_8
```

```kotlin
private val MIGRATIONS = arrayOf(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6,
    MIGRATION_6_7,
    MIGRATION_7_8
)
```

- [ ] **Step 8: Compile check**

```
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 9: Commit**

```
git add app/src/main/java/com/msmobile/visitas/preference/ \
        app/src/main/java/com/msmobile/visitas/migration/Migration_7_8.kt \
        app/src/main/java/com/msmobile/visitas/VisitasDatabase.kt \
        app/src/test/java/com/msmobile/visitas/preference/
git commit -m "Add visitMapEngineOption to Preference with Room migration 7→8"
```

---

### Task 3: SettingsDetailViewModel — load and save map engine preference

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/settings/SettingsDetailViewModel.kt`
- Create: `app/src/test/java/com/msmobile/visitas/settings/SettingsDetailViewModelTest.kt`

- [ ] **Step 1: Write failing tests**

Create `app/src/test/java/com/msmobile/visitas/settings/SettingsDetailViewModelTest.kt`:

```kotlin
package com.msmobile.visitas.settings

import com.msmobile.visitas.preference.Preference
import com.msmobile.visitas.preference.PreferenceRepository
import com.msmobile.visitas.util.BackupHandler
import com.msmobile.visitas.util.DispatcherProvider
import com.msmobile.visitas.util.MainDispatcherRule
import com.msmobile.visitas.util.MockReferenceHolder
import com.msmobile.visitas.visit.VisitListDateFilterOption
import com.msmobile.visitas.visit.VisitListDistanceFilterOption
import com.msmobile.visitas.visit.VisitMapEngineOption
import junit.framework.TestCase.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyBlocking

class SettingsDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `onEvent with ViewCreated loads map engine from saved preference`() {
        val viewModel = createViewModel(savedMapEngine = VisitMapEngineOption.Leaflet)

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.ViewCreated)

        assertEquals(VisitMapEngineOption.Leaflet, viewModel.uiState.value.selectedMapEngine)
    }

    @Test
    fun `onEvent with MapEngineSelected updates state to selected engine`() {
        val viewModel = createViewModel()

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.MapEngineSelected(VisitMapEngineOption.Leaflet))

        assertEquals(VisitMapEngineOption.Leaflet, viewModel.uiState.value.selectedMapEngine)
    }

    @Test
    fun `onEvent with MapEngineSelected saves preference to repository`() {
        val preferenceRepositoryRef = MockReferenceHolder<PreferenceRepository>()
        val viewModel = createViewModel(preferenceRepositoryRef = preferenceRepositoryRef)

        viewModel.onEvent(SettingsDetailViewModel.UiEvent.MapEngineSelected(VisitMapEngineOption.Leaflet))

        verifyBlocking(requireNotNull(preferenceRepositoryRef.value)) { save(any()) }
    }

    private fun createViewModel(
        savedMapEngine: VisitMapEngineOption = VisitMapEngineOption.MapLibre,
        preferenceRepositoryRef: MockReferenceHolder<PreferenceRepository>? = null
    ): SettingsDetailViewModel {
        val dispatchers = DispatcherProvider(io = mainDispatcherRule.dispatcher)
        val backupHandler = mock<BackupHandler>()
        val preferenceRepository = mock<PreferenceRepository> {
            on { get() } doReturn Preference(
                visitListDateFilterOption = VisitListDateFilterOption.All,
                visitListDistanceFilterOption = VisitListDistanceFilterOption.All,
                visitMapEngineOption = savedMapEngine
            )
        }
        preferenceRepositoryRef?.value = preferenceRepository
        return SettingsDetailViewModel(
            preferenceRepository = preferenceRepository,
            backupHandler = backupHandler,
            dispatchers = dispatchers
        )
    }
}
```

- [ ] **Step 2: Run tests — expect compile failure**

```
./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.settings.SettingsDetailViewModelTest"
```

Expected: compilation error — `ViewCreated`, `MapEngineSelected`, `selectedMapEngine` not found.

- [ ] **Step 3: Update SettingsDetailViewModel**

`app/src/main/java/com/msmobile/visitas/settings/SettingsDetailViewModel.kt` — full replacement:

```kotlin
package com.msmobile.visitas.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msmobile.visitas.preference.PreferenceRepository
import com.msmobile.visitas.util.BackupHandler
import com.msmobile.visitas.util.DispatcherProvider
import com.msmobile.visitas.visit.VisitMapEngineOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsDetailViewModel @Inject constructor(
    private val preferenceRepository: PreferenceRepository,
    private val backupHandler: BackupHandler,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    fun onEvent(event: UiEvent) {
        when (event) {
            is UiEvent.ViewCreated -> viewCreated()
            is UiEvent.MapEngineSelected -> mapEngineSelected(event.engine)
            is UiEvent.CreateBackup -> handleCreateBackup(event.successMessage, event.errorMessage)
            is UiEvent.RestoreBackup -> handleRestoreBackup(event.fileUri, event.successMessage, event.errorMessage)
            is UiEvent.CreateBackupFailed -> handleCreateBackupError(event.message)
            is UiEvent.RestoreBackupFailed -> handleRestoreBackupError(event.message)
            is UiEvent.BackupCanceled -> handleBackupCanceled()
            is UiEvent.BackupResultAcknowledged -> handleBackupResultAcknowledged()
        }
    }

    private fun viewCreated() {
        viewModelScope.launch(dispatchers.io) {
            val preference = preferenceRepository.get()
            _uiState.update { it.copy(selectedMapEngine = preference.visitMapEngineOption) }
        }
    }

    private fun mapEngineSelected(engine: VisitMapEngineOption) {
        viewModelScope.launch(dispatchers.io) {
            val preference = preferenceRepository.get().copy(visitMapEngineOption = engine)
            preferenceRepository.save(preference)
        }
        _uiState.update { it.copy(selectedMapEngine = engine) }
    }

    private fun handleCreateBackup(successMessage: String, errorMessage: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch(dispatchers.io) {
            backupHandler.createBackupFile()
                .onSuccess { uri ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            backupResult = BackupResult.BackupCreationSuccess(
                                message = successMessage,
                                shareFileUri = uri
                            )
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            backupResult = BackupResult.RestoreFailure(errorMessage)
                        )
                    }
                }
        }
    }

    private fun handleRestoreBackup(fileUri: Uri, successMessage: String, errorMessage: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch(dispatchers.io) {
            backupHandler.restoreBackup(fileUri)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            backupResult = BackupResult.RestoreSuccess(successMessage)
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            backupResult = BackupResult.RestoreFailure(errorMessage)
                        )
                    }
                }
        }
    }

    private fun handleCreateBackupError(message: String) {
        _uiState.update { it.copy(isLoading = false, backupResult = BackupResult.RestoreFailure(message)) }
    }

    private fun handleRestoreBackupError(message: String) {
        _uiState.update { it.copy(isLoading = false, backupResult = BackupResult.RestoreFailure(message)) }
    }

    private fun handleBackupResultAcknowledged() {
        _uiState.update { it.copy(backupResult = null) }
    }

    private fun handleBackupCanceled() {
        _uiState.update { it.copy(backupResult = null) }
    }

    sealed class UiEvent {
        data object ViewCreated : UiEvent()
        data class MapEngineSelected(val engine: VisitMapEngineOption) : UiEvent()
        data class CreateBackup(val successMessage: String, val errorMessage: String) : UiEvent()
        data class RestoreBackup(val fileUri: Uri, val successMessage: String, val errorMessage: String) : UiEvent()
        data class CreateBackupFailed(val message: String) : UiEvent()
        data class RestoreBackupFailed(val message: String) : UiEvent()
        data object BackupCanceled : UiEvent()
        data object BackupResultAcknowledged : UiEvent()
    }

    sealed class UiEventState {
        data object Idle : UiEventState()
    }

    sealed class BackupResult {
        data class BackupCreationSuccess(val message: String, val shareFileUri: Uri) : BackupResult()
        data class RestoreSuccess(val message: String) : BackupResult()
        data class RestoreFailure(val message: String) : BackupResult()
    }

    data class UiState(
        val isLoading: Boolean = false,
        val backupResult: BackupResult? = null,
        val eventState: UiEventState = UiEventState.Idle,
        val selectedMapEngine: VisitMapEngineOption = VisitMapEngineOption.MapLibre
    )
}
```

- [ ] **Step 4: Run tests — expect pass**

```
./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.settings.SettingsDetailViewModelTest"
```

Expected: all 3 tests PASS.

- [ ] **Step 5: Commit**

```
git add app/src/main/java/com/msmobile/visitas/settings/SettingsDetailViewModel.kt \
        app/src/test/java/com/msmobile/visitas/settings/
git commit -m "Extend SettingsDetailViewModel with map engine preference"
```

---

### Task 4: SettingsScreen — map engine dropdown

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/settings/SettingsScreen.kt`

- [ ] **Step 1: Add the dropdown to SettingsScreenContent**

In `SettingsScreen.kt`, add a `LaunchedEffect` for `ViewCreated` and an `ExposedDropdownMenuBox` for engine selection. The dropdown sits between the existing backup buttons and the bottom version text.

Replace the `SettingsScreen` composable and `SettingsScreenContent` with:

```kotlin
@Destination<RootGraph>(style = DetailScreenStyle::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsDetailViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onEvent = viewModel::onEvent

    LaunchedEffect(Unit) {
        onEvent(SettingsDetailViewModel.UiEvent.ViewCreated)
    }

    SettingsScreenContent(
        uiState = uiState,
        onEvent = onEvent
    )
}
```

Add the following imports at the top of `SettingsScreen.kt`:

```kotlin
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.msmobile.visitas.visit.VisitMapEngineOption
```

In `SettingsScreenContent`, add the dropdown after the second `OutlinedButton` (restore backup) and before `if (uiState.isLoading)`:

```kotlin
Spacer(modifier = Modifier.height(16.dp))

MapEngineDropdown(
    selectedEngine = uiState.selectedMapEngine,
    onEngineSelected = { engine ->
        onEvent(SettingsDetailViewModel.UiEvent.MapEngineSelected(engine))
    }
)
```

Add the private composable at the bottom of the file:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapEngineDropdown(
    selectedEngine: VisitMapEngineOption,
    onEngineSelected: (VisitMapEngineOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedEngine.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "Map Engine") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            VisitMapEngineOption.entries.forEach { engine ->
                DropdownMenuItem(
                    text = { Text(text = engine.name) },
                    onClick = {
                        onEngineSelected(engine)
                        expanded = false
                    }
                )
            }
        }
    }
}
```

- [ ] **Step 2: Compile check**

```
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```
git add app/src/main/java/com/msmobile/visitas/settings/SettingsScreen.kt
git commit -m "Add map engine dropdown to SettingsScreen"
```

---

### Task 5: VisitListViewModel — expose visitMapEngine in UiState

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitListViewModel.kt`
- Modify: `app/src/test/java/com/msmobile/visitas/visit/VisitListViewModelTest.kt`

- [ ] **Step 1: Write a failing test**

In `VisitListViewModelTest.kt`, add this test inside the class:

```kotlin
@Test
fun `onEvent with ViewCreated loads visitMapEngine from preference`() {
    val viewModel = createViewModel(savedMapEngine = VisitMapEngineOption.Leaflet)

    viewModel.onEvent(VisitListViewModel.UiEvent.ViewCreated)

    assertEquals(VisitMapEngineOption.Leaflet, viewModel.uiState.value.visitMapEngine)
}
```

Add this import at the top:

```kotlin
import com.msmobile.visitas.visit.VisitMapEngineOption
```

Add `savedMapEngine: VisitMapEngineOption = VisitMapEngineOption.MapLibre` to `createViewModel()`'s signature:

```kotlin
private fun createViewModel(
    visitHouseholderRepositoryRef: MockReferenceHolder<VisitHouseholderRepository>? = null,
    uriRef: MockReferenceHolder<Uri>? = null,
    hasLocationPermission: Boolean = false,
    locationFlowRef: MockReferenceHolder<MutableStateFlow<UserLocationProvider.UserLocation>>? = null,
    distanceResults: Map<DistanceInput, AddressProvider.AddressDistance> = emptyMap(),
    visitListDateFilterOption: VisitListDateFilterOption = VisitListDateFilterOption.All,
    savedMapEngine: VisitMapEngineOption = VisitMapEngineOption.MapLibre
): VisitListViewModel {
```

Update the `preferenceRepository` mock inside `createViewModel()` to use `savedMapEngine`:

```kotlin
val preferenceRepository = mock<PreferenceRepository> {
    on { get() } doReturn Preference(
        visitListDateFilterOption = visitListDateFilterOption,
        visitListDistanceFilterOption = VisitListDistanceFilterOption.All,
        visitMapEngineOption = savedMapEngine
    )
}
```

- [ ] **Step 2: Run test — expect compile failure**

```
./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.visit.VisitListViewModelTest.onEvent with ViewCreated loads visitMapEngine from preference"
```

Expected: compilation error — `visitMapEngine` not in `UiState`.

- [ ] **Step 3: Add visitMapEngine to UiState and load it in refreshVisits**

In `VisitListViewModel.kt`:

Add `visitMapEngine: VisitMapEngineOption = VisitMapEngineOption.MapLibre` to `UiState`:

```kotlin
data class UiState(
    val visitList: List<VisitHouseholderState>,
    val filter: VisitFilter,
    val selectedDate: LocalDateTime,
    val isVisitsFilterMenuExpanded: Boolean,
    val selectedTabIndex: Int,
    val visitsFilterOptions: List<VisitListDateFilterOption>,
    val selectedVisitFilterOption: VisitListDateFilterOption,
    val showLocationRationale: Boolean,
    val showLocationPermissionDialog: Boolean,
    val isLoadingVisits: Boolean,
    val showNearbyVisits: Boolean,
    val showBackupSheet: Boolean,
    val showVisitMapSheet: Boolean,
    val currentCoordinates: Pair<Double, Double>,
    val visitMapState: VisitMapState,
    val previewBackupFileState: PreviewBackupFileState,
    val visitMapEngine: VisitMapEngineOption = VisitMapEngineOption.MapLibre
)
```

Add the import at the top of `VisitListViewModel.kt`:

```kotlin
import com.msmobile.visitas.visit.VisitMapEngineOption
```

In `refreshVisits()`, add one line after reading the other preference fields:

```kotlin
private fun refreshVisits() {
    viewModelScope.launch(dispatchers.io) {
        val preference = preferenceRepository.get()
        val selectedVisitFilterOption = preference.visitListDateFilterOption
        val selectedVisitDistanceFilterOption = preference.visitListDistanceFilterOption
        val visitMapEngine = preference.visitMapEngineOption   // add this line
        val visitList = visitHouseholderRepository.getAll().map { visitHouseholder ->
            visitHouseholder.asState
        }.filterBy(_uiState.value.filter)
        val visitsFilterOptions = VisitListDateFilterOption.entries
        val showNearbyVisits =
            selectedVisitDistanceFilterOption == VisitListDistanceFilterOption.Nearby
        val userLocation = userLocationProvider.location.value
        newState {
            copy(
                visitList = visitList,
                visitsFilterOptions = visitsFilterOptions,
                selectedVisitFilterOption = selectedVisitFilterOption,
                isLoadingVisits = false,
                showNearbyVisits = showNearbyVisits,
                visitMapEngine = visitMapEngine               // add this line
            ).applyFilters()
                .calculateDistanceBetweenUserAndHouseholders(userLocation)
                .applyFilters()
        }
    }
}
```

- [ ] **Step 4: Run tests — expect all pass**

```
./gradlew :app:testDebugUnitTest --tests "com.msmobile.visitas.visit.VisitListViewModelTest"
```

Expected: all tests PASS.

- [ ] **Step 5: Commit**

```
git add app/src/main/java/com/msmobile/visitas/visit/VisitListViewModel.kt \
        app/src/test/java/com/msmobile/visitas/visit/VisitListViewModelTest.kt
git commit -m "Expose visitMapEngine in VisitListViewModel.UiState"
```

---

### Task 6: Thread engine through VisitListScreen and VisitsMap.kt

**Files:**
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitListScreen.kt`
- Modify: `app/src/main/java/com/msmobile/visitas/visit/VisitsMap.kt`

- [ ] **Step 1: Update VisitsMap.kt — add engine parameter and assetPath function**

Replace `VisitsMap.kt` entirely:

```kotlin
package com.msmobile.visitas.visit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.msmobile.visitas.R
import com.msmobile.visitas.ui.views.WebView
import com.msmobile.visitas.ui.views.WebViewViewBridge

@Composable
fun VisitsMap(
    currentLocation: Pair<Double, Double>,
    visitMapState: VisitMapState.Visits,
    engine: VisitMapEngineOption,
    onMapEvent: (VisitsMapEvent) -> Unit
) {
    val currentLocationText = stringResource(R.string.current_location).replace("'", "\\'")
    val webViewBridgeState = remember { mutableStateOf<WebViewViewBridge?>(null) }

    val (currentLatitude, currentLongitude) = currentLocation

    LaunchedEffect(visitMapState.serialized, currentLatitude, currentLongitude) {
        webViewBridgeState.value?.let { bridge ->
            val visitsJson = visitMapState.serialized
            bridge.executeScript("setMarkers($currentLatitude, $currentLongitude, $visitsJson);") { }
        }
    }

    WebView(
        url = assetPath(engine),
        javascriptInterface = VisitsMapJavascriptInterface(onMapEvent),
        isJavaScriptEnabled = true,
        isZoomEnabled = true,
        isDomStorageEnabled = true,
        isFileAccessAllowed = true,
        onInitializationComplete = { webViewBridge ->
            webViewBridgeState.value = webViewBridge
            val initScript = "initializeMap('${currentLocationText}');"
            webViewBridge.executeScript(initScript) { _ ->
                val visitsJson = visitMapState.serialized
                webViewBridge.executeScript("setMarkers($currentLatitude, $currentLongitude, $visitsJson);") { }
            }
        }
    )
}

sealed class VisitsMapEvent {
    data class ErrorLoadingMap(val errorMessage: String) : VisitsMapEvent()
}

private fun assetPath(engine: VisitMapEngineOption) = when (engine) {
    VisitMapEngineOption.MapLibre -> "file:///android_asset/map/maplibre/visits-map.html"
    VisitMapEngineOption.Leaflet  -> "file:///android_asset/map/leaflet/visits-map.html"
}
```

- [ ] **Step 2: Thread engine through VisitListScreen**

In `VisitListScreen.kt`, make these four targeted changes:

**2a. `VisitListScreenContent` — pass engine down to `VisitMapSheet`**

In the `VisitMapSheet(...)` call (around line 240), add `engine = visitListUiState.visitMapEngine`:

```kotlin
VisitMapSheet(
    isVisible = visitListUiState.showVisitMapSheet,
    currentCoordinate = visitListUiState.currentCoordinates,
    visitMapState = visitListUiState.visitMapState,
    engine = visitListUiState.visitMapEngine,
    onDismiss = {
        onVisitListEvent(VisitListViewModel.UiEvent.VisitMapSheetDismissed)
    },
    onVisitMapEvent = onVisitMapEvent
)
```

**2b. `VisitMapSheet` signature — add engine parameter**

```kotlin
private fun ColumnScope.VisitMapSheet(
    isVisible: Boolean,
    currentCoordinate: Pair<Double, Double>,
    visitMapState: VisitMapState,
    engine: VisitMapEngineOption,
    onDismiss: () -> Unit,
    onVisitMapEvent: (VisitsMapEvent) -> Unit
)
```

Pass it to `LazyLoadedVisitsMap`:

```kotlin
LazyLoadedVisitsMap(
    currentCoordinate = currentCoordinate,
    visitMapState = visitMapState,
    engine = engine,
    onVisitMapEvent = onVisitMapEvent
)
```

**2c. `LazyLoadedVisitsMap` signature — add engine parameter**

```kotlin
private fun LazyLoadedVisitsMap(
    currentCoordinate: Pair<Double, Double>,
    visitMapState: VisitMapState,
    engine: VisitMapEngineOption,
    onVisitMapEvent: (VisitsMapEvent) -> Unit
)
```

Pass it to `VisitsMap`:

```kotlin
VisitsMap(
    currentLocation = currentCoordinate,
    visitMapState = visitMapState,
    engine = engine,
    onMapEvent = onVisitMapEvent
)
```

**2d. Add import at the top of `VisitListScreen.kt`:**

```kotlin
import com.msmobile.visitas.visit.VisitMapEngineOption
```

- [ ] **Step 3: Compile check**

```
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```
git add app/src/main/java/com/msmobile/visitas/visit/VisitsMap.kt \
        app/src/main/java/com/msmobile/visitas/visit/VisitListScreen.kt
git commit -m "Thread VisitMapEngineOption through VisitListScreen and VisitsMap"
```

---

### Task 7: Move Leaflet assets to map/leaflet/ subfolder

**Files:**
- Move: `app/src/main/assets/map/*` → `app/src/main/assets/map/leaflet/`

- [ ] **Step 1: Move the files**

```bash
mkdir -p app/src/main/assets/map/leaflet
mv app/src/main/assets/map/visits-map.html \
   app/src/main/assets/map/leaflet-2.0.0-alpha.1.js \
   app/src/main/assets/map/leaflet-2.0.0-alpha.1.css \
   app/src/main/assets/map/leaflet/
mv app/src/main/assets/map/images app/src/main/assets/map/leaflet/images
```

- [ ] **Step 2: Verify structure**

```bash
find app/src/main/assets/map/leaflet -type f
```

Expected output:
```
app/src/main/assets/map/leaflet/visits-map.html
app/src/main/assets/map/leaflet/leaflet-2.0.0-alpha.1.js
app/src/main/assets/map/leaflet/leaflet-2.0.0-alpha.1.css
app/src/main/assets/map/leaflet/images/marker-icon.png
app/src/main/assets/map/leaflet/images/marker-icon-2x.png
app/src/main/assets/map/leaflet/images/marker-shadow.png
```

- [ ] **Step 3: Compile check**

```
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```
git add app/src/main/assets/map/
git commit -m "Move Leaflet assets to map/leaflet/ subfolder"
```

---

### Task 8: Vendor MapLibre GL JS v5.24.0

**Files:**
- Create: `app/src/main/assets/map/maplibre/maplibre-gl.js`
- Create: `app/src/main/assets/map/maplibre/maplibre-gl.css`

- [ ] **Step 1: Download and vendor the assets**

```bash
mkdir -p app/src/main/assets/map/maplibre
curl -L "https://unpkg.com/maplibre-gl@5.24.0/dist/maplibre-gl.js" \
     -o "app/src/main/assets/map/maplibre/maplibre-gl.js"
curl -L "https://unpkg.com/maplibre-gl@5.24.0/dist/maplibre-gl.css" \
     -o "app/src/main/assets/map/maplibre/maplibre-gl.css"
```

- [ ] **Step 2: Verify files**

```bash
ls -lh app/src/main/assets/map/maplibre/
```

Expected: two files, `maplibre-gl.js` (~900 KB) and `maplibre-gl.css` (~10 KB).

- [ ] **Step 3: Commit**

```
git add app/src/main/assets/map/maplibre/maplibre-gl.js \
        app/src/main/assets/map/maplibre/maplibre-gl.css
git commit -m "Vendor MapLibre GL JS v5.24.0"
```

---

### Task 9: MapLibre visits-map.html

**Files:**
- Create: `app/src/main/assets/map/maplibre/visits-map.html`

- [ ] **Step 1: Create the HTML with the same JS contract as the Leaflet engine**

Create `app/src/main/assets/map/maplibre/visits-map.html`:

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8"/>
    <meta name="viewport"
          content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"/>
    <title>MapLibre Map</title>
    <link rel="stylesheet" href="maplibre-gl.css"/>
    <style>
        #map { height: 100vh; width: 100%; }
        body { margin: 0; padding: 0; }
        .maplibregl-popup-content { padding: 8px 12px; }
    </style>
</head>
<body>
<div id="map"></div>
<script src="maplibre-gl.js"></script>
<script>
    let map;
    let markers = [];
    let routeAdded = false;
    let pendingSetMarkers = null;
    let labels = { currentLocation: undefined };
    let didFitBounds = false;

    function log(message) {
        console.log(message);
        if (window.Visits && window.Visits.log) window.Visits.log(message);
    }

    function logError(message) {
        console.error(message);
        if (window.Visits && window.Visits.error) window.Visits.error(message);
    }

    log('Script loaded successfully');

    async function initializeMap(currentLocationText) {
        try {
            log('Initializing map');

            if (typeof maplibregl === 'undefined') {
                throw new Error('MapLibre GL library not loaded');
            }

            labels.currentLocation = currentLocationText;

            map = new maplibregl.Map({
                container: 'map',
                style: 'https://demotiles.maplibre.org/style.json',
                center: [0, 0],
                zoom: 2
            });

            map.on('load', () => {
                map.addSource('route', {
                    type: 'geojson',
                    data: { type: 'FeatureCollection', features: [] }
                });
                map.addLayer({
                    id: 'route',
                    type: 'line',
                    source: 'route',
                    paint: {
                        'line-color': '#2E86AB',
                        'line-width': 4,
                        'line-opacity': 0.8
                    },
                    layout: {
                        'line-join': 'round',
                        'line-cap': 'round'
                    }
                });
                routeAdded = true;
                log('Map initialized successfully');
                if (window.Visits && window.Visits.onMapReady) window.Visits.onMapReady();

                if (pendingSetMarkers) {
                    const { latitude, longitude, visits } = pendingSetMarkers;
                    pendingSetMarkers = null;
                    setMarkers(latitude, longitude, visits);
                }
            });

            map.on('error', (e) => {
                logError('MapLibre error: ' + e.error.message);
            });

        } catch (error) {
            logError('Map initialization error: ' + error.message);
            if (window.Visits && window.Visits.onMapInitializationError) {
                window.Visits.onMapInitializationError(error.message || error.toString());
            }
        }
    }

    async function setMarkers(latitude, longitude, visits) {
        try {
            if (!map) {
                logError('Map not initialized');
                return;
            }

            if (!routeAdded) {
                pendingSetMarkers = { latitude, longitude, visits };
                return;
            }

            log('Setting markers...');

            // Clear existing markers
            markers.forEach(m => m.remove());
            markers = [];

            // Current location marker
            const currentMarker = new maplibregl.Marker({ color: '#0078D4' })
                .setLngLat([longitude, latitude])
                .setPopup(new maplibregl.Popup({ offset: 25 }).setText(labels.currentLocation))
                .addTo(map);
            markers.push(currentMarker);

            const bounds = new maplibregl.LngLatBounds();
            bounds.extend([longitude, latitude]);

            // Clear route
            map.getSource('route').setData({ type: 'FeatureCollection', features: [] });

            if (visits && visits.length > 0) {
                const sortedVisits = visits.slice().sort((a, b) => a.visitOrder - b.visitOrder);

                sortedVisits.forEach((visit, index) => {
                    if (!visit || !visit.householderLatitude || !visit.householderLongitude) return;

                    const {
                        householderLatitude, householderLongitude,
                        householderName, householderDistance,
                        visitSubject, visitOrder, householderAddress
                    } = visit;

                    log('Adding marker for: ' + householderName + ' (order: ' + visitOrder + ')');

                    // Custom marker element: red pin with order number
                    const el = document.createElement('div');
                    el.style.cssText = 'position:relative;cursor:pointer;';
                    el.innerHTML =
                        '<svg width="25" height="41" viewBox="0 0 25 41" xmlns="http://www.w3.org/2000/svg">' +
                        '<path d="M12.5 0C5.6 0 0 5.6 0 12.5c0 9.4 12.5 28.5 12.5 28.5S25 21.9 25 12.5C25 5.6 19.4 0 12.5 0z" fill="#dc2626"/>' +
                        '</svg>' +
                        '<div style="position:absolute;top:4px;left:0;width:25px;text-align:center;color:white;font-weight:bold;font-size:11px;line-height:1;">' +
                        (visitOrder + 1) +
                        '</div>';

                    const popupContent =
                        '<div>' +
                        '<div style="display:flex;justify-content:space-between;align-items:center;">' +
                        '<strong>' + householderName + '</strong>' +
                        '<span style="font-size:0.9em;color:#666;margin-left:8px;">Order: ' + (visitOrder + 1) + '</span>' +
                        '</div>' +
                        '<div style="display:flex;justify-content:space-between;align-items:center;">' +
                        '<span>' + visitSubject + '</span>' +
                        '<span style="font-size:0.9em;color:#666;">' + (householderDistance || '') + '</span>' +
                        '</div>' +
                        householderAddress +
                        '</div>';

                    const marker = new maplibregl.Marker({ element: el, anchor: 'bottom' })
                        .setLngLat([householderLongitude, householderLatitude])
                        .setPopup(new maplibregl.Popup({ offset: 5 }).setHTML(popupContent))
                        .addTo(map);

                    el.addEventListener('click', () => {
                        if (window.Visits && window.Visits.onMarkerClicked) {
                            window.Visits.onMarkerClicked('marker_' + index);
                        }
                    });

                    markers.push(marker);
                    bounds.extend([householderLongitude, householderLatitude]);
                });

                // Draw route
                if (sortedVisits.length > 0 && sortedVisits[0].routeGeometry) {
                    log('Drawing route with geometry');
                    map.getSource('route').setData(sortedVisits[0].routeGeometry);
                } else if (sortedVisits.length > 1) {
                    log('Drawing simple route line');
                    const routePoints = [[longitude, latitude]];
                    sortedVisits.forEach(visit => {
                        if (visit.householderLatitude && visit.householderLongitude) {
                            routePoints.push([visit.householderLongitude, visit.householderLatitude]);
                        }
                    });
                    if (routePoints.length > 1) {
                        map.getSource('route').setData({
                            type: 'Feature',
                            geometry: { type: 'LineString', coordinates: routePoints }
                        });
                    }
                }

                if (!didFitBounds && !bounds.isEmpty()) {
                    didFitBounds = true;
                    map.fitBounds(bounds, { padding: 40 });
                }

                log('All markers added successfully');
            } else {
                log('No visits provided or visits array is empty.');
                if (!didFitBounds) {
                    didFitBounds = true;
                    map.setCenter([longitude, latitude]);
                    map.setZoom(15);
                }
            }
        } catch (error) {
            logError('Error setting markers: ' + error.message);
            if (window.Visits && window.Visits.onMapInitializationError) {
                window.Visits.onMapInitializationError(error.message || error.toString());
            }
        }
    }

    window.onerror = function(message, source, lineno, colno, error) {
        logError('JavaScript error: ' + message + ' at ' + source + ' ' + lineno + ':' + colno);
        if (window.Visits && window.Visits.onMapInitializationError) {
            window.Visits.onMapInitializationError(message);
        }
    };
</script>
</body>
</html>
```

- [ ] **Step 2: Run all unit tests to confirm nothing is broken**

```
./gradlew :app:testDebugUnitTest
```

Expected: all tests PASS.

- [ ] **Step 3: Commit**

```
git add app/src/main/assets/map/maplibre/visits-map.html
git commit -m "Add MapLibre GL visits-map.html with same JS contract as Leaflet engine"
```

---

### Task 10: Final verification

- [ ] **Step 1: Build the debug APK**

```
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Manual verification checklist**

Install the debug APK on a device or emulator and verify:

- [ ] Open Settings → "Map Engine" dropdown shows "MapLibre" selected by default
- [ ] Select "Leaflet" → dropdown updates, reopen Settings → still shows "Leaflet" (persisted)
- [ ] Open the map sheet → Leaflet map renders with markers and route
- [ ] Switch back to "MapLibre" in Settings → open map sheet → MapLibre map renders with markers and route
- [ ] Logcat shows no `VisitsMapJS` errors during either engine's initialization

- [ ] **Step 3: Create PR**

```
git push -u origin <branch-name>
gh pr create --title "Add MapLibre GL as second map engine with Settings toggle"
```
