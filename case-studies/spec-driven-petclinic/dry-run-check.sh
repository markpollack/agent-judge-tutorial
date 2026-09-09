#!/usr/bin/env bash
#
# Preflight for the conference dry run. Answers one question: is this machine
# ready to present? Prints READY or STOP and never changes anything.
#
#   ./case-studies/spec-driven-petclinic/dry-run-check.sh
#
# Run it from anywhere; it locates the repository itself.

set -uo pipefail

REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CASE="$REPO/case-studies/spec-driven-petclinic"
EXPECTED_BRANCH="petclinic-evidence-arc"

# The rehearsed tree is whatever this tag points at, resolved at run time rather than
# hardcoded. A pinned SHA here went stale the first time the demo was improved, and this
# script then told the presenter STOP -- do not present on a tree that was perfectly fine.
# Moving the tag is now the single deliberate act that says "this tree was rehearsed".
EXPECTED_TAG="conference-merge-gate"

bold=$'\e[1m'; red=$'\e[31m'; green=$'\e[32m'; yellow=$'\e[33m'; off=$'\e[0m'

# --warm: do the preparation rather than printing commands for someone to copy at 8am.
if [ "${1:-}" = "--warm" ]; then
    echo
    echo "${bold}Warming the candidate${off}"
    echo "  materializing (deletes and re-copies from the vendored fixture)..."
    ( cd "$CASE/fixtures/petclinic" && ./materialize-large-candidate.sh >/dev/null ) || {
        echo "${red}  materialization failed${off}"; exit 1; }
    echo "  building the candidate (about 40s)..."
    ( cd "$CASE/fixtures/petclinic/build/large-candidate" && ./mvnw -o -q test ) >/dev/null 2>&1 || {
        echo "${red}  candidate build failed${off}"; exit 1; }
    echo "${green}  warm.${off} Re-running the check..."
    exec "${BASH_SOURCE[0]}"
fi
problems=()
warnings=()

check() { # name, actual, expected
    if [ "$2" = "$3" ]; then
        printf "  %-22s %s\n" "$1" "${green}$2${off}"
    else
        printf "  %-22s %s   ${red}expected: %s (tag %s)${off}\n" "$1" "${red}$2${off}" "$3" "$EXPECTED_TAG"
        problems+=("$1")
    fi
}

echo
echo "${bold}Conference dry-run preflight${off}"
echo "  $REPO"
echo

branch="$(git -C "$REPO" branch --show-current 2>/dev/null)"
head="$(git -C "$REPO" rev-parse --short HEAD 2>/dev/null)"
expected_head="$(git -C "$REPO" rev-parse --short "$EXPECTED_TAG" 2>/dev/null)"

if [ -z "$expected_head" ]; then
    printf "  %-22s %s\n" "rehearsed tag" "${red}$EXPECTED_TAG not found${off}"
    echo
    echo "${bold}${red}  STOP — cannot tell whether this tree was rehearsed.${off}"
    echo "  Hand this output over. Do not try to fix it yourself."
    echo
    exit 1
fi
# No exclusions. These files were untracked when this check was written and were skipped
# for that reason; they are tracked now, and an exclusion list in a pre-flight check is a
# way to be told everything is fine while something is not.
dirty="$(git -C "$REPO" status --porcelain 2>/dev/null | wc -l | tr -d ' ')"

check "branch"          "$branch"                     "$EXPECTED_BRANCH"
check "commit"          "$head"                       "$expected_head"
check "uncommitted"     "$dirty file(s)"              "0 file(s)"
check "ANTHROPIC_API_KEY" "${ANTHROPIC_API_KEY:+SET}${ANTHROPIC_API_KEY:-unset}" "unset"

if [ -d "$CASE/fixtures/petclinic/build/large-candidate" ]; then
    printf "  %-22s %s\n" "candidate" "${green}materialized${off}"
else
    printf "  %-22s %s\n" "candidate" "${yellow}not materialized${off}"
    warnings+=("candidate")
fi

if [ -d "$CASE/fixtures/petclinic/build/large-candidate/target/classes" ]; then
    printf "  %-22s %s\n" "build" "${green}warm${off}"
else
    printf "  %-22s %s\n" "build" "${yellow}cold — module 01 will be slower${off}"
    warnings+=("warm")
fi

echo
if [ ${#problems[@]} -gt 0 ]; then
    echo "${bold}${red}  STOP — do not present.${off}"
    echo "  Wrong: ${problems[*]}"
    echo
    echo "  Hand this output over. Do not try to fix it yourself."
    echo
    exit 1
fi

if [ ${#warnings[@]} -gt 0 ]; then
    echo "${bold}${yellow}  NOT READY — the candidate needs warming.${off}"
    echo
    echo "  Run this same script with --warm and it will do it for you (about a minute):"
    echo
    echo "    ./case-studies/spec-driven-petclinic/dry-run-check.sh --warm"
    echo
    exit 0
fi

# Everything else is right, so the last question is whether it actually builds offline.
printf "  %-22s " "offline build"
if (cd "$REPO" && ./mvnw -o -q -f "$CASE/pom.xml" install -DskipTests) >/dev/null 2>&1; then
    echo "${green}OK${off}"
else
    echo "${red}FAILED${off}"
    echo
    echo "${bold}${red}  STOP — do not present.${off}"
    echo "  The case study does not build offline. Hand this output over."
    echo
    exit 1
fi

echo
echo "${bold}${green}  READY.${off}  Open IntelliJ and follow DRY-RUN.md Part B."
echo
exit 0
