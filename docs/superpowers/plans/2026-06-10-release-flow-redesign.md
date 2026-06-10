# Release Flow Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the monolithic `release.yml` with four focused workflows: master CI (debug-only), manual release branch creation, release branch CI (with Play Store artifacts + version bump PR), and a manual deploy.

**Architecture:** `pull-request-build.yml` handles master CI with no version code or signing concerns. `cut-release.yml` is a `workflow_dispatch` that creates the `release/x.y.z` branch from master. `release-build.yml` handles release branch CI with the full version code + signing setup, and runs an idempotent version bump PR job on master after a successful build. `deploy.yml` is the manual Play Store deploy, replacing `release.yml`, downloading artifacts from a specified release branch.

**Tech Stack:** GitHub Actions, `dawidd6/action-download-artifact@v6`, `r0adkll/upload-google-play@v1`, `softprops/action-gh-release@v2`, `gh` CLI, `WORKFLOW_TOKEN` PAT (already used by `branch-sync.yml`)

---

### Task 1: Simplify `pull-request-build.yml` to debug-only master CI

**Files:**
- Modify: `.github/workflows/build.yml`

The keystore decode, version code calculation, and release build tasks are removed. Only a debug build and tests remain. `GOOGLE_SERVICES_RELEASE` and the three release artifact uploads are dropped.

- [ ] **Step 1: Rewrite `pull-request-build.yml`**

Replace the entire file with:

```yaml
# Android Build Pipeline — master CI (debug builds + tests only)
# Release builds run in release-build.yml on release/** branches

name: Build

on:
  push:
    branches:
      - master
  pull_request:
    branches:
      - master

concurrency:
  group: build-${{ github.ref }}
  cancel-in-progress: true

jobs:
  build:
    name: Build Android App
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Create Google Services JSON
        env:
          GOOGLE_SERVICES_DEBUG: ${{ secrets.GOOGLE_SERVICES_JSON_DEBUG }}
        run: |
          mkdir -p app/src/debug
          printf '%s' "$GOOGLE_SERVICES_DEBUG" > app/src/debug/google-services.json
          if [ ! -f "app/src/debug/google-services.json" ]; then
            echo "ERROR: google-services.json was not created"
            exit 1
          fi
          echo "Google Services JSON created successfully"

      - name: Make gradlew executable
        run: chmod +x gradlew

      - name: Calculate week number
        id: week
        run: echo "number=$(date +%V)" >> $GITHUB_OUTPUT

      - name: Cache Gradle dependencies
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: gradle-${{ runner.os }}-week-${{ steps.week.outputs.number }}-${{ hashFiles('gradle/libs.versions.toml') }}
          restore-keys: |
            gradle-${{ runner.os }}-
            gradle-

      - name: Build Debug and Run Tests
        run: ./gradlew assembleDebug test validateDebugScreenshotTest
        env:
          ENCRYPTION_PASSPHRASE: ${{ secrets.ENCRYPTION_PASSPHRASE }}
          SENTRY_DSN: ${{ secrets.SENTRY_DSN }}

      - name: Upload Test Results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: test-results
          path: '**/build/test-results/**/*.xml'

      - name: Upload Screenshot Test Results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: screenshot-test-results
          path: '**/build/reports/screenshotTest/**'
```

- [ ] **Step 2: Validate YAML syntax**

```bash
actionlint .github/workflows/build.yml
```

Expected: no output (no errors).

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/build.yml
git commit -m "ci: simplify build.yml to debug-only master CI"
```

---

### Task 2: Create `cut-release.yml`

**Files:**
- Create: `.github/workflows/cut-release.yml`

A `workflow_dispatch` that reads the current version from `version.properties` on master (or accepts an override), guards against an already-existing release branch, then creates and pushes `release/{version}`. Pushing the branch triggers `release-build.yml` automatically.

- [ ] **Step 1: Create `.github/workflows/cut-release.yml`**

```yaml
# Cut Release — creates a release branch from master
# Pushing the branch automatically triggers release-build.yml

name: Cut Release

on:
  workflow_dispatch:
    inputs:
      version_name:
        description: 'Version to release (e.g. 1.1.0). Defaults to version.properties.'
        required: false
        default: ''

