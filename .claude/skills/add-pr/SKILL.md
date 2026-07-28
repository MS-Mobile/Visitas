---
name: add-pr
description: Use when the user says "Add PR", "open a PR", or asks to finish a branch with a pull request
---

# Add PR routine

When asked to "Add PR" or finish work with a pull request, do these in order:

1. Create a feature branch (if not already on one).
2. Commit all changed files.
3. Push to remote.
4. Open a PR whose body follows `.github/PULL_REQUEST_TEMPLATE.md` — fill every section (Description, Type of Change checkboxes, Checklist). Never submit a plain-prose body. This applies even when creating the PR via the GitHub API.

## PR target branch

If the branch was cut from `release/**`, the PR must target that same release branch (e.g. `release/1.2.0`), not `master`. Master receives the change later via the backmerge workflow. See the `release-pipeline` skill.

## Compose screenshot references

For any Compose UI / preview change, regenerate and commit reference screenshots **before** opening the PR:

- Gradle task: `:app:updateDebugScreenshotTest` (the user calls this "updateDebugScreenshots").
- Reference PNGs live under `app/src/screenshotTestDebug/reference/**`; `git status` shows which changed.
- Shared detail chrome (`DetailFooterAction`) means one detail-screen change can shift both `VisitDetailScreenshotTest` and `ConversationDetailScreenshotTest` references.
