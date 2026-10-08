# Configured Agent Eval walkthrough

Use Java 21 and the tutorial Maven wrapper. This separate reactor uses the released Agent Eval
coordinates `io.github.markpollack:agent-judge-*:0.18.0` from Maven Central; the root fundamentals
reactor still uses released 0.17.0. No snapshot, local producer build or AgentWorks BOM entry is assumed.

From the tutorial root, resolve the release and prepare callers, Maven plugins, JBang harness and
materialized child build with one command (Java 21, Python 3 and JBang required):

```bash
./scripts/prepare-petclinic-demo.sh
```

Preparation downloads dependencies, including the released Agent Eval artifacts, into the standard
`~/.m2/repository`; no producer checkout or custom Maven repository setting is needed. Select Java 21
in `JAVA_HOME` and `PATH`. Unrelated versions and artifacts are retained. Maven 3.8.6 does not read
`MAVEN_ARGS`; the preparation script passes its chosen repository explicitly. The root tutorial install
supplies its parent and keeps fundamentals on released 0.17.0; the separate case install supplies the
demo dependencies. The child build is cleaned to remove stale compiled tests/reports before it runs;
every harness caller also runs during preparation so dependencies and plugins are usable offline.
Java 21, Python 3 and JBang must already be available.

Building the producer from source is optional and not needed for the walkthrough. With
`./scripts/prepare-petclinic-demo.sh --source` the script first runs the **complete** producer
reactor's `clean install` (wrapper, tests enabled) from the sibling `../agent-judge` checkout, which
must be clean and at the `v0.18.0` release tag; the script refuses to move it or overwrite local
source changes. The installed bytes then occupy the same `0.18.0` coordinates. The equivalent
library-only command is:

```bash
cd ../agent-judge
git checkout v0.18.0
./mvnw -B -ntp clean install
cd ../agent-judge-tutorial
```

For an explicitly isolated source reproduction, use `./scripts/prepare-petclinic-demo.sh --isolated` and
`./scripts/rehearse-petclinic-demo.sh --isolated`. This optional mode clones the producer at `v0.18.0`
into `.petclinic-demo/isolated/agent-judge` and installs into `.petclinic-demo/isolated/m2`, both ignored.
`EVAL_SOURCE` can reuse any existing clean checkout at the tag in source modes; `EVAL_M2` can
select a different cache (absolute path without whitespace) in any mode. Supply the same mode and
overrides to both scripts. Isolated mode has its own manifests and does not replace the standard-cache receipts.

Rehearse all ordinary checks, all five demos and the separate intentional assertion failures:

```bash
./scripts/rehearse-petclinic-demo.sh
```

For short stage commands in [PRESENTER-RUNBOOK.md](PRESENTER-RUNBOOK.md), keep this shell setup:

```bash
export AGENT_JUDGE_TUTORIAL_AGENT=recorded
unset AGENT_JUDGE_TUTORIAL_CAPTURE AGENT_JUDGE_TUTORIAL_AI_VALIDATE
export MAVEN_ARGS="-o"
```

`.petclinic-demo/producer.txt` records the producer identity (the release, or the source tag and commit); `artifacts.sha256` records the
installed POM/JAR bytes; `repository.txt` records the chosen cache. The rehearsal checks every hash
before and after execution.
`LoadedArtifactsTest` prints the actual class locations and SHA-256 values for the six loaded
Agent Eval libraries and Jackson. With `-Deval.artifacts="$PWD/.petclinic-demo/artifacts.sha256"`
it checks loaded producer bytes against that manifest, as the rehearsal does. Module 01's
Surefire classpath identifies the installed exec JAR as well. Ordinary development and IntelliJ
use the verified standard cache. In optional isolated mode, use its manifest and add
`-Dmaven.repo.local=<absolute isolated cache>` to CLI/import/runner settings and `MAVEN_ARGS`.
Maven wrapper distributions and JBang's harness dependencies have their own caches, also prepared
by the first command.

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

## IntelliJ

Open the tutorial root and link **both** its root `pom.xml` and the separate
`case-studies/spec-driven-petclinic/pom.xml`. They are distinct Maven reactors. Use JDK 21 and
the Maven wrapper; keep the default local repository. Reload both projects after preparation.
Shared replay run configurations under `.run/` use the tutorial root as their working directory.
See [the presenter runbook](PRESENTER-RUNBOOK.md#intellij-and-fallback) for the exact import and run sequence.

## Optional live backend

The existing opt-in is `AGENT_JUDGE_TUTORIAL_AGENT=live` for an individual console module, with
AgentClient/Claude and its credentials installed. Leave `AGENT_JUDGE_TUTORIAL_CAPTURE` unset to
avoid writing recordings. Workspace/tools/timeout are configured by `JudgeBackends` below the
public Judge/Jury interfaces. Live may take minutes and outcomes may differ; it has not been run
for this rehearsal. The preparation/rehearsal scripts explicitly select recorded mode, and the
single-example JUnit tests always use archived selections. Keep replay as the conference fallback.
