#!/usr/bin/env bash
#
# Materialize the large spec-driven candidate into a buildable workspace.
#
# The vendored fixture is byte-identical to upstream and stays that way, which
# means it does NOT build: it fails spring-javaformat:validate on one file. That
# is a real finding about the generated code and is recorded in PROVENANCE.md.
#
# This script copies the fixture and applies the formatter so modules 06 to 08
# have something they can compile and test. It reports what it changed rather
# than fixing it silently, because a materialization step that quietly repairs
# its subject is hiding evidence.
#
# Usage:
#   ./materialize-large-candidate.sh [target-dir]

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOURCE="$HERE/appointment-scheduling-spec-with-usecases"
TARGET="${1:-$HERE/build/large-candidate}"

if [ ! -d "$SOURCE" ]; then
  echo "large candidate fixture missing at $SOURCE" >&2
  exit 1
fi

rm -rf "$TARGET"
mkdir -p "$(dirname "$TARGET")"
cp -r "$SOURCE" "$TARGET"
rm -rf "$TARGET/target"

( cd "$TARGET" && ./mvnw -q -B spring-javaformat:apply >/dev/null 2>&1 )

# diff exits non-zero when files differ, which pipefail would treat as failure
CHANGED=$(diff -rq --exclude=target "$SOURCE" "$TARGET" 2>/dev/null | wc -l || true)
if [ "$CHANGED" -gt 0 ]; then
  echo "note: applied spring-javaformat to $CHANGED file(s) the generated code left non-conforming" >&2
  diff -rq --exclude=target "$SOURCE" "$TARGET" 2>/dev/null \
    | sed "s|.*$TARGET/||;s| differ||;s|^|      |" >&2 || true
fi

echo "$TARGET"
