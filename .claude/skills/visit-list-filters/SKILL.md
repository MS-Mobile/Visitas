---
name: visit-list-filters
description: Use when adding or changing a filter in the visit list (VisitListViewModel.filterBy) — date, distance, search, or a new one
---

# Visit list filter invariants

`VisitFilter` destructures to `(search, dateFilter, distanceFilter)`. In
`VisitListViewModel.filterBy`, two invariants must always hold:

- **Drafts are always visible** — `hasDrafts` short-circuits every filter (date, distance, search).
- **A name-search match is always visible** — an explicit search is a specific ask and must not be
  narrowed by any other filter.

Only the default (empty-search) browsing view is constrained by the date/distance filters. The
current shape:

```kotlin
val show = hasDrafts
        || isSearchEmpty && (matchesDate || matchesDistance)
        || matchesName
visit.copy(hide = !show)
```

Note that filtering marks each item `hide = !show` rather than dropping it from the list.

Drafts are also **sorted first** — the sort is
`compareByDescending { hasDrafts }.thenByDescending { matchesDistance }.thenBy { date }`. Keep both
halves of the draft guarantee: visible *and* on top.

**Why:** filters trim the browsing view; drafts are work-in-progress the user must not lose sight of,
and a typed search expresses direct intent that overrides browsing filters.

**How to apply:** when introducing a new filter, OR it into the empty-search branch alongside
`matchesDate`/`matchesDistance` (or AND it into that branch if it should narrow rather than widen) —
never wrap `hasDrafts` or `matchesName` with it.
