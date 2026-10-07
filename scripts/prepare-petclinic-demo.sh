#!/usr/bin/env bash
# Install the complete pinned reactor, callers, plugins and nested build for offline replay.
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/petclinic-demo-env.sh"
cd "$REPO"
mkdir -p "$STATE" "$EVAL_M2"
if [ ! -d "$SOURCE" ]; then
    if "$ISOLATED"; then
        git clone --no-checkout https://github.com/markpollack/agent-judge.git "$SOURCE"
        git -C "$SOURCE" checkout --detach "$PIN"
    else
        echo "Missing producer checkout: $SOURCE. Clone Agent Judge beside this tutorial, or set EVAL_SOURCE." >&2
        exit 2
    fi
fi
if [ "$(git -C "$SOURCE" rev-parse HEAD)" != "$PIN" ] ||
        [ -n "$(git -C "$SOURCE" status --porcelain --untracked-files=no)" ]; then
    echo "Preparation requires a clean producer at $PIN; preserve your changes and choose EVAL_SOURCE or --isolated." >&2
    exit 2
fi
JAVA_VERSION="$(java -version 2>&1)"
if [[ "$JAVA_VERSION" != *'version "21.'* ]]; then
    echo 'Select Java 21 (JAVA_HOME and PATH) before preparation.' >&2
    exit 2
fi
export MAVEN_ARGS="-B -ntp -nsu -Dmaven.repo.local=$EVAL_M2"
(cd "$SOURCE" && ./mvnw -B -ntp -Dmaven.repo.local="$EVAL_M2" clean install)
./mvnw -Dmaven.repo.local="$EVAL_M2" clean install
FIXTURES="$REPO/case-studies/spec-driven-petclinic/fixtures/petclinic"
if [ ! -d "$FIXTURES/build/large-candidate" ]; then
    "$FIXTURES/materialize-large-candidate.sh"
fi
# Remove stale compiled tests/reports from this derived workspace before the real build.
(cd "$FIXTURES/build/large-candidate" && ./mvnw -Dmaven.repo.local="$EVAL_M2" clean)
./mvnw -Dmaven.repo.local="$EVAL_M2" -f case-studies/spec-driven-petclinic/pom.xml clean install
# Module 01's ordinary regression has prepared the materialized child build dependencies.
./integration-testing/scripts/run-integration-tests.sh --case-study
printf '%s\n' "$PIN" > "$STATE/producer-commit.txt"
printf '%s\n' "$EVAL_M2" > "$STATE/repository.txt"
python3 - "$EVAL_M2" "$STATE/artifacts.sha256" <<'PY'
import hashlib, pathlib, sys
repo, manifest = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
files = sorted(repo.glob('io/github/markpollack/agent-judge-*/0.18.0-SNAPSHOT/*.jar'))
files += sorted(repo.glob('io/github/markpollack/agent-judge-*/0.18.0-SNAPSHOT/*.pom'))
assert files, 'No installed producer artifacts'
manifest.write_text(''.join(f'{hashlib.sha256(p.read_bytes()).hexdigest()}  {p}\n' for p in files))
PY
printf '\nPrepared replay. EVAL_M2=%s\nArtifact identities: %s/artifacts.sha256\n' "$EVAL_M2" "$STATE"
