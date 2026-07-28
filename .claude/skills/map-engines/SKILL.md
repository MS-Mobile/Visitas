---
name: map-engines
description: Use when working on the visit map — Leaflet or MapLibre GL rendering, markers, the JS↔Kotlin bridge, or the map engine preference
---

# Dual map engine setup

Two map engines are vendored under `app/src/main/assets/map/`, each in its own subfolder, both implementing the same JS bridge.

## Leaflet (`assets/map/leaflet/`)

- `leaflet-2.0.0-alpha.1.js` — the **IIFE/global** build (`leaflet-global.js` from unpkg, NOT `leaflet.js`, which is ESM-only), plus its CSS and `images/` marker PNGs.
- v2 global is `window.leaflet`; instantiate with `new leaflet.ClassName()` — lowercase factory functions were removed in v2.
- Coordinate order: `[latitude, longitude]`.

## MapLibre GL JS (`assets/map/maplibre/`)

- `maplibre-gl.js` / `maplibre-gl.css` — v5.24.0.
- Tile style: OpenFreeMap liberty (`https://tiles.openfreemap.org/styles/liberty`) — free, no API key.
- Dark mode: CSS `filter: brightness(0.7)` on `#map` via `@media (prefers-color-scheme: dark)`.
- Visit markers are SVG-only (pin + number in one element, `anchor: 'bottom'`); **`position: relative` on the wrapper breaks MapLibre's anchor — don't add it.**
- `fitBounds` uses `{ animate: false }`.
- `pendingSetMarkers` handles the race when `setMarkers` is called before `map.on('load')` fires.
- Coordinate order: `[longitude, latitude]` (GeoJSON) — the `setMarkers` call from Kotlin sends `latitude, longitude`, so **swap to `[lng, lat]`** here.

## Engine selection

- `VisitMapEngineOption` enum (`MapLibre` default, `Leaflet`), persisted in the `Preference` table (Room migration 7→8).
- Settings screen dropdown; `SettingsDetailViewModel` reads/saves via `PreferenceRepository`. `VisitListViewModel.UiState.visitMapEngine` exposes the active engine.
- `VisitsMap.kt` calls `assetPath(engine)` to select the HTML; `engine` threads `VisitListScreen → VisitMapSheet → LazyLoadedVisitsMap → VisitsMap`.

## JS bridge contract (both engines identical)

- Kotlin → JS: `initializeMap(currentLocationText)`, `setMarkers(lat, lng, visitsJson)`.
- JS → Kotlin (`window.Visits.*`): `onMapReady()`, `onMarkerClicked(id)`, `onMapInitializationError(msg)`, `log(msg)`, `error(msg)`.
- `VisitsMapJavascriptInterface(onMapError: (String) -> Unit, onMapReady: () -> Unit)`; JS callbacks arrive on a background thread — `onMapReady` is dispatched to main via `Handler(Looper.getMainLooper())`.
- `LazyLoadedVisitsMap` renders the WebView immediately, overlays the loading state, and fades it out only after `onMapReady` fires (`isMapEngineReady`, reset via `remember(engine)`).
