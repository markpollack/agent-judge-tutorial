#!/usr/bin/env bash
# Shared paths for preparation and rehearsal; source from those entry points.
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PIN=b7d2d88ace7cbf110dbfd47ab57221d9e364fb7e
STATE="$REPO/.petclinic-demo"
ISOLATED=false
for argument in "$@"; do
    case "$argument" in
        --isolated) ISOLATED=true ;;
        --help)
            echo "Usage: $0 [--isolated]"
            echo 'Default: sibling agent-judge checkout and ~/.m2/repository.'
            echo 'Optional: --isolated uses a pinned clone and cache under .petclinic-demo/isolated.'
            echo 'EVAL_SOURCE and EVAL_M2 override source/cache paths in either mode.'
            exit 0 ;;
        *) echo "Unknown option: $argument" >&2; exit 2 ;;
    esac
done
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
export AGENT_JUDGE_TUTORIAL_AGENT=recorded
unset AGENT_JUDGE_TUTORIAL_CAPTURE AGENT_JUDGE_TUTORIAL_AI_VALIDATE
