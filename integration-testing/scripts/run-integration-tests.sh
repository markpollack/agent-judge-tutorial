#!/bin/bash
#
# Run all integration tests for Agent Judge Tutorial
#
# Usage:
#   ./scripts/run-integration-tests.sh              # Run all tests
#   ./scripts/run-integration-tests.sh --demo       # Run only the live demo path (01-06)
#   ./scripts/run-integration-tests.sh --rest       # Run everything else (07-11)
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

PASSED=0
FAILED=0
SKIPPED=0

# Parse arguments
RUN_DEMO=true
RUN_REST=true

if [ "$1" == "--demo" ]; then
    RUN_REST=false
    echo -e "${YELLOW}Running only the live demo path${NC}"
elif [ "$1" == "--rest" ]; then
    RUN_DEMO=false
    echo -e "${YELLOW}Running only the modules outside the demo path${NC}"
fi

echo "================================================================"
echo "   Agent Judge Tutorial - Integration Test Suite"
echo "================================================================"
echo ""

# The conference path: the oracle boundary through the definition of done.
DEMO_MODULES=(
    "module-01-build-judge"
    "module-02-coverage-evidence"
    "module-03-ai-architecture-judge"
    "module-04-agentic-architecture-judge"
    "module-01-oracle-boundary"
    "module-02-build-and-tests"
    "module-03-coverage-evidence"
    "module-04-custom-judge"
    "module-05-derived-judge"
    "module-06-definition-of-done"
)

# The learning path: everything else, in sequence.
REST_MODULES=(
    "module-07-model-backed-judge"
    "module-08-jury"
    "module-09-error-and-escalation"
    "module-10-koog-evaluation"
    "module-11-langchain4j-evaluation"
)

run_test() {
    local module=$1
    echo ""
    echo "----------------------------------------------------------------"
    echo "Running: $module"
    echo "----------------------------------------------------------------"

    if jbang RunIntegrationTest.java "$module"; then
        echo -e "${GREEN}PASSED: $module${NC}"
        ((++PASSED))
    else
        echo -e "${RED}FAILED: $module${NC}"
        ((++FAILED))
    fi
}

if [ "$RUN_DEMO" == "true" ]; then
    echo ""
    echo "Live demo path (no API key required)"
    echo "--------------------------------------------"
    for module in "${DEMO_MODULES[@]}"; do
        if [ -f "configs/${module}.json" ]; then
            run_test "$module"
        else
            echo -e "${YELLOW}Skipping $module (no config)${NC}"
            ((++SKIPPED))
        fi
    done
fi

if [ "$RUN_REST" == "true" ]; then
    echo ""
    echo "Learning path (no API key required)"
    echo "------------------"
    for module in "${REST_MODULES[@]}"; do
        if [ -f "configs/${module}.json" ]; then
            run_test "$module"
        else
            echo -e "${YELLOW}Skipping $module (no config)${NC}"
            ((++SKIPPED))
        fi
    done
fi

echo ""
echo "================================================================"
echo "   Test Summary"
echo "================================================================"
echo -e "   ${GREEN}Passed:${NC}  $PASSED"
echo -e "   ${RED}Failed:${NC}  $FAILED"
echo -e "   ${YELLOW}Skipped:${NC} $SKIPPED"
echo ""

if [ $FAILED -gt 0 ]; then
    echo -e "${RED}Some tests failed${NC}"
    exit 1
else
    echo -e "${GREEN}All tests passed!${NC}"
    exit 0
fi
