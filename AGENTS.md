# AGENTS.md — Visitas Codebase Guide

## Architecture Overview

**MVVM + Unidirectional Data Flow (UDF).** Every screen has a dedicated ViewModel exposing a single `StateFlow<UiState>`. User actions flow in as `UiEvent`, and one-time side effects are modeled via `UiEventState` (Idle / Loading / Success / Error). The three nested classes (`UiState`, `UiEvent`, `UiEventState`) live **at the bottom** of each ViewModel file.

```
Screen composable ──UiEvent──▶ ViewModel.onEvent()
                  ◀──UiState── ViewModel._uiState (StateFlow)
```

Key files: `VisitListViewModel.kt`, `VisitDetailViewModel.kt`, `ConversationDetailViewModel.kt`.

## Package Structure

```
com.msmobile.visitas/
├── visit/          # Core feature: visit list & detail (largest feature)
├── householder/    # Householder entity, DAO, repository
├── conversation/   # Conversation list & detail
├── fieldservice/   # Field service timer tracking
├── summary/        # Monthly statistics summary
├── backup/         # Backup/restore ViewModel + Sheet composable
├── routing/        # OSRM route optimization (OsrmRoutingProvider, OsrmService)
├── migration/      # Room migration objects (MIGRATION_1_2 … MIGRATION_5_6)
├── serialization/  # Moshi adapters (LocalDateTime, UUID, SerializationFactory)
├── preference/     # Single-row user preferences (Room entity)
├── di/             # ApplicationModule.kt + NavigationDependencies.kt
├── util/           # Helpers (StringResource, DispatcherProvider, BackupHandler, etc.)
├── extension/      # Kotlin extension functions
└── ui/             # theme/ + views/ (reusable composables)
```

## Critical Patterns

### ViewModel Construction
All nested classes go at the **bottom** of the ViewModel file. The `onEvent` function contains a flat `when` expression dispatching to private handlers.

```kotlin
@HiltViewModel
class FeatureViewModel @Inject constructor(...) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState(...))
    val uiState: StateFlow<UiState> = _uiState

    fun onEvent(event: UiEvent) { when (event) { ... } }

    // --- bottom of file ---
    sealed class UiEvent { ... }
    sealed class UiEventState { data object Idle : UiEventState() }
    data class UiState(val eventState: UiEventState = UiEventState.Idle, ...)
}
```

### Screen Composable Pattern
Screens receive `uiState` and `onEvent` in a private `*Content` composable. `@Destination` is on the public composable only.

```kotlin
@Destination
@Composable
fun FeatureScreen(viewModel: FeatureViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FeatureScreenContent(uiState = uiState, onEvent = viewModel::onEvent)
}
```

### Navigation + Multi-ViewModel Screens
ViewModels are wired to screens via **`di/NavigationDependencies.kt`** using `dependency(hiltViewModel<T>())`. Some screens receive multiple ViewModels this way (e.g., `VisitListScreenDestination` gets `VisitListViewModel`, `SummaryViewModel`, and `BackupViewModel`). When adding a new screen that needs Hilt ViewModels, register them there.

### VisitHouseholder is a Database View
`VisitHouseholder` is annotated `@DatabaseView`, not `@Entity`. It joins `visit` and `householder` and is registered in `VisitasDatabase` under `views = [VisitHouseholder::class]`. Do not add it to `entities`.

### StringResource Utility
ViewModels pass `StringResource(@StringRes textResId: Int, arguments: List<Any>)` to `UiState` to keep UI strings out of the data layer. Resolve it in the composable with `stringResource`.

### Injected Providers — never call static platform APIs in a DI class
In any class that participates in Hilt DI (ViewModel, repository, AppFunctions, …), use the injected
provider wrapper instead of the static call. This keeps tests deterministic — time, locale, IDs and
dispatchers can all be mocked, with no reflection.

| Instead of | Use |
|---|---|
| `LocalDate.now()` | `dateTimeProvider.nowLocalDate()` |
| `LocalDateTime.now()` | `dateTimeProvider.nowLocalDateTime()` |
| `Date()` / `System.nanoTime()` | `dateTimeProvider.nowDate()` / `dateTimeProvider.nanoTime()` |
| `Locale.getDefault()` | `localeProvider.getLocale()` |
| `UUID.randomUUID()` | `idProvider.generateId()` |
| `Dispatchers.IO` / `Dispatchers.Main` | `dispatcherProvider.io` / `dispatcherProvider.main` |

All are `@Singleton`s provided in `di/ApplicationModule.kt`, declared in `util/`, and taken via
`@Inject constructor`. There is no static-analysis rule enforcing this yet — it is review-enforced.

### Database Migrations
Add new migrations in `migration/` following the `MIGRATION_N_(N+1).kt` naming convention, then register them in `VisitasDatabase.MIGRATIONS`.

