#!/usr/bin/env bash
# Verify the current checkout and its prepared offline cache. Historical tags are immutable.
# --warm downloads dependencies and builds both the tutorial and the real fixture first.
set -euo pipefail
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CASE="$REPO/case-studies/spec-driven-petclinic"
cd "$REPO"
case "${1:-}" in
    ""|--warm) ;;
    *) echo "Usage: $0 [--warm]" >&2; exit 2 ;;
esac
stop() { echo "STOP — $*" >&2; exit 1; }
echo "Current-checkout preflight: $(git rev-parse --short HEAD) ($(git branch --show-current))"
[ -z "$(git status --porcelain)" ] || stop "commit or set aside uncommitted changes before presenting."
[ -z "${ANTHROPIC_API_KEY:-}" ] || stop "unset ANTHROPIC_API_KEY for the recorded walkthrough."
mode="${AGENT_JUDGE_TUTORIAL_AGENT:-}"
[ "${mode,,}" != live ] || stop "unset AGENT_JUDGE_TUTORIAL_AGENT=live."
ai_validation="${AGENT_JUDGE_TUTORIAL_AI_VALIDATE:-false}"
[ "${ai_validation,,}" != true ] || stop "disable optional AI validation."
version="$(sed -n 's:.*<agent-judge.version>\(.*\)</agent-judge.version>.*:\1:p' pom.xml)"
[ "$version" = 0.17.0 ] || stop "expected released Agent Judge 0.17.0; found $version."
for module in module-01-build module-02-ears-slice module-03-ears-usecase module-04-rfc2119-rules module-05-investigation; do
    [ -f "$CASE/$module/pom.xml" ] || stop "missing current module $module."
done
if [ "${1:-}" = --warm ]; then
    echo "Preparing dependencies and the real PetClinic build (network access required on first use)."
    ./mvnw -B -ntp install || stop "fundamentals preparation failed."
    ./mvnw -B -ntp -f "$CASE/pom.xml" install || stop "case-study preparation failed."
    ./mvnw -q -f "$CASE/pom.xml" exec:java -pl module-02-ears-slice || stop "demo preparation failed."
fi
# MAVEN_ARGS reaches nested Maven builds and fixture materialization as well as this invocation.
export MAVEN_ARGS="${MAVEN_ARGS:-} -o"
echo "Checking the complete case-study regression suite with Maven offline, including the real build..."
./mvnw -B -ntp -f "$CASE/pom.xml" install || stop "offline verification failed; prepare with --warm first."
echo "READY. Follow DRY-RUN.md Part B; its intentional merge gates are separate from this green suite."
