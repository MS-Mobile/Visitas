# Jetpack Compose Maintainability Checklist

Use this checklist when reviewing or authoring Compose screens in this project.

---

## 🔴 Critical

- [ ] **No side effects during composition** — Navigation calls, logging, state mutations must never be called directly inside a `when`/`if` during composition. Wrap them in `LaunchedEffect`.
- [ ] **`LaunchedEffect` keyed correctly** — Use `Unit` for fire-once effects. Use the driving state value (e.g. `uiState.eventState`) when the effect should re-run on change. Never use `null` as a key.
- [ ] **No phantom composables for side-effect-only logic** — Do not create composables whose sole job is to call `LaunchedEffect` or trigger navigation. Inline the `LaunchedEffect` in the nearest real composable instead.

---

## 🟡 Moderate

- [ ] **`AnimatedVisibility(visible = true)` is a no-op for exit** — If an item should only animate on enter/reorder, use `Modifier.animateItem()` directly on the item root instead of wrapping in `AnimatedVisibility`.
- [ ] **Composables accept a `modifier: Modifier = Modifier` parameter** — Every non-trivial composable should accept a modifier so callers can control layout and animation (e.g. `Modifier.animateItem()`).
- [ ] **`StateHandler`-style composables avoided** — Composables that only conditionally show dialogs or trigger side effects should be inlined into the parent composable, not extracted into a separate named composable.
- [ ] **Dialogs shown via `if` check, not a wrapper composable** — e.g. `if (uiState.eventState is DeleteConfirmation) { DeleteConfirmationDialog(...) }` directly in the content composable.

---

## 🟢 Style / Minor

- [ ] **`@Destination` only on the public entry-point composable** — Private `*Content` composables handle the actual UI; the public composable wires the ViewModel and navigator.
- [ ] **`collectAsStateWithLifecycle()` used** — Never use `collectAsState()` without lifecycle awareness.
- [ ] **All strings via `stringResource`** — No hardcoded string literals in composables.
- [ ] **`key` provided in `LazyColumn` / `LazyRow` items** — Always supply a stable, unique key to prevent incorrect recomposition on list changes.
- [ ] **`@PreviewParameter` used for preview variants** — Avoids duplicating multiple `@Preview` functions with slightly different state.
- [ ] **No logic in composable layer** — Derived values, null checks used to drive UI flags (e.g. `showDeleteButton`) should come from `UiState`, not be computed inline in the composable.

---

## ✅ Always-Good Patterns (don't change these)

- `onEvent` lambda hoisted to ViewModel — composables never own state logic.
- Screen composable receives `uiState` and `onEvent` only — no raw repository or ViewModel references below the entry-point composable.
- `OnBackPressed` extension used for back-press handling.
- `DispatcherProvider` injected in ViewModels — never `Dispatchers.IO` directly.

