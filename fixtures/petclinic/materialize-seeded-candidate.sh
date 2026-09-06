#!/usr/bin/env bash
#
# Materialize the large candidate with one seeded defect: the negative control
# for module 06.
#
# A judge that has only ever been shown good code has not been shown to work.
# This produces a workspace that differs from the passing one by a single
# comparison operator, on a boundary the test suite does not test, so the
# expected difference in the verdict is exactly one criterion.
#
# Usage:
#   ./materialize-seeded-candidate.sh [target-dir]

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET="${1:-$HERE/build/seeded-candidate}"
PATCH="$HERE/uc6-ac8-boundary.patch"

"$HERE/materialize-large-candidate.sh" "$TARGET" >/dev/null

BEFORE=$(cd "$TARGET" && git hash-object src/main/java/org/springframework/samples/petclinic/scheduling/service/AppointmentService.java)
( cd "$TARGET" && patch -p1 --batch --forward <"$PATCH" >/dev/null )
AFTER=$(cd "$TARGET" && git hash-object src/main/java/org/springframework/samples/petclinic/scheduling/service/AppointmentService.java)

# A seed that silently did not apply is worse than no seed: the negative control
# then reports a pass and calls it evidence.
if [ "$BEFORE" = "$AFTER" ]; then
  echo "seed did not change AppointmentService.java" >&2
  exit 1
fi

echo "$TARGET"
