# MapLibre GL as Second Map Engine

**Date:** 2026-06-04
**Status:** Approved

## Overview

Add MapLibre GL as the default map engine alongside the existing Leaflet engine. Users select their preferred engine from a dropdown in Settings. The choice is persisted to the Room database via the existing `Preference` table. Both engines honour an identical JS contract so the Kotlin WebView bridge requires no engine-specific logic.

---

## 1. Data Layer

### `VisitMapEngineOption`

New file `app/src/main/java/com/msmobile/visitas/visit/VisitMapEngineOption.kt`:

```kotlin
enum class VisitMapEngineOption { MapLibre, Leaflet }
```

`MapLibre` is the default.

### `Preference`

Add one field with a default value:

```kotlin
val visitMapEngineOption: VisitMapEngineOption = VisitMapEngineOption.MapLibre
```

A Room migration is required. The database version bumps from 7 → 8 with a new `MIGRATION_7_8`:

```kotlin
ALTER TABLE preference ADD COLUMN visitMapEngineOption TEXT NOT NULL DEFAULT 'MapLibre'
```

`MIGRATION_7_8` is added to `VisitasDatabase.MIGRATIONS` and the `@Database(version = ...)` annotation is updated to 8.

### `PreferenceTypeConverters`

Add two converters following the existing pattern:

```kotlin
@TypeConverter fun fromMapEngineOption(value: VisitMapEngineOption): String = value.name
@TypeConverter fun toMapEngineOption(value: String): VisitMapEngineOption =
    runCatching { VisitMapEngineOption.valueOf(value) }.getOrDefault(VisitMapEngineOption.MapLibre)
```

`PreferenceRepository.get()` requires no changes — the default value is on the entity field.

---

## 2. Settings Layer

### `SettingsDetailViewModel`

- Inject `PreferenceRepository` (new dependency).
- Add `UiEvent.ViewCreated` — on receipt, load the saved preference and populate state.
- Add `UiEvent.MapEngineSelected(engine: VisitMapEngineOption)` — save the updated preference and update state.
- `UiState` gains `val selectedMapEngine: VisitMapEngineOption = VisitMapEngineOption.MapLibre`.

Save pattern mirrors `visitsFilterOptionSelected` in `VisitListViewModel`:

```kotlin
private fun mapEngineSelected(engine: VisitMapEngineOption) {
    viewModelScope.launch(dispatchers.io) {
        val preference = preferenceRepository.get().copy(visitMapEngineOption = engine)
        preferenceRepository.save(preference)
    }
    _uiState.update { it.copy(selectedMapEngine = engine) }
}
```

### `SettingsScreen`

Add a dropdown row (same style as existing filter dropdowns) showing the two options:
- **MapLibre** (default, shown first)
- **Leaflet**

Sends `UiEvent.MapEngineSelected` on selection.

---

## 3. VisitListViewModel

`UiState` gains:

```kotlin
val visitMapEngine: VisitMapEngineOption = VisitMapEngineOption.MapLibre
```

Populated in `refreshVisits()` from the same `preferenceRepository.get()` call that already reads filter preferences — no extra async work.

---

## 4. VisitsMap.kt

The composable gains an `engine: VisitMapEngineOption` parameter. The hard-coded constant becomes a function:

```kotlin
private fun assetPath(engine: VisitMapEngineOption) = when (engine) {
    VisitMapEngineOption.MapLibre -> "file:///android_asset/map/maplibre/visits-map.html"
    VisitMapEngineOption.Leaflet  -> "file:///android_asset/map/leaflet/visits-map.html"
}
```

No other Kotlin changes — the JS contract is identical for both engines.

---

## 5. Assets

### File structure after this change

```
app/src/main/assets/map/
  leaflet/
    visits-map.html           (moved from assets/map/)
    leaflet-2.0.0-alpha.1.js
    leaflet-2.0.0-alpha.1.css
    images/
      marker-icon.png
      marker-icon-2x.png
      marker-shadow.png
  maplibre/
    visits-map.html           (new)
    maplibre-gl.js            (vendored, latest stable)
    maplibre-gl.css           (vendored, latest stable)
```

### JS contract (both engines must implement)

**Kotlin → JS (called via `executeScript`):**

| Call | When |
|---|---|
| `initializeMap(currentLocationText)` | After page load |
| `setMarkers(latitude, longitude, visitsJson)` | On every state update |

**JS → Kotlin (via `window.Visits`):**

| Callback | When |
|---|---|
| `window.Visits.onMapReady()` | Map initialised |
| `window.Visits.onMarkerClicked(markerId)` | Marker tapped |
| `window.Visits.onMapInitializationError(message)` | Any fatal error |
| `window.Visits.log(message)` | Debug logging |
| `window.Visits.error(message)` | Error logging |

### MapLibre tile source

`demotiles.maplibre.org` (free, no API key) as the default tile style. Dark/light mode and a production tile provider (MapTiler, Stadia Maps) are a follow-up concern.

---

## Out of Scope

- Dark/light map theme switching (follow-up)
- Production tile provider / API key management (follow-up)
- Removing Leaflet once MapLibre is proven stable (follow-up)
