#!/usr/bin/env bash
#
# Materialize the small city-search candidate workspace from the pinned baseline.
#
# Deterministic and offline: copies fixtures/petclinic/baseline and applies
# city-search.patch. Nothing is fetched. The baseline itself is never modified.
#
# Usage:
#   ./materialize-city-search.sh [target-dir]
#
# Default target: build/city-search-candidate under this fixtures directory.

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET="${1:-$HERE/build/city-search-candidate}"

if [ ! -d "$HERE/baseline" ]; then
  echo "baseline fixture missing at $HERE/baseline" >&2
  exit 1
fi
if [ ! -f "$HERE/city-search.patch" ]; then
  echo "city-search.patch missing at $HERE/city-search.patch" >&2
  exit 1
fi

rm -rf "$TARGET"
mkdir -p "$(dirname "$TARGET")"
cp -r "$HERE/baseline" "$TARGET"
rm -rf "$TARGET/target"

# -p1 strips the leading baseline/ or candidate/ path component
patch -s -p1 -d "$TARGET" < "$HERE/city-search.patch"

echo "$TARGET"
