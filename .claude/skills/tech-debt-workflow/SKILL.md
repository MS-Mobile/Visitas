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

## Finding the current backlog

Query the live `tech-debt` label — it is the source of truth for what is open. The initial batch came
from a 2026-06-12 audit (15 high-impact findings), but open/closed state has moved since, so never
work from a remembered list of issue numbers.
