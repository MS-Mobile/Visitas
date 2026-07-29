---
name: release-pipeline
description: Use when cutting a release, deploying to Play, bumping version codes, or opening a PR against a release/** branch
---

# Releasing

Three workflows, run in this order. Read the YAML for what each step does; this covers the order,
the manual dispatches, and the rules that live in no single file.

1. **`cut-release.yml`** — manual. Creates `release/X.Y.Z` from master, **via the GitHub API rather
   than a plain git push**, because a pushed branch would not trigger the downstream workflows.
   Prefer the version-matched path: the optional `version_name` input creates `release/<input>`
   *without* updating `version.properties` on the branch, so the branch-name guard then fails it.
2. **`release-build.yml`** — automatic on push to `release/**`. Build only: signed AAB/APK plus
   tests, artifacts retained 400 days (`build-info` carries `version-code.txt` and `commit-sha.txt`),
   and an idempotent next-minor version-bump PR opened on master. It deliberately does **not** tag —
   per-build tags previously spammed a tag and a Release on every push.
3. **`deploy.yml`** — manual, takes a `release_branch` input. Pulls the latest successful
   release-build artifacts from that branch, uploads to the Play internal track, then tags the built
   commit `v{versionName}` and creates the GitHub Release. It refuses to run if the tag or Release
   already exists, or if the built SHA is not an ancestor of the release branch. The changelog is
   generated at deploy time from commits since the previous tag.

`versionName` comes from `version.properties`, grepped from the **checked-out branch** — so a branch
whose name and `version.properties` disagree fails the guards in steps 2 and 3.

## A PR targeting a release branch must be cut from that release branch

Not from master. If it is cut from master, the squash-merge silently carries master's whole diff —
including version bumps — onto the release branch. This is the most damaging mistake available here,
and the guards only catch the branch-name half of it.

## Backmerge lands as a merge commit, never a squash

`backmerge.yml` (push to `release/**`) builds `backmerge/<version>` from the release head, pre-merges
master, and opens an idempotent PR back to master — green if clean, a ⚠️ PR listing the conflicting
files if not. **Merge-commit it.** Squashing loses the release branch's ancestry, and the next
backmerge re-proposes the same commits.

Because push-triggered workflows run from the *pushed branch's* copy of the workflow file, a release
branch inherits these workflows from master at cut time — fixing a workflow on master does not
retroactively apply to an already-cut release branch.

## Version codes

`VERSION_CODE = VERSION_CODE_OFFSET (repo Actions variable) + run_number*10 + run_attempt`. The trap
— `run_number` resets when the workflow file is renamed, which has already caused a Play rejection —
is documented at the calculation itself in `release-build.yml`.
