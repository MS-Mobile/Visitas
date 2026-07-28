---
name: tech-debt-workflow
description: Use when picking up, filing, or fixing a tech-debt / code-smell item in Visitas
---

# Tech-debt workflow

Tech-debt and code-smell findings are tracked **as GitHub issues** labeled `tech-debt` (plus `bug` for live defects) — **not** in a committed markdown registry. The user decided against a `docs/code-smells.md` file (that branch was abandoned).

## How to work it

- **Fix one issue per session:** pick an issue, fix it, and close it via `Fixes #N` in the PR.
- **File new items** with the `.github/ISSUE_TEMPLATE/tech_debt.yml` template. It has no registry-ID field (the user removed it).
- The audit **methodology** spec lives at `docs/superpowers/specs/2026-06-12-code-smell-audit-design.md` — methodology only, no findings.

## Origin (point-in-time, verify against GitHub)

A 2026-06-12 audit produced 15 high-impact findings tracked as issues in roughly the `#193`–`#208` range (e.g. release HTTP body logging, hash-sum dirty-check data loss, wrong dropdown-dismiss event, "Main St, null" addresses, calendar-sync duplication, static backup IV, god-class ViewModels, events-as-state/navigation-during-composition, Activity leak, swallowed exceptions, DI-provider bypasses, no static-analysis gate). Check the live `tech-debt` label for current open/closed state before acting.
