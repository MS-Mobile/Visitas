#!/usr/bin/env sh
#
# Regenerate docs/screenshots/ (the README gallery) from the committed
# Compose screenshot-test reference renders.
#
# The reference renders live under
#   app/src/screenshotTestDebug/reference/com/msmobile/visitas/<pkg>/<Test>/
# with generated filenames of the form
#   <Method>$app_Phone_<themeHash>_<contentHash>_<index>.png
# where <themeHash> encodes the device/theme config (266ff7ee == light) and
# <index> is the position of the preview in its PreviewParameterProvider.
#
# We curate a handful of light-theme screens into readable filenames so the
# README isn't coupled to those generated paths. Regenerate the references
# first with:  ./gradlew :app:updateDebugScreenshotTest
#
# The (dir, index) pairs below map to the providers in:
#   VisitListPreviewConfigProvider.kt / VisitDetailPreviewConfigProvider.kt
# If you reorder those providers, update the indices here to match.

set -eu

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"

REF_ROOT="app/src/screenshotTestDebug/reference/com/msmobile/visitas"
DEST="docs/screenshots"
LIGHT_THEME="266ff7ee" # light-theme device-config hash in reference filenames

if [ ! -d "$REF_ROOT" ]; then
	echo "error: $REF_ROOT not found — run './gradlew :app:updateDebugScreenshotTest' first" >&2
	exit 1
fi

mkdir -p "$DEST"

# dest filename | reference test dir (under REF_ROOT) | preview index
status=0
while IFS='|' read -r name dir index; do
	[ -z "$name" ] && continue
	case "$name" in \#*) continue ;; esac

	# shellcheck disable=SC2086
	set -- "$REF_ROOT/$dir"/*_"${LIGHT_THEME}"_*_"${index}".png
	if [ "$#" -ne 1 ] || [ ! -f "$1" ]; then
		echo "  x $name: expected exactly one reference for '$dir' index $index, found $#" >&2
		status=1
		continue
	fi

	cp "$1" "$DEST/$name"
	echo "  + $name"
done <<'MANIFEST'
visit-list.png|visit/VisitListScreenshotTest|0
visit-list-address-options.png|visit/VisitListScreenshotTest|9
visit-detail.png|visit/VisitDetailScreenshotTest|0
visit-phone-options.png|visit/VisitDetailScreenshotTest|14
conversation-list.png|conversation/ConversationListScreenshotTest|0
conversation-detail.png|conversation/ConversationDetailScreenshotTest|0
settings.png|settings/SettingsScreenshotTest|0
MANIFEST

if [ "$status" -ne 0 ]; then
	echo "sync-readme-screenshots: one or more references could not be resolved" >&2
	exit "$status"
fi

echo "docs/screenshots is in sync."
