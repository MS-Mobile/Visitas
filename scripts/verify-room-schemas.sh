#!/usr/bin/env sh
#
# Verify that the committed Room schema JSONs under app/schemas match the
# ones the current sources produce.
#
# Room exports a schema JSON per database version at *compile* time — the
# androidx.room Gradle plugin copies them from the KSP output into the
# `schemaDirectory` configured in app/build.gradle.kts. No device, emulator
# or app launch is involved, so a plain `./gradlew :app:assembleDebug` on a
# CI runner regenerates them just as a local build does.
#
# Run this AFTER a debug build in the same workspace. It fails when:
#   1. no schema exists for the @Database version declared in VisitasDatabase.kt
#      (i.e. the build did not export schemas at all — which would make the
#      diff check below vacuously green), or
#   2. the build changed or added anything under app/schemas, meaning the
#      generated schema was never committed.
#
# Usage:  ./gradlew :app:assembleDebug && sh scripts/verify-room-schemas.sh

set -eu

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

# 1. A schema for the declared version must have been exported.
missing=1
for schema_dir in "$SCHEMA_ROOT"/*; do
	[ -d "$schema_dir" ] || continue
	if [ -f "$schema_dir/$DB_VERSION.json" ]; then
		echo "  + $schema_dir/$DB_VERSION.json"
		missing=0
	fi
done

if [ "$missing" -ne 0 ]; then
	cat >&2 <<EOF
error: no schema JSON for database version $DB_VERSION under $SCHEMA_ROOT/.

The build should have exported it. Check that app/build.gradle.kts still has
the androidx.room plugin applied with room { schemaDirectory(...) }, and that
this script runs after a debug build in the same workspace.
EOF
	exit 1
fi

# 2. The build must not have changed or added anything under app/schemas.
#    --porcelain also reports untracked files, so a brand-new N.json that was
#    never committed is caught too.
if [ -n "$(git status --porcelain -- "$SCHEMA_ROOT")" ]; then
	echo >&2
	echo "error: the build regenerated Room schemas that differ from the committed ones:" >&2
	git status --short -- "$SCHEMA_ROOT" >&2
	echo >&2
	git --no-pager diff -- "$SCHEMA_ROOT" >&2
	cat >&2 <<'EOF'

Commit the regenerated schemas. Either build locally and commit the result:

  ./gradlew :app:assembleDebug && git add app/schemas && git commit

or dispatch the "Regenerate Room Schemas" workflow
(.github/workflows/regenerate-room-schemas.yml) for this branch and let it
commit them for you.
EOF
	exit 1
fi

echo "Room schemas are up to date."
