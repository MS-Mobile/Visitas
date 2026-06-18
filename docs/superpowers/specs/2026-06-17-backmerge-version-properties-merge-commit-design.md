# Backmerge: fold version.properties fix into the merge commit

**Date:** 2026-06-17
**Status:** Approved
**Scope:** `.github/workflows/backmerge.yml`

## Problem

`backmerge.yml` protects master's `version.properties` from being downgraded by a
backmerge. It does this with a **standalone** commit:

```sh
git checkout origin/master -- version.properties
git add version.properties
if ! git diff --cached --quiet; then
  git commit -m "Backmerge: keep master's version.properties"
fi
```

That commit lands on the backmerge branch and — because backmerge PRs are merged
with a merge commit (no squash) — is preserved verbatim on master's history. Release
notes are generated with `git log <prev-tag>..<sha> --no-merges` (release-build.yml).
The standalone commit is a **non-merge** commit, so it survives the `--no-merges`
filter and appears in the release notes of every future release cut from master.

## Why the downgrade happens at all (context)

- **Default cut** (no `version_name`): the release branch never touches
  `version.properties`; master is bumped via the bump PR. At backmerge, master is the
  side that changed, so the merge correctly keeps master's value. No fix needed.
- **Patch / `version_name` cut**: `cut-release` writes a `Set release version to X`
  commit on the release branch. Now the release side diverged downward while master is
  unchanged from the merge-base, so git silently adopts the release (lower) value with
  no conflict. This is the case the fix exists for.

The existing post-merge detection (`git diff --cached --quiet`) is already correct
about **when** to apply the fix. The only problem is **how** it records it.

## Design (Approach A)

Keep the existing detection. Change the recording: fold the `version.properties`
restoration **into the merge commit** instead of a standalone commit. `--no-merges`
excludes merge commits, so nothing reaches the changelog.

### No-conflict path

After `git merge origin/master --no-edit` succeeds (a merge commit now exists):

```sh
git checkout origin/master -- version.properties
git add version.properties
if ! git diff --cached --quiet; then
  git commit --amend --no-edit      # fold into the merge commit; parents preserved
fi
```

`git commit --amend` preserves the merge commit's two parents, so it stays a merge
commit (still `--no-merges`-excluded) and keeps its merge message. The existing
`git push -f` publishes the rewritten commit.

### Conflict path

The merge is aborted and a human finishes it on the backmerge branch, producing their
own merge commit. We must **not** add a standalone commit here (it would pollute the
same way). Instead, the conflict-PR body instructs the resolver to restore master's
`version.properties` as part of their merge commit:

```
git fetch origin
git checkout <backmerge-branch>
git merge origin/master                          # resolve conflicts
git checkout origin/master -- version.properties # keep master's version
git commit                                       # finalize the merge commit
git push origin <backmerge-branch>
```

Because the fix lands inside the human's merge commit, it is also `--no-merges`-excluded.

## Non-goals / rejected alternatives

- **Approach B — drop the release's `version.properties` commit before merging.**
  Path-independent, but the target commit is usually buried under hotfix commits at
  backmerge time (a `rebase --onto` that can conflict) and must no-op safely in the
  default-cut case to avoid rewriting history shared with master. More fragile than A
  for no benefit, since A reuses the already-correct detection.

## Verification

- No-conflict, patch cut: backmerge branch's merge commit carries master's
  `version.properties`; no `Backmerge: keep master's version.properties` commit exists;
  `git log <range> --no-merges` does not list any version-keeping commit.
- No-conflict, default cut: detection is a no-op (no diff), merge commit unchanged.
- Conflict path: PR body shows the `git checkout origin/master -- version.properties`
  step; no standalone commit is pushed by the workflow.
- `grep -r "Backmerge: keep master's version.properties"` returns nothing.