jobs:
  cut:
    name: Cut Release Branch
    runs-on: ubuntu-latest
    permissions:
      contents: write

    steps:
      - name: Checkout master
        uses: actions/checkout@v4
        with:
          ref: master
          token: ${{ secrets.WORKFLOW_TOKEN }}

      - name: Resolve version
        id: version
        run: |
          INPUT="${{ inputs.version_name }}"
          if [ -n "$INPUT" ]; then
            NAME="$INPUT"
          else
            NAME=$(grep '^versionName=' version.properties | cut -d'=' -f2)
          fi
          echo "name=$NAME" >> $GITHUB_OUTPUT
          echo "branch=release/${NAME}" >> $GITHUB_OUTPUT
          echo "Resolved version: $NAME (branch: release/${NAME})"

      - name: Guard — fail if release branch already exists
        run: |
          BRANCH="${{ steps.version.outputs.branch }}"
          if git ls-remote --exit-code --heads origin "$BRANCH" >/dev/null 2>&1; then
            echo "ERROR: Branch $BRANCH already exists on remote."
            exit 1
          fi
          echo "Branch $BRANCH does not exist — safe to proceed"

      - name: Create and push release branch
        run: |
          BRANCH="${{ steps.version.outputs.branch }}"
          git checkout -b "$BRANCH"
          git push origin "$BRANCH"
          echo "Release branch $BRANCH created and pushed."
          echo "release-build.yml will trigger automatically."
```

- [ ] **Step 2: Validate YAML syntax**

```bash
actionlint .github/workflows/cut-release.yml
```

Expected: no output (no errors).

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/cut-release.yml
git commit -m "ci: add cut-release.yml workflow_dispatch to create release branches"
```

---

### Task 4: Create `release-build.yml`

**Files:**
- Create: `.github/workflows/release-build.yml`

Two jobs: `build` (full release build with version code) and `open-version-bump-pr` (runs after build success, idempotent). The bump job uses two guards: Guard 1 checks for an open PR, Guard 2 reads `version.properties` from master to detect a previously-merged bump.

- [ ] **Step 1: Create `.github/workflows/release-build.yml`**

