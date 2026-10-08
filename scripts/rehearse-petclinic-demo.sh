#!/usr/bin/env bash
# All ordinary regressions and harness demos, plus separately verified instructional failures.
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/petclinic-demo-env.sh"
cd "$REPO"
test "$(cat "$STATE/producer.txt")" = "$(producer_identity)"
test "$(cat "$STATE/repository.txt")" = "$EVAL_M2"
sha256sum --check "$STATE/artifacts.sha256"
export MAVEN_ARGS="-B -ntp -o -Dmaven.repo.local=$EVAL_M2"
CASE=case-studies/spec-driven-petclinic
./mvnw -o -Dmaven.repo.local="$EVAL_M2" clean verify
./mvnw -o -Dmaven.repo.local="$EVAL_M2" -Deval.artifacts="$STATE/artifacts.sha256" -f "$CASE/pom.xml" clean verify
./integration-testing/scripts/run-integration-tests.sh --case-study --offline
./mvnw -o -Dmaven.repo.local="$EVAL_M2" -f "$CASE/pom.xml" -pl module-02-ears-slice \
    -Dtest=ShouldIMergeSliceDemo test

reject() {
    local module="$1" demo="$2" conclusion="$3" started code
    started="$(date +%s)"
    if ./mvnw -o -Dmaven.repo.local="$EVAL_M2" -f "$CASE/pom.xml" -pl "$module" \
            -Dtest="$demo" test > "$STATE/$demo.log" 2>&1; then
        echo "Unexpected passing subject gate: $demo" >&2
        exit 1
    else
        code=$?
    fi
    test "$code" = 1
    python3 - "$CASE/$module/target/surefire-reports" "$demo" "$conclusion" "$started" <<'PY'
import pathlib, sys, xml.etree.ElementTree as ET
directory, demo, conclusion, started = sys.argv[1:]
reports = list(pathlib.Path(directory).glob(f'TEST-*.{demo}.xml'))
assert len(reports) == 1, 'Missing subject assertion report; infrastructure failure'
assert reports[0].stat().st_mtime >= int(started), 'Stale assertion report'
suite = ET.parse(reports[0]).getroot()
assert [suite.get(k) for k in ('tests', 'failures', 'errors')] == ['1', '1', '0'], suite.attrib
failure = suite.find('testcase/failure')
assert failure is not None and 'Assertion' in failure.get('type', ''), 'Not an assertion failure'
assert conclusion in (failure.get('message', '') + (failure.text or '')), 'Wrong rejection conclusion'
print(f'{demo}: expected {conclusion} assertion rejection; 1 failure, 0 infrastructure errors')
PY
}
reject module-03-ears-usecase ShouldIMergeBehaviorDemo INCONCLUSIVE
reject module-04-rfc2119-rules ShouldIMergeArchitectureDemo FAIL
sha256sum --check "$STATE/artifacts.sha256"
echo 'Offline replay rehearsal complete. Zero live inference.'
