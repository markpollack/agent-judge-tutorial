# Configured Agent Eval walkthrough

Use Java 21 and the tutorial Maven wrapper. This separate reactor uses Agent Eval source
`0d0abe47b34c0e638e68ed81c5a764d6b424bc86` at `0.18.0-SNAPSHOT`; the root fundamentals
reactor still uses released 0.17.0. No published snapshot or AgentWorks BOM entry is assumed.

Create an isolated repository and build the exact source there. The first preparation may download
Maven dependencies; later offline execution resolves only these installed producer bytes.

```bash
export EVAL_M2="$PWD/eval-m2"
git clone https://github.com/markpollack/agent-judge.git /tmp/agent-eval-source
git -C /tmp/agent-eval-source checkout --detach 0d0abe47b34c0e638e68ed81c5a764d6b424bc86
(cd /tmp/agent-eval-source && ./mvnw -Dmaven.repo.local="$EVAL_M2" \
  -pl agent-judge-ai-core,agent-judge-agent-client,agent-judge-exec,agent-judge-assertj \
  -am -DskipTests clean install)
```

`-DskipTests` builds artifacts, without claiming fresh producer acceptance verification.
Keep the same Maven repository when running the tutorial. From the tutorial root:

```bash
unset AGENT_JUDGE_TUTORIAL_AGENT ANTHROPIC_API_KEY
export MAVEN_ARGS="-o -Dmaven.repo.local=$EVAL_M2"
./mvnw install
./mvnw -f case-studies/spec-driven-petclinic/pom.xml clean install
./integration-testing/scripts/run-integration-tests.sh --case-study --offline
```

The root install supplies the tutorial parent. Module 01 performs a real build of a materialized
copy; vendored subjects and all historical recording bytes stay unchanged. Modules 02–05 replay
archived responses. Fresh replay verifies the public caller and retention, not evaluator accuracy.
If Maven plugins or child-build dependencies are missing, prepare them online before adding `-o`.

| Demo | Selected roster | Recorded outcomes | Verdict conclusion | Backend executions |
|---|---|---|---|---|
| `EarsSliceDemo` | UC6-AC7 through UC6-AC12 | 6 PASS | PASS | 1 |
| `EarsUseCaseDemo` | Complete UC6 | 51 PASS, UC6-AC41 ABSTAIN | INCONCLUSIVE | 1 |
| `Rfc2119RulesDemo` | Architectural MUSTs | 5 PASS, 8 FAIL | FAIL | 1 |

Each `.jury(runtime)` configures `EarsJury` or `Rfc2119Jury` from the actual source document
and revision `petclinic:fc9df4af`. `.build().vote()` enters the investigative backend once for
the whole selected roster; an actual agent may make several internal model/tool calls.
Workspace, tools, permissions and native timeout belong to `JudgeBackends`/the backend, not a
`judge(context)` argument. Recorded replay exercises that protocol without new investigation.

The result is a complete `Verdict`: roster, one child attempt per requirement, each actual
requirement association, original judgments and native facts. One shared root Invocation owns
usage; child results reference its ID. Original recording usage is unknown and remains absent.
`RosterResults` supplies a display-only view; storage, policy and assertions retain the whole Verdict.

`ConfiguredExamples` demonstrates prepared RFC/EARS singles and structured typed runtimes with
actual evidence. Structured rosters execute per item: 6, 52 or 13 executions. The conformance tests
verify those counts, actual associations, native refusal with own Checks and lower/item invocation
owners, cancellation, protocol errors and zero-call unsupported-mode preflight.

## AssertJ and policy

Use `io.github.markpollack.judge.assertj.Assertions.assertThat` from `agent-judge-assertj`.
A Requirement-first assertion supplies the actual requirement to construction:

```java
assertThat(requirement).judgedBy(Rfc2119Judge.builder().runtime(runtime))
    .withEvidence(capturedEvidence).isSatisfied();
```

A ready roster uses `assertThat(jury).isPassed()` or `.evaluate()`.
A live stage caches the complete EvaluationResult or thrown failure across terminals.
`.withPolicy(wholeVerdict -> decision)` decides application reliance once and retains the original
Verdict. ABSTAIN/ESCALATE actions do not rewrite its conclusion. The regression tests deliberately
withhold reliance even on a passing slice, while preserving PASS in the stored Verdict.

The three `ShouldIMerge*Demo` classes are explicit subject gates, excluded from ordinary `*Test`
discovery. Rehearse separately:

```bash
./mvnw -f case-studies/spec-driven-petclinic/pom.xml -pl module-02-ears-slice -Dtest=ShouldIMergeSliceDemo test
./mvnw -f case-studies/spec-driven-petclinic/pom.xml -pl module-03-ears-usecase -Dtest=ShouldIMergeBehaviorDemo test
./mvnw -f case-studies/spec-driven-petclinic/pom.xml -pl module-04-rfc2119-rules -Dtest=ShouldIMergeArchitectureDemo test
```

The slice passes. The complete behavioral and architectural gates intentionally fail on
INCONCLUSIVE and FAIL. Regression tests assert the recorded populations and pass.

`NativeRequirementCodecs.codec()` writes and reads V6 Verdict/EvaluationResult values through
`agent-judge-json-jackson2`. Reopened results, reports, and retained AssertJ assertions execute
no producers or policies. `requireUsable()` returns the same validated object.
Current typed readers refuse V2–V5; preserve historical bytes and use the appropriate archival reader.
Preservation limits carry complete originals for caller-owned alternate archival storage.

Module 05 still consumes the actual RULE-4 failed result and runs one separate investigation.
Its archived CONFIRMED/REACHABLE answer is not a newly executed subject proof. Module 06 remains
unimplemented. [DRY-RUN.md](DRY-RUN.md) is the historical 0.17 conference transcript.
