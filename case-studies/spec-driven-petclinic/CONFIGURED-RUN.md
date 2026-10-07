# Configured Agent Eval walkthrough

Use Java 21 and the tutorial Maven wrapper. This separate reactor uses Agent Eval source
`b7d2d88ace7cbf110dbfd47ab57221d9e364fb7e` at `0.18.0-SNAPSHOT`; the root fundamentals
reactor still uses released 0.17.0. No published snapshot or AgentWorks BOM entry is assumed.

From the tutorial root, prepare the exact source, callers, Maven plugins, JBang harness and
materialized child build with one command (Java 21, Python 3 and JBang required):

```bash
./scripts/prepare-petclinic-demo.sh
```

Preparation may download dependencies. It clones the immutable producer into
`.petclinic-demo/agent-judge` and installs into `.petclinic-demo/m2`, both ignored by Git.
`EVAL_SOURCE` may name an existing clean checkout at the exact pin; `EVAL_M2` may name a different
task-owned repository with an absolute path and no whitespace. Producer Maven 3.8.6 does not read
`MAVEN_ARGS`, so preparation passes `-Dmaven.repo.local` explicitly to its wrapper.
`-DskipTests` builds producer artifacts and does not claim fresh producer acceptance gates.
The root install supplies the tutorial parent; the separate case install supplies demo dependencies.
The child build and each harness caller run during preparation so an empty cache is usable offline.

Rehearse all ordinary checks, all five demos and the separate intentional assertion failures:

```bash
./scripts/rehearse-petclinic-demo.sh
```

For short stage commands in [PRESENTER-RUNBOOK.md](PRESENTER-RUNBOOK.md), keep this shell setup:

```bash
export EVAL_M2="$PWD/.petclinic-demo/m2"
export AGENT_JUDGE_TUTORIAL_AGENT=recorded
unset AGENT_JUDGE_TUTORIAL_CAPTURE
export MAVEN_ARGS="-o -Dmaven.repo.local=$EVAL_M2"
```

`.petclinic-demo/producer-commit.txt` records source identity; `artifacts.sha256` records the
installed POM/JAR bytes. The rehearsal checks every hash before and after execution.
`LoadedArtifactsTest` prints the actual class locations and SHA-256 values for the six loaded
Agent Eval libraries and Jackson. With `-Deval.artifacts="$PWD/.petclinic-demo/artifacts.sha256"`
it checks loaded producer bytes against that manifest, as the rehearsal does. Module 01's
Surefire classpath identifies the installed exec JAR as well. The default Maven cache is not used
for these library artifacts. Maven wrapper distributions and JBang's harness dependencies have
their own caches, also prepared by the first command.

Module 01 performs a real build of a materialized copy; vendored subjects and all historical
recording bytes stay unchanged. Materialization applies Spring's formatter and prints the changed
file disclosure. Modules 02–05 replay archived responses. Fresh replay verifies current API behavior
and retention over those answers, not fresh inference or newly established subject truth.

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

`RequirementExamplesTest` shows actual RULE-3 and UC6-AC7 requirements, real source/test evidence,
public `Rfc2119Judge`/`EarsJudge` construction and `judge()`, requirement-first AssertJ, and
conclusion versus application reliance. Its single answers are selected verbatim from roster
recordings by `RecordedEvalModel.singleRequirement`; full archived text and provenance remain
native facts. These are explicitly extracted replay answers, not new single-requirement runs.

`ConfiguredExamples` supplies prepared/structured factories for conformance tests. Their synthetic
responses are plumbing controls, not PetClinic findings. Structured rosters execute per item:
6, 52 or 13 executions. The conformance tests
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

## Optional live backend

The existing opt-in is `AGENT_JUDGE_TUTORIAL_AGENT=live` for an individual console module, with
AgentClient/Claude and its credentials installed. Leave `AGENT_JUDGE_TUTORIAL_CAPTURE` unset to
avoid writing recordings. Workspace/tools/timeout are configured by `JudgeBackends` below the
public Judge/Jury interfaces. Live may take minutes and outcomes may differ; it has not been run
for this rehearsal. The preparation/rehearsal scripts explicitly select recorded mode, and the
single-example JUnit tests always use archived selections. Keep replay as the conference fallback.
