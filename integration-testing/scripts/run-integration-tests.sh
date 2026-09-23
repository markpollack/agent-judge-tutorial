#!/usr/bin/env bash
# Run all eleven fundamentals modules, or the five PetClinic modules.
# --demo / --rest select fundamentals 01–06 / 07–11; --offline uses prepared caches.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$SCRIPT_DIR/../.." && pwd)"
RUNNER="$REPO/integration-testing/RunIntegrationTest.java"
MODE=all
CASE_STUDY=false
JBANG=(jbang)
for arg in "$@"; do
    case "$arg" in
        --case-study) CASE_STUDY=true ;;
        --demo) MODE=demo ;;
        --rest) MODE=rest ;;
        --offline) JBANG+=(--offline); export MAVEN_ARGS="${MAVEN_ARGS:-} -o" ;;
        *) echo "Unknown option: $arg" >&2; exit 2 ;;
    esac
done
DEMO_MODULES=(module-01-oracle-boundary module-02-build-and-tests module-03-coverage-evidence
    module-04-custom-judge module-05-derived-judge module-06-definition-of-done)
REST_MODULES=(module-07-model-backed-judge module-08-jury module-09-error-and-escalation
    module-10-koog-evaluation module-11-langchain4j-evaluation)
if $CASE_STUDY; then
    [ "$MODE" = all ] || { echo "--case-study cannot be combined with --demo/--rest" >&2; exit 2; }
    cd "$REPO/case-studies/spec-driven-petclinic/integration-testing"
    ALL_MODULES=(module-01-build module-02-ears-slice module-03-ears-usecase
        module-04-rfc2119-rules module-05-investigation)
    MODULES=("${ALL_MODULES[@]}")
else
    cd "$REPO/integration-testing"
    ALL_MODULES=("${DEMO_MODULES[@]}" "${REST_MODULES[@]}")
    case "$MODE" in
        all) MODULES=("${ALL_MODULES[@]}") ;;
        demo) MODULES=("${DEMO_MODULES[@]}") ;;
        rest) MODULES=("${REST_MODULES[@]}") ;;
    esac
fi
# A missing config or retired name is a coverage failure, never a skipped success.
"${JBANG[@]}" "$RUNNER" --check-roster "${ALL_MODULES[@]}"
PASSED=0
FAILED=0
for module in "${MODULES[@]}"; do
    if "${JBANG[@]}" "$RUNNER" "$module"; then
        PASSED=$((PASSED + 1))
    else
        FAILED=$((FAILED + 1))
    fi
done
printf '\nPassed: %s  Failed: %s  Skipped: 0\n' "$PASSED" "$FAILED"
[ "$FAILED" -eq 0 ]