Bumping the `@Database` version also produces a new exported schema JSON in `app/schemas/`,
which must be committed alongside the migration. Room exports these during **KSP compilation**
(the `androidx.room` plugin's `copyRoomSchemas` task copies them into the `schemaDirectory` set in
`app/build.gradle.kts`) — the app does not need to be installed or launched:

```bash
sh scripts/verify-room-schemas.sh                # export + assert committed schemas match
sh scripts/verify-room-schemas.sh --export-only   # just regenerate app/schemas/
```

**A plain `assembleDebug` is not enough to regenerate them.** The directory KSP writes schemas to
is not a declared cacheable output of the KSP task, so whenever `kspDebugKotlin` is restored
`FROM-CACHE` (the norm on CI) `copyRoomSchemas` reports `NO-SOURCE` and no schema is exported.
The script therefore runs both tasks with `--rerun` and fails if nothing was written.

The PR build runs that same check (`Verify Room Schemas Are Committed`) against the schemas the
build just regenerated, so a forgotten or stale schema JSON fails the PR. To fix it without
building locally, dispatch the **Regenerate Room Schemas** workflow
(`.github/workflows/regenerate-room-schemas.yml`) for your branch and it commits them for you.

## Build & Developer Workflows

### Build Commands
```bash
./gradlew assembleDebug            # debug build
./gradlew assembleRelease          # requires env vars (see below)
./gradlew test                     # unit tests
./gradlew connectedAndroidTest     # instrumented tests (device required)
./gradlew installGitHooks          # install pre-commit hook
```

### Required Environment Variables (release only)
`VERSION_CODE`, `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEYSTORE_ALIAS`, `ENCRYPTION_PASSPHRASE`, `SENTRY_DSN`. Sentry source-map upload also needs `SENTRY_ORG`, `SENTRY_PROJECT`, `SENTRY_AUTH_TOKEN`.

### Version Name
Comes from `version.properties` (root). `versionCode` is the `VERSION_CODE` env var.

### Dependencies
All dependencies are declared in `gradle/libs.versions.toml`. Never add them directly to `build.gradle.kts`.

## Testing Conventions

### Unit Tests (app/src/test/)
- Always include `@get:Rule val mainDispatcherRule = MainDispatcherRule()`.
- Mock all dependencies inside a `createViewModel()` factory. Never configure mocks outside it.
- Use `MockReferenceHolder<T>` when a test needs to verify or interact with a mock after VM creation.
- No `whenever(...)` stubbing inside `@Test` methods — use factory parameters instead.

```kotlin
private fun createViewModel(
    loadResult: List<Item> = emptyList(),
    repoRef: MockReferenceHolder<FeatureRepository>? = null
): FeatureViewModel {
    val repo = mock<FeatureRepository> { on { getAll() } doReturn MutableStateFlow(loadResult) }
    repoRef?.value = repo
    return FeatureViewModel(repo, DispatcherProvider(UnconfinedTestDispatcher()))
}
```

### Instrumented Tests (app/src/androidTest/)
Use `@HiltAndroidTest` + `HiltAndroidRule`. Test runner is `HiltTestRunner`.

### Screenshot Tests (app/src/screenshotTest/)
Compose Preview screenshot tests (`*ScreenshotTest.kt`) render each case supplied by a
`PreviewParameterProvider` (e.g. `VisitListPreviewConfigProvider`) and diff against reference
PNGs in `app/src/screenshotTestDebug/reference/`. The PR build runs `validateDebugScreenshotTest`.

- **To exercise a new rendering, add a new variant at the end of the provider — never edit the
  shared preview state.** Providers build most variants from one shared `UiState` (e.g.
  `previewVisitListUiState`), so mutating that shared value ripples into *every* variant's
  baseline and produces a huge, noisy screenshot diff. Adding a variant (via `.copy(...)` of
  the shared state) only creates new reference images and leaves existing baselines untouched.
- Reference PNGs are regenerated and committed by the **Regenerate Screenshots** workflow
  (`.github/workflows/regenerate-screenshots.yml`, runs `updateDebugScreenshotTest`), dispatched
  per branch. Dispatch it after any intended UI change so `validateDebugScreenshotTest` passes.

## Pre-commit Hook
Modifying `VisitasDatabase.kt` triggers `BackupHandlerTest` automatically (requires a connected device). Run `./gradlew installGitHooks` once after cloning.

## External Integrations
| Concern | Library / Service |
|---|---|
| Route optimization | OSRM (self-hosted) via Retrofit — `routing/OsrmService.kt` |
| Error monitoring | Sentry (`sentry-android`, `sentry-compose`, `sentry-okhttp`) |
| Location | Google Play Services `FusedLocationProviderClient` |
| Backup encryption | `androidx.security:security-crypto` via `EncryptionHandler` |
| In-app update | Google Play `app-update-ktx` |

