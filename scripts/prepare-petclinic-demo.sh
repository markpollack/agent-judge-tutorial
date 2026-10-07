#!/usr/bin/env bash
# Prepare the pinned source, callers, plugins and nested build for offline replay.
set -euo pipefail
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO"
STATE="$REPO/.petclinic-demo"
PIN=b7d2d88ace7cbf110dbfd47ab57221d9e364fb7e
export EVAL_M2="${EVAL_M2:-$STATE/m2}"
SOURCE="${EVAL_SOURCE:-$STATE/agent-judge}"
mkdir -p "$STATE" "$EVAL_M2"
EVAL_M2="$(cd "$EVAL_M2" && pwd)"
export EVAL_M2
if [[ "$EVAL_M2" =~ [[:space:]] ]]; then
    echo 'Use a Maven repository path without whitespace (MAVEN_ARGS is shared with child builds).' >&2
    exit 2
fi
if [ ! -d "$SOURCE" ]; then
    git clone --no-checkout https://github.com/markpollack/agent-judge.git "$SOURCE"
    git -C "$SOURCE" checkout --detach "$PIN"
fi
test "$(git -C "$SOURCE" rev-parse HEAD)" = "$PIN"
test -z "$(git -C "$SOURCE" status --porcelain --untracked-files=no)"
# Explicit replay selection overrides an inherited live setting. No capture is performed.
export AGENT_JUDGE_TUTORIAL_AGENT=recorded
unset AGENT_JUDGE_TUTORIAL_CAPTURE AGENT_JUDGE_TUTORIAL_AI_VALIDATE
export MAVEN_ARGS="-B -ntp -nsu -Dmaven.repo.local=$EVAL_M2"
(cd "$SOURCE" && ./mvnw -B -ntp -Dmaven.repo.local="$EVAL_M2" -pl agent-judge-ai-core,agent-judge-agent-client,agent-judge-exec,agent-judge-assertj \
    -am -DskipTests clean install)
./mvnw -Dmaven.repo.local="$EVAL_M2" clean install
./mvnw -Dmaven.repo.local="$EVAL_M2" -f case-studies/spec-driven-petclinic/pom.xml clean install
FIXTURES="$REPO/case-studies/spec-driven-petclinic/fixtures/petclinic"
if [ ! -d "$FIXTURES/build/large-candidate" ]; then
    "$FIXTURES/materialize-large-candidate.sh"
fi
# Module 01's ordinary regression has prepared the materialized child build dependencies.
./integration-testing/scripts/run-integration-tests.sh --case-study
printf '%s\n' "$PIN" > "$STATE/producer-commit.txt"
python3 - "$EVAL_M2" "$STATE/artifacts.sha256" <<'PY'
import hashlib, pathlib, sys
repo, manifest = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
files = sorted(repo.glob('io/github/markpollack/agent-judge-*/0.18.0-SNAPSHOT/*.jar'))
files += sorted(repo.glob('io/github/markpollack/agent-judge-*/0.18.0-SNAPSHOT/*.pom'))
assert files, 'No installed producer artifacts'
manifest.write_text(''.join(f'{hashlib.sha256(p.read_bytes()).hexdigest()}  {p}\n' for p in files))
PY
printf '\nPrepared replay. EVAL_M2=%s\nArtifact identities: %s/artifacts.sha256\n' "$EVAL_M2" "$STATE"
