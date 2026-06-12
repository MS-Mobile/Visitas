# Code Smell Audit — Design

**Date:** 2026-06-12
**Status:** Approved

## Goal

Find high-impact code smells and anti-patterns in the Visitas app Kotlin code and document them as tracked issues, so they can be fixed one by one at a later time. An audit pass produces documentation only — no fixes.

This document describes the audit method so it can be repeated; it intentionally contains no findings.

## Scope

- **In scope:** Kotlin code under `app/src/main/java/com/msmobile/visitas/`.
- **Out of scope:** the fixes themselves, test code (`test/`, `androidTest/`), vendored JS map assets (`assets/map/`), Gradle build scripts, and CI workflow internals — with one exception: a single meta-finding about the absence of a static-analysis gate (detekt/lint) in CI, if it holds up.

## Severity bar

High-impact only: structural problems and true anti-patterns whose consequence is a leak, crash, data bug, untestability, or broken layering. Examples: god classes, leaked coroutine scopes, business logic in composables, blocking calls on the main thread, large copy-paste duplication blocks. Style and naming issues do not qualify. Expected volume: roughly 10–20 entries.

## Deliverable

One GitHub issue per finding, created with the **Tech Debt** issue template (`.github/ISSUE_TEMPLATE/tech_debt.yml`). Findings that are live defects (user-visible bugs, data loss, security exposure) additionally get the `bug` label so they surface in the normal bug flow.

Each issue carries:

| Field | Content |
|---|---|
| Title | `[Tech Debt]: short description` |
| Description | What the debt is — the current state of the code |
| Location | `file:line` references |
| Impact | The concrete consequence, not a style preference |
| Suggested Fix | Direction only — fixes are designed when they are picked up |
| Effort | S (under an hour) / M (a focused session) / L (multi-session refactor) |

## Audit procedure

1. **Inventory pass** — line counts across all in-scope Kotlin files to surface god-class candidates.
2. **Marker grep pass** — search for smell markers calibrated to this project's own conventions:
   - Direct `Dispatchers.*`, `LocalDate.now()` / `LocalDateTime.now()`, `Locale.getDefault()` usage (violates the project's DI provider rule: inject `DispatcherProvider` / `DateTimeProvider` / `LocaleProvider`)
   - `GlobalScope`, `runBlocking`, ad-hoc `CoroutineScope(...)` construction
   - `!!` non-null assertions
   - Broad `catch (e: Exception)` / swallowed exceptions
   - `@SuppressLint`
   - Business logic in composables, mutable singletons / global state
3. **Deep read** — full read of the ~10 flagged hotspot files, judging structure: single responsibility, layering violations, duplication blocks.
4. **Write-up** — only findings meeting the severity bar become issues.

## Non-goals

- No code changes, refactors, or fixes in this pass.
- No exhaustive lint-style sweep; noise is worse than a miss at this severity bar.
- No tooling integration (adding detekt to the build is a potential *finding*, not part of this work).

## Success criteria

- Every finding meeting the severity bar exists as a Tech Debt issue carrying all fields above.
- Each issue is actionable in isolation: a future session can pick one issue and fix it without re-running the audit.