```yaml
# Release Branch CI — full release build with version code + signing
# After a successful build, opens an idempotent version bump PR on master

name: Release Build

on:
  push:
    branches:
      - 'release/**'

concurrency:
  group: release-build-${{ github.ref }}
  cancel-in-progress: true

jobs:
  build:
    name: Build Release
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Decode Keystore
        run: |
          echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > app/keystore.jks
          echo "Keystore file created"

      - name: Validate Keystore
        run: |
          if [ ! -f "app/keystore.jks" ]; then
            echo "Keystore file not found"
            exit 1
          fi
          echo "Keystore file found!"

      - name: Create Google Services JSON
        env:
          GOOGLE_SERVICES_DEBUG: ${{ secrets.GOOGLE_SERVICES_JSON_DEBUG }}
          GOOGLE_SERVICES_RELEASE: ${{ secrets.GOOGLE_SERVICES_JSON_RELEASE }}
        run: |
          mkdir -p app/src/debug app/src/release
          printf '%s' "$GOOGLE_SERVICES_DEBUG" > app/src/debug/google-services.json
          printf '%s' "$GOOGLE_SERVICES_RELEASE" > app/src/release/google-services.json
          if [ ! -f "app/src/debug/google-services.json" ] || [ ! -f "app/src/release/google-services.json" ]; then
            echo "ERROR: google-services.json files were not created"
            exit 1
          fi
          echo "Google Services JSON files created successfully"

      - name: Make gradlew executable
        run: chmod +x gradlew

      - name: Calculate week number
        id: week
        run: echo "number=$(date +%V)" >> $GITHUB_OUTPUT

      - name: Cache Gradle dependencies
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: gradle-${{ runner.os }}-week-${{ steps.week.outputs.number }}-${{ hashFiles('gradle/libs.versions.toml') }}
          restore-keys: |
            gradle-${{ runner.os }}-
            gradle-

      - name: Calculate Version Code
        id: version
        run: |
          if [ -z "$VERSION_CODE_OFFSET" ]; then
            echo "ERROR: VERSION_CODE_OFFSET variable is required but not set"
            echo "Set it at: Settings → Secrets and variables → Actions → Variables"
            exit 1
          fi
          VERSION_CODE=$(( VERSION_CODE_OFFSET + ${{ github.run_number }} * 10 + ${{ github.run_attempt }} ))
          echo "code=${VERSION_CODE}" >> $GITHUB_OUTPUT
          echo "Calculated VERSION_CODE: ${VERSION_CODE} (Base: ${VERSION_CODE_OFFSET}, Run: ${{ github.run_number }}, Attempt: ${{ github.run_attempt }})"
          echo "${VERSION_CODE}" > version-code.txt
        env:
          VERSION_CODE_OFFSET: ${{ vars.VERSION_CODE_OFFSET }}

      - name: Build Release Bundle and Run Tests
        run: ./gradlew bundleRelease assembleRelease test validateDebugScreenshotTest
        env:
          VERSION_CODE: ${{ steps.version.outputs.code }}
          KEYSTORE_FILE: ${{ github.workspace }}/app/keystore.jks
          KEYSTORE_ALIAS: ${{ secrets.KEYSTORE_ALIAS }}
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          ENCRYPTION_PASSPHRASE: ${{ secrets.ENCRYPTION_PASSPHRASE }}
          SENTRY_DSN: ${{ secrets.SENTRY_DSN }}

      - name: Upload Test Results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: test-results
          path: '**/build/test-results/**/*.xml'

      - name: Upload Screenshot Test Results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: screenshot-test-results
          path: '**/build/reports/screenshotTest/**'

      - name: Upload App Bundle
        uses: actions/upload-artifact@v4
        with:
          name: app-bundle
          path: app/build/outputs/bundle/release/*.aab
          retention-days: 400

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: app-apk
          path: app/build/outputs/apk/release/*.apk
          retention-days: 400

      - name: Upload build info
        uses: actions/upload-artifact@v4
        with:
          name: build-info
          path: version-code.txt
          retention-days: 400

  open-version-bump-pr:
    name: Open Version Bump PR on Master
    runs-on: ubuntu-latest
    needs: build

    steps:
      - name: Checkout master
        uses: actions/checkout@v4
        with:
          ref: master
          token: ${{ secrets.WORKFLOW_TOKEN }}
          fetch-depth: 0

      - name: Parse release version and compute next
        id: versions
        run: |
          BRANCH="${{ github.ref_name }}"
          RELEASE_VERSION="${BRANCH#release/}"
          IFS='.' read -r MAJOR MINOR PATCH <<< "$RELEASE_VERSION"
          NEXT_MINOR=$(( MINOR + 1 ))
          NEXT_VERSION="${MAJOR}.${NEXT_MINOR}.0"
          BUMP_BRANCH="bump/version-${NEXT_VERSION}"
          echo "release_version=$RELEASE_VERSION" >> $GITHUB_OUTPUT
          echo "next_version=$NEXT_VERSION" >> $GITHUB_OUTPUT
          echo "bump_branch=$BUMP_BRANCH" >> $GITHUB_OUTPUT
          echo "Release: $RELEASE_VERSION  →  Next: $NEXT_VERSION  (branch: $BUMP_BRANCH)"

      - name: Guard 1 — skip if bump PR already open
        id: guard1
        run: |
          BUMP_BRANCH="${{ steps.versions.outputs.bump_branch }}"
          COUNT=$(gh pr list --head "$BUMP_BRANCH" --state open --json number --jq 'length')
          echo "open_count=$COUNT" >> $GITHUB_OUTPUT
          echo "Open PRs with head $BUMP_BRANCH: $COUNT"
        env:
          GH_TOKEN: ${{ secrets.WORKFLOW_TOKEN }}

      - name: Guard 2 — skip if master already bumped
        id: guard2
        run: |
          NEXT_VERSION="${{ steps.versions.outputs.next_version }}"
          MASTER_VERSION=$(grep '^versionName=' version.properties | cut -d'=' -f2)
          # needs_bump=true only when MASTER_VERSION < NEXT_VERSION
          HIGHER=$(printf '%s\n' "$MASTER_VERSION" "$NEXT_VERSION" | sort -V | tail -1)
          if [ "$MASTER_VERSION" = "$HIGHER" ]; then
            echo "needs_bump=false" >> $GITHUB_OUTPUT
            echo "Master ($MASTER_VERSION) >= next ($NEXT_VERSION) — no bump needed"
          else
            echo "needs_bump=true" >> $GITHUB_OUTPUT
            echo "Master ($MASTER_VERSION) < next ($NEXT_VERSION) — bump needed"
          fi

      - name: Create bump branch and update version.properties
        if: steps.guard1.outputs.open_count == '0' && steps.guard2.outputs.needs_bump == 'true'
        run: |
          BUMP_BRANCH="${{ steps.versions.outputs.bump_branch }}"
          NEXT_VERSION="${{ steps.versions.outputs.next_version }}"
          git config user.name "github-actions[bot]"
          git config user.email "github-actions[bot]@users.noreply.github.com"
          # Clean up any orphaned remote branch from a cancelled prior run
          git push origin --delete "$BUMP_BRANCH" 2>/dev/null || true
          git checkout -b "$BUMP_BRANCH"
          sed -i "s/^versionName=.*/versionName=${NEXT_VERSION}/" version.properties
          git add version.properties
          git commit -m "Bump version to ${NEXT_VERSION}"
          git push origin "$BUMP_BRANCH"

      - name: Open PR to master
        if: steps.guard1.outputs.open_count == '0' && steps.guard2.outputs.needs_bump == 'true'
        run: |
          NEXT_VERSION="${{ steps.versions.outputs.next_version }}"
          BUMP_BRANCH="${{ steps.versions.outputs.bump_branch }}"
          RELEASE_BRANCH="${{ github.ref_name }}"
          BODY="Automated version bump after cutting \`${RELEASE_BRANCH}\`.

To override with a major bump, edit \`version.properties\` in this PR before merging."
          gh pr create \
            --title "Bump version to ${NEXT_VERSION}" \
            --body "$BODY" \
            --base master \
            --head "$BUMP_BRANCH"
        env:
          GH_TOKEN: ${{ secrets.WORKFLOW_TOKEN }}
```

