---
name: visit-list-filters
description: Use when adding or changing a filter in the visit list (VisitListViewModel.filterBy) — type, date, distance, search, or a new one
---

# Visit list filter invariants

In `VisitListViewModel.filterBy`, two invariants must always hold:

- **Drafts are always visible** — `isDraft` short-circuits every filter (type, date, distance, search).
- **A name-search match is always visible** — an explicit search is a specific ask and must not be narrowed by the type filter or any other filter.

Only the default (empty-search) date/distance view is constrained by additional filters like `matchesType`. The shape is:

```kotlin
show = isDraft || matchesName || (otherFilters && isSearchEmpty && (...))
```

**Why:** filters trim the browsing view; drafts are work-in-progress the user must not lose sight of, and a typed search expresses direct intent that overrides browsing filters.

**How to apply:** when introducing a new filter, AND it into the empty-search branch only — never wrap `isDraft` or `matchesName` with it.
