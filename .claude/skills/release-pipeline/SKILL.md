---
name: release-pipeline
description: Use when cutting a release, deploying to Play, bumping version codes, or opening a PR against a release/** branch
---

# CI/CD release pipeline

## Three workflows

- **`cut-release.yml`** — manual; creates `release/X.Y.Z` from master **via the GitHub API** (a plain git push wouldn't trigger downstream workflows). Its optional `version_name` input creates `release/<input>` **without** updating `version.properties` on the branch, so the branch-name guard will fail it — that input path is half-broken; prefer the version-matched path.
- **`release-build.yml`** — on push to `release/**`; **build-only**. Builds signed AAB/APK + tests, uploads artifacts (400-day retention; `build-info` = `version-code.txt` + `commit-sha.txt`), and opens an idempotent next-minor version-bump PR on master. It does **not** tag — per-build tags previously spammed a tag+Release on every push.
- **`deploy.yml`** — manual (`workflow_dispatch` with `release_branch` input); downloads the latest successful release-build artifacts from that branch, uploads to the Play internal track, then tags the built commit SHA `v{versionName}` and creates the GitHub Release. Guards: tag/Release must not already exist; built SHA must be an ancestor of the release branch. Changelog is generated at deploy time (commits since previous tag).

`versionName` lives in `version.properties`; workflows grep it from the **checked-out branch**.

## Version-code pitfall

`VERSION_CODE = VERSION_CODE_OFFSET (repo Actions variable) + run_number*10 + run_attempt`.

`github.run_number` is scoped to the workflow **file path** — **renaming the workflow file resets it to 1.** A past rename regressed codes below what was already live on Play, and Play rejected the upload. **Fix / rule: keep `VERSION_CODE_OFFSET` above the last deployed code. Any future workflow rename requires raising the offset again.**

## Release-branch PR rule

A PR whose base is a release branch **MUST be cut from that release branch**, not master — otherwise the squash-merge silently carries master's diff (including version bumps) onto the release branch. `release-build` and `deploy` fail fast if branch name ≠ `release/<versionName>`.

## Backmerge

`backmerge.yml` (push to `release/**`) builds `backmerge/<version>` from the release head, pre-merges master, and opens an idempotent PR to master (clean → green PR; conflict → ⚠️ PR listing conflicting files). **Merge-commit it, never squash.** Push-triggered workflows run from the pushed branch's copy of the file, so release branches must inherit these workflow files from master when cut.