- [ ] **Step 2: Validate YAML syntax**

```bash
actionlint .github/workflows/release-build.yml
```

Expected: no output (no errors).

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/release-build.yml
git commit -m "ci: add release-build.yml for release branch CI and version bump PR"
```

---

### Task 5: Create `deploy.yml`

**Files:**
- Create: `.github/workflows/deploy.yml`

This is the current `release.yml` reshaped: accepts `release_branch` instead of `version_name`, reads version from the branch's `version.properties`, and downloads artifacts from `release-build.yml` on that branch.

- [ ] **Step 1: Create `.github/workflows/deploy.yml`**

```yaml
# Play Store Deploy — manual deployment from a release branch
# Downloads artifacts produced by release-build.yml on the specified branch

name: Deploy

on:
  workflow_dispatch:
    inputs:
      release_branch:
        description: 'Release branch to deploy from (e.g. release/1.1.0)'
        required: true
      release_notes:
        description: 'Release notes (Portuguese)'
        required: false
        default: 'Ajustes e melhorias.'
        type: string

jobs:
  deploy:
    name: Deploy to Play Store
    runs-on: ubuntu-latest
    permissions:
      actions: read
      contents: write

    steps:
      - name: Checkout release branch
        uses: actions/checkout@v4
        with:
          ref: ${{ inputs.release_branch }}
          fetch-depth: 0

      - name: Read version from release branch
        id: version
        run: |
          NAME=$(grep '^versionName=' version.properties | cut -d'=' -f2)
          echo "name=$NAME" >> $GITHUB_OUTPUT
          echo "tag=v$NAME" >> $GITHUB_OUTPUT
          echo "Version: $NAME (tag: v$NAME)"

      - name: Guard — fail if tag already exists
        run: |
          TAG="${{ steps.version.outputs.tag }}"
          if git rev-parse "$TAG" >/dev/null 2>&1; then
            echo "ERROR: Tag $TAG already exists. This version has already been released."
            exit 1
          fi
          echo "Tag $TAG does not exist — safe to proceed"

      - name: Generate changelog
        id: changelog
        run: |
          PREV_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "")
          if [ -n "$PREV_TAG" ]; then
            echo "Generating changelog since $PREV_TAG"
            LOG=$(git log "$PREV_TAG"..HEAD --pretty=format:"- %s" --no-merges)
          else
            echo "No previous tag found — including all commits"
            LOG=$(git log --pretty=format:"- %s" --no-merges)
          fi
          echo "content<<EOF" >> $GITHUB_OUTPUT
          echo "$LOG" >> $GITHUB_OUTPUT
          echo "EOF" >> $GITHUB_OUTPUT

      - name: Download App Bundle from release branch
        id: download
        uses: dawidd6/action-download-artifact@v6
        with:
          name: app-bundle
          path: artifacts
          workflow: release-build.yml
          branch: ${{ inputs.release_branch }}
          workflow_conclusion: success
          github_token: ${{ secrets.GITHUB_TOKEN }}

      - name: Download build info
        uses: dawidd6/action-download-artifact@v6
        with:
          name: build-info
          path: build-info
          workflow: release-build.yml
          run_id: ${{ steps.download.outputs.run-id }}
          github_token: ${{ secrets.GITHUB_TOKEN }}

      - name: Download APK
        uses: dawidd6/action-download-artifact@v6
        with:
          name: app-apk
          path: apk
          workflow: release-build.yml
          run_id: ${{ steps.download.outputs.run-id }}
          github_token: ${{ secrets.GITHUB_TOKEN }}

      - name: Read version code
        id: vcode
        run: |
          CODE=$(cat build-info/version-code.txt)
          echo "code=$CODE" >> $GITHUB_OUTPUT
          echo "Version code: $CODE"

      - name: List artifacts
        run: |
          echo "Downloaded artifacts:"
          find artifacts -name "*.aab" -type f

      - name: Create whatsnew directory
        run: |
          mkdir -p whatsnew
          echo "${{ inputs.release_notes }}" > whatsnew/whatsnew-pt-BR

      - name: Deploy to Play Store (Internal Testing)
        uses: r0adkll/upload-google-play@v1
        with:
          serviceAccountJsonPlainText: ${{ secrets.PLAY_STORE_SERVICE_ACCOUNT_JSON }}
          packageName: com.msmobile.visitas
          releaseFiles: artifacts/*.aab
          track: internal
          status: completed
          releaseName: ${{ inputs.release_notes }}
          whatsNewDirectory: whatsnew
          inAppUpdatePriority: 5

      - name: Create git tag and GitHub Release
        uses: softprops/action-gh-release@v2
        with:
          tag_name: ${{ steps.version.outputs.tag }}
          name: Release ${{ steps.version.outputs.tag }}
          body: |
            ${{ steps.changelog.outputs.content }}

            ---
            **Build:** `${{ steps.vcode.outputs.code }}`
          files: apk/*.apk
```

- [ ] **Step 2: Validate YAML syntax**

```bash
actionlint .github/workflows/deploy.yml
```

Expected: no output (no errors).

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/deploy.yml
git commit -m "ci: add deploy.yml for manual Play Store deployment from release branches"
```

---

### Task 6: Delete `release.yml` and push

**Files:**
- Delete: `.github/workflows/release.yml`

- [ ] **Step 1: Remove `release.yml`**

```bash
git rm .github/workflows/release.yml
```

- [ ] **Step 2: Commit and push**

```bash
git commit -m "ci: remove release.yml (replaced by release-build.yml + deploy.yml)"
git push origin master
```

- [ ] **Step 3: Verify workflows visible on GitHub**

Open the Actions tab on GitHub and confirm:
- "Build" workflow is present (triggered by the push above)
- "Release Build" workflow is present but has no recent runs (no release branch pushed yet)
- "Deploy" workflow is present with a "Run workflow" button
- "Release" workflow (old) is no longer listed

---

### Task 7: End-to-end smoke test

No code changes — this is manual verification of all three workflows.

- [ ] **Step 1: Verify master CI is debug-only**

Open the "Build" run triggered in Task 4 and confirm:
- Steps "Decode Keystore" and "Calculate Version Code" are absent
- Gradle task logged is `assembleDebug test validateDebugScreenshotTest`
- Artifacts uploaded: only `test-results` and `screenshot-test-results` (no `app-bundle`, `app-apk`, `build-info`)

- [ ] **Step 2: Cut a test release branch via `cut-release.yml`**

Trigger `cut-release.yml` from the GitHub Actions UI with `version_name = smoke-test` (or leave blank to use the current `version.properties` value).

Open the Actions tab and confirm:
- `cut-release.yml` creates `release/smoke-test` (or `release/{current-version}`)
- "Release Build" triggers automatically on the new branch. After it completes:
- Steps "Calculate Version Code" and "Build Release Bundle and Run Tests" are present
- Artifacts `app-bundle`, `app-apk`, `build-info` uploaded with 400-day retention
- Job `open-version-bump-pr` runs and opens a PR titled "Bump version to X.Y.0" targeting master

- [ ] **Step 3: Verify Guard 1 — no duplicate PR on second push**

```bash
git commit --allow-empty -m "test: second push"
git push origin release/smoke-test
```

Wait for the "Release Build" run to complete. Confirm: the `open-version-bump-pr` job exits without creating a second PR (Guard 1: open PR already exists).

- [ ] **Step 4: Verify Guard 2 — no new PR after bump merges**

Merge the version bump PR on master. Then:

```bash
git commit --allow-empty -m "test: third push after bump merged"
git push origin release/smoke-test
```

Wait for the run. Confirm: `open-version-bump-pr` job exits silently — no new PR (Guard 2: master version already >= next version).

- [ ] **Step 5: Verify `cut-release.yml` guard**

Trigger `cut-release.yml` again with the same version. Confirm it fails with "Branch already exists on remote."

- [ ] **Step 6: Clean up**

```bash
git push origin --delete release/smoke-test
git checkout master
git branch -d release/smoke-test 2>/dev/null || true
```

If the version bump PR incremented to a version you don't want on master, revert `version.properties` manually and push a follow-up commit.
