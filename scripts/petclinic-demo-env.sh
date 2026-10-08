#!/usr/bin/env bash
# Shared paths for preparation and rehearsal; source from those entry points.
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# Released Agent Eval coordinates io.github.markpollack:agent-judge-*:$RELEASE, from Maven Central.
RELEASE=0.18.0
# Optional source mode builds the producer from its release tag instead of downloading it.
SOURCE_REF="v$RELEASE"
STATE="$REPO/.petclinic-demo"
FROM_SOURCE=false
ISOLATED=false
for argument in "$@"; do
    case "$argument" in
        --source) FROM_SOURCE=true ;;
        --isolated) FROM_SOURCE=true; ISOLATED=true ;;
        --help)
            echo "Usage: $0 [--source | --isolated]"
            echo "Default: released Agent Eval $RELEASE from Maven Central into ~/.m2/repository."
            echo "Optional: --source builds the sibling agent-judge checkout at $SOURCE_REF instead."
            echo 'Optional: --isolated builds a pinned clone into a cache under .petclinic-demo/isolated.'
            echo 'EVAL_SOURCE and EVAL_M2 override source/cache paths in any mode.'
            exit 0 ;;
        *) echo "Unknown option: $argument" >&2; exit 2 ;;
    esac
done
CASE_VERSION="$(sed -n 's:.*<agent-judge.version>\(.*\)</agent-judge.version>.*:\1:p' \
    "$REPO/case-studies/spec-driven-petclinic/pom.xml")"
if [ "$CASE_VERSION" != "$RELEASE" ]; then
    echo "Case-study POM uses Agent Eval $CASE_VERSION; these scripts expect $RELEASE." >&2
    exit 2
fi
if "$ISOLATED"; then
    STATE="$STATE/isolated"
    SOURCE="${EVAL_SOURCE:-$STATE/agent-judge}"
    export EVAL_M2="${EVAL_M2:-$STATE/m2}"
else
    SOURCE="${EVAL_SOURCE:-$REPO/../agent-judge}"
    export EVAL_M2="${EVAL_M2:-$HOME/.m2/repository}"
fi
EVAL_M2="$(python3 - "$EVAL_M2" <<'PY'
import pathlib, sys
print(pathlib.Path(sys.argv[1]).expanduser().resolve())
PY
)"
export EVAL_M2
if [[ "$EVAL_M2" =~ [[:space:]] ]]; then
    echo 'Use a Maven repository path without whitespace (MAVEN_ARGS is shared with child builds).' >&2
    exit 2
fi
# Identity recorded by preparation and checked by rehearsal.
producer_identity() {
    if "$FROM_SOURCE"; then
        echo "source $SOURCE_REF $(git -C "$SOURCE" rev-parse HEAD)"
    else
        echo "release io.github.markpollack:agent-judge-*:$RELEASE"
    fi
}
export AGENT_JUDGE_TUTORIAL_AGENT=recorded
unset AGENT_JUDGE_TUTORIAL_CAPTURE AGENT_JUDGE_TUTORIAL_AI_VALIDATE
