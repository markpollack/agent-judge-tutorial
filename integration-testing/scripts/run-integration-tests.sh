#!/bin/bash
#
# Run all integration tests for Agent Judge Tutorial
#
# Usage:
#   ./scripts/run-integration-tests.sh              # Run all tests
#   ./scripts/run-integration-tests.sh --core       # Run only core evaluation (01-05)
#   ./scripts/run-integration-tests.sh --custom     # Run only custom judges (06-08)
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
RUN_CORE=true
RUN_CUSTOM=true

if [ "$1" == "--core" ]; then
    RUN_CUSTOM=false
    echo -e "${YELLOW}Running only core evaluation tests (modules 01-05)${NC}"
elif [ "$1" == "--custom" ]; then
    RUN_CORE=false
    echo -e "${YELLOW}Running only custom judge tests (modules 06-08)${NC}"
fi

echo "================================================================"
echo "   Agent Judge Tutorial - Integration Test Suite"
echo "================================================================"
echo ""

# Core evaluation modules (no API key)
CORE_MODULES=(
    "module-01-single-judge"
    "module-02-build-judge"
    "module-03-composition"
    "module-04-simple-jury"
    "module-05-cascaded-jury"
)

# Custom judge modules (no API key)
CUSTOM_MODULES=(
    "module-06-lambda-judge"
    "module-07-deterministic-judge"
    "module-08-model-backed-judge"
)

run_test() {
    local module=$1
    echo ""
    echo "----------------------------------------------------------------"
    echo "Running: $module"
    echo "----------------------------------------------------------------"

    if jbang RunIntegrationTest.java "$module"; then
        echo -e "${GREEN}PASSED: $module${NC}"
        ((PASSED++))
    else
        echo -e "${RED}FAILED: $module${NC}"
        ((FAILED++))
    fi
}

if [ "$RUN_CORE" == "true" ]; then
    echo ""
    echo "Core Evaluation Tests (no API key required)"
    echo "--------------------------------------------"
    for module in "${CORE_MODULES[@]}"; do
        if [ -f "configs/${module}.json" ]; then
            run_test "$module"
        else
            echo -e "${YELLOW}Skipping $module (no config)${NC}"
            ((SKIPPED++))
        fi
    done
fi

if [ "$RUN_CUSTOM" == "true" ]; then
    echo ""
    echo "Custom Judge Tests"
    echo "------------------"
    for module in "${CUSTOM_MODULES[@]}"; do
        if [ -f "configs/${module}.json" ]; then
            run_test "$module"
        else
            echo -e "${YELLOW}Skipping $module (no config)${NC}"
            ((SKIPPED++))
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
