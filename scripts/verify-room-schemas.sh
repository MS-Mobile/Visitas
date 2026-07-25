#!/usr/bin/env sh
#
# Regenerate the Room schema JSONs under app/schemas and check they match what
# is committed.
#
# Room exports a schema JSON per database version at *compile* time: KSP writes
# them into a build intermediate directory, and the androidx.room Gradle plugin's
# copyRoomSchemas task copies them into the `schemaDirectory` configured in
# app/build.gradle.kts. No device, emulator or app launch is involved.
#
# Why this forces both tasks to re-run instead of piggybacking on a plain build:
# the intermediate directory KSP writes to is not a declared, cacheable output
# of the KSP task, so when kspDebugKotlin is restored FROM-CACHE (the normal case
# on CI, where ~/.gradle is cached) the intermediate stays empty and
# copyRoomSchemas reports NO-SOURCE — the export silently does not happen.
# A check that merely diffed app/schemas after `assembleDebug` would then pass
# without having regenerated anything. `--rerun` ignores both the up-to-date
# check and the build cache for the requested tasks, and the freshness assertion
# below fails loudly if the export still produces nothing.
#
# Usage:
#   sh scripts/verify-room-schemas.sh                 # export, then verify committed
#   sh scripts/verify-room-schemas.sh --export-only   # export only (caller commits)

set -eu

EXPORT_ONLY=0
case "${1:-}" in
	--export-only) EXPORT_ONLY=1 ;;
	'') ;;
	*)
		echo "usage: $0 [--export-only]" >&2
		exit 2
		;;
esac

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"

SCHEMA_ROOT="app/schemas"
DATABASE_SOURCE="app/src/main/java/com/msmobile/visitas/VisitasDatabase.kt"

if [ ! -f "$DATABASE_SOURCE" ]; then
	echo "error: $DATABASE_SOURCE not found" >&2
	exit 1
fi

# The `version = N` line inside the @Database annotation.
DB_VERSION="$(sed -n 's/^[[:space:]]*version[[:space:]]*=[[:space:]]*\([0-9]\{1,\}\).*/\1/p' "$DATABASE_SOURCE" | head -n 1)"
if [ -z "$DB_VERSION" ]; then
	echo "error: could not read the @Database version from $DATABASE_SOURCE" >&2
	exit 1
fi

echo "VisitasDatabase declares version $DB_VERSION"

# Delete the declared version's schema first, so its presence afterwards proves
# the export actually wrote it. Comparing mtimes instead would be racy: `-nt` in
# POSIX sh compares whole seconds, so an export finishing in the same second as
# the reference file reads as "not newer" and the check would flake.
# Deleting the destination also means a stale copyRoomSchemas up-to-date result
# cannot keep the task from running.
for schema_dir in "$SCHEMA_ROOT"/*; do
	[ -d "$schema_dir" ] || continue
	rm -f "$schema_dir/$DB_VERSION.json"
done

# copyRoomSchemas is contributed by the androidx.room plugin. If a Room upgrade
# ever renames it, Gradle fails here with "task not found" — loudly, which is
# the point: this check must never silently degrade into a no-op.
./gradlew :app:kspDebugKotlin :app:copyRoomSchemas --rerun

# 1. The export must have (re)created the declared version's schema.
exported=1
for schema_dir in "$SCHEMA_ROOT"/*; do
	[ -d "$schema_dir" ] || continue
	if [ -f "$schema_dir/$DB_VERSION.json" ]; then
		echo "  + $schema_dir/$DB_VERSION.json (exported by this run)"
		exported=0
	fi
done

if [ "$exported" -ne 0 ]; then
	# Nothing was generated, so restore the file we deleted rather than leaving
	# the working tree damaged. Safe here precisely because the export produced
	# nothing — there is no new schema to preserve.
	git checkout -- "$SCHEMA_ROOT" 2>/dev/null || true
	cat >&2 <<EOF

error: the export produced no schema for database version $DB_VERSION under $SCHEMA_ROOT/.

The Gradle run above did not write one, so this check cannot verify anything.
Look for ':app:copyRoomSchemas NO-SOURCE' in its output. Check that
app/build.gradle.kts still applies the androidx.room plugin with
room { schemaDirectory(...) }, and that both task names above still exist.
EOF
	exit 1
fi

if [ "$EXPORT_ONLY" -eq 1 ]; then
	echo "Room schemas exported."
	exit 0
fi

# 2. The export must not have changed or added anything under app/schemas.
#    --porcelain also reports untracked files, so a brand-new N.json that was
#    never committed is caught too.
if [ -n "$(git status --porcelain -- "$SCHEMA_ROOT")" ]; then
	echo >&2
	echo "error: the exported Room schemas differ from the committed ones:" >&2
	git status --short -- "$SCHEMA_ROOT" >&2
	echo >&2
	git --no-pager diff -- "$SCHEMA_ROOT" >&2
	cat >&2 <<'EOF'

Commit the regenerated schemas. Either export locally and commit the result:

  sh scripts/verify-room-schemas.sh --export-only
  git add app/schemas && git commit

or dispatch the "Regenerate Room Schemas" workflow
(.github/workflows/regenerate-room-schemas.yml) for this branch and let it
commit them for you.
EOF
	exit 1
fi

echo "Room schemas are up to date."
