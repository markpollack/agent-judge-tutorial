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
dirty="$(git -C "$REPO" status --porcelain 2>/dev/null | grep -v 'DRY-RUN.md\|dry-run-check.sh' | wc -l | tr -d ' ')"

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
    echo "${bold}${yellow}  READY, but warm up first.${off}"
    echo
    echo "  Copy and run this one line, then re-run this check:"
    echo
    echo "    ( cd $CASE/fixtures/petclinic && ./materialize-large-candidate.sh ) \\"
    echo "      && ( cd $CASE/fixtures/petclinic/build/large-candidate && ./mvnw -o -q test )"
    echo
    exit 0
fi

echo "${bold}${green}  READY. Go to Step 3.${off}"
echo
exit 0
