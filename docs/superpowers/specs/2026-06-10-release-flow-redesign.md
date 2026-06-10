# Release Flow Redesign

**Date:** 2026-06-10

## Context

The current `release.yml` bundles too many concerns: CI build, Play Store deployment, git tag creation, and GitHub release creation all live in one workflow triggered manually from master. The goal is to split this into three workflows with a single responsibility each, and introduce a release branch model so master can advance independently while a release is being stabilized and shipped.

## Goals

- Release artifacts are only ever built from `release/**` branches
- Master CI is fast and simple (no Play Store involvement, no VERSION_CODE_OFFSET)
- Cutting a release branch is a single manual workflow dispatch (no local git required)
- Creating a release branch automatically opens a version bump PR on master
- Play Store deployment remains a manual step (no accidental deploys)

## Non-Goals

- Multi-track Play Store promotion (alpha → beta → production) — stays on internal track
- Fastlane or any deploy tooling change — keep `r0adkll/upload-google-play`
- Automated cherry-pick or merge-back from release branch to master

---

## Workflow Structure

### 1. `build.yml` (modified) — master CI

**Trigger:** push to `master`, PRs to `master`

**Changes from current:**
- Drops VERSION_CODE_OFFSET logic entirely
- Switches from release build tasks to debug-only: `assembleDebug test validateDebugScreenshotTest`
- Removes build-info artifact upload (no longer needed on master)
- Removes 400-day retention artifacts (only release branches need those)

**Rationale for debug-only:** passing a stub `VERSION_CODE=1` to a release build produces a valid AAB that could accidentally be uploaded. Debug-only removes the ambiguity.

---

### 2. `cut-release.yml` (new) — manual release branch creation

**Trigger:** `workflow_dispatch`

**Inputs:**

| Input | Required | Default |
|---|---|---|
| `version_name` | no | read from `version.properties` on master |

**Steps:**
1. Checkout master
2. Read version from `version.properties` (or use input override)
3. **Guard:** verify `release/{version}` does not already exist on remote — fail fast
4. Create and push `release/{version}` branch from master HEAD

Pushing the branch automatically triggers `release-build.yml`.

**Token:** uses `WORKFLOW_TOKEN` PAT so the push triggers downstream CI (same restriction as `branch-sync.yml`).

---

### 3. `release-build.yml` (new) — release branch CI

**Trigger:** `push` to `release/**`

**Build steps:** identical to the current `build.yml` release build:
- Decodes keystore, creates `google-services.json`
- Calculates `VERSION_CODE = VERSION_CODE_OFFSET + (run_number × 10) + run_attempt`
- Runs `bundleRelease assembleRelease test validateDebugScreenshotTest`
- Uploads AAB, APK, build-info with 400-day retention; test results with 30-day retention

**After successful build — `open-version-bump-pr` job:**

1. Parse version from branch name (`release/1.1.0` → `1.1.0`)
2. Compute next version: bump minor (`1.1.0` → `1.2.0`)
3. **Idempotency check (two guards):**
   - **Guard 1:** if an open PR with head `bump/version-1.2.0` already exists → exit silently (PR not yet merged)
   - **Guard 2:** read `version.properties` from master HEAD; if `masterVersion >= nextVersion` → exit silently (PR already merged)
   - Both guards are needed: Guard 1 prevents duplicate open PRs; Guard 2 prevents a new PR from being opened after the first one merges and master advances
4. Create branch `bump/version-1.2.0` from master HEAD
5. Update `version.properties`: `versionName=1.2.0`
6. Commit, push, open PR to master titled `Bump version to 1.2.0`

**Token:** uses `WORKFLOW_TOKEN` PAT (same as `branch-sync.yml`) so the new PR triggers downstream CI.

**Adjusting for major bumps:** the PR always targets a minor increment. Edit `version.properties` in the PR before merging to override.

---

### 4. `deploy.yml` (new, replaces `release.yml`) — manual Play Store deploy

**Trigger:** `workflow_dispatch`

**Inputs:**

| Input | Required | Default |
|---|---|---|
| `release_branch` | yes | — |
| `release_notes` | no | `"Ajustes e melhorias."` |

**Steps:**
1. Checkout `release_branch`
2. Read version from `version.properties` on that branch
3. **Guard:** verify tag `v{version}` does not already exist — fail fast to prevent double-release
4. Download AAB, APK, build-info artifacts from the latest successful `release-build.yml` run on `release_branch` (via `dawidd6/action-download-artifact`, same action currently used in `release.yml`)
5. Write `whatsnew/whatsnew-pt-BR` from `release_notes` input
6. Generate changelog from `git log {last_tag}..{release_branch}`
7. Upload AAB to Play Store internal track (`r0adkll/upload-google-play`)
8. Create git tag `v{version}` and GitHub release with changelog + APK attached

**Files removed:** `release.yml` is deleted and replaced by `deploy.yml`.

---

## File Changes Summary

| File | Action |
|---|---|
| `.github/workflows/build.yml` | Modified — debug-only build, drop VERSION_CODE_OFFSET |
| `.github/workflows/cut-release.yml` | Created — manual release branch creation |
| `.github/workflows/release-build.yml` | Created — release branch CI + version bump PR job |
| `.github/workflows/deploy.yml` | Created — manual Play Store deploy |
| `.github/workflows/release.yml` | Deleted |

`auto-merge.yml` and `branch-sync.yml` are unchanged.

---

## Release Runbook

1. **Cut release branch**: trigger `cut-release.yml` via GitHub Actions UI (version defaults to `version.properties`)
2. **CI runs automatically**: `release-build.yml` builds the artifact and opens a `bump/version-1.2.0` PR on master
3. **Review and merge the version bump PR** on master
4. **Verify the artifact** in the GitHub Actions run for the release branch
5. **Deploy**: trigger `deploy.yml` with `release_branch = release/1.1.0` and optional release notes
6. **Confirm** on Play Store internal track

---

## Verification

- Trigger `cut-release.yml` → confirm `release/x.y.z` branch is created and `release-build.yml` triggers automatically
- Trigger `cut-release.yml` again for the same version → confirm it fails at the guard step (branch already exists)
- Confirm `release-build.yml` artifacts upload and version bump PR opens on master
- Push a second commit to the release branch before the bump PR merges → confirm no duplicate PR is opened (Guard 1)
- Merge the bump PR, then push a third commit to the release branch → confirm no new PR is opened (Guard 2)
- Push a commit to master → confirm `build.yml` runs debug-only with no VERSION_CODE_OFFSET in logs
- Trigger `deploy.yml` with a valid release branch → confirm Play Store upload, tag creation, GitHub release
- Trigger `deploy.yml` a second time for the same version → confirm it fails at the tag guard step
