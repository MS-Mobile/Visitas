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

### DispatcherProvider
Always inject `DispatcherProvider` instead of using `Dispatchers.IO` directly. This enables swapping dispatchers in tests without reflection.

### Database Migrations
Add new migrations in `migration/` following the `MIGRATION_N_(N+1).kt` naming convention, then register them in `VisitasDatabase.MIGRATIONS`.

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

