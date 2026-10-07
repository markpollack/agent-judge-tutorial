# PetClinic presenter runbook

Say up front: **the build is real; model answers are committed-response replay.** This demonstrates
current Agent Eval APIs over archived responses, without fresh inference or a new claim of subject
truth. The subject is Anton Arhipov's `appointment-scheduling-spec-with-usecases` at
`fc9df4af46171bf7b6146d0477cc68d70e8532ad`. The producer is integrated source
`b7d2d88ace7cbf110dbfd47ab57221d9e364fb7e`, locally built as `0.18.0-SNAPSHOT`.

Prepare from the tutorial root: `./scripts/prepare-petclinic-demo.sh`.
Verify the offline fallback: `./scripts/rehearse-petclinic-demo.sh`.
Both select replay even if the calling shell was set to live. Java 21, Python 3 and JBang are needed.
Keep the prepared worktree, `.petclinic-demo/m2`, materialized candidate and wrapper/JBang caches
on the presentation machine. [CONFIGURED-RUN.md](CONFIGURED-RUN.md) explains artifact identity.

For the short commands below, from the tutorial root:

```bash
export EVAL_M2="$PWD/.petclinic-demo/m2"
export MAVEN_ARGS="-o -Dmaven.repo.local=$EVAL_M2"
export AGENT_JUDGE_TUTORIAL_AGENT=recorded
unset AGENT_JUDGE_TUTORIAL_CAPTURE
CS=case-studies/spec-driven-petclinic
```

Paths below are relative to `$CS`. Java files are under each named module's
`src/main/java/io/github/markpollack/judge/tutorial/` or, for JUnit, `src/test/java/`.

| Order / files to open | Short command | Expected result / teaching point |
|---|---|---|
| 1. `fixtures/petclinic/PROVENANCE.md`; `module-01-build` → `build/BuildDemo.java` | `./mvnw -f "$CS/pom.xml" -pl module-01-build exec:java` | Build PASS. Real javac/JUnit execution in a materialized copy, about a minute. The formatter disclosure belongs to preparation; vendored source is unchanged. |
| 2. Vendored `spec/smart-appointment-scheduling/rules.md` → RULE-3; UC6 `criteria.md` → UC6-AC7; `module-02-ears-slice` test → `ears/RequirementExamplesTest.java` | `./mvnw -f "$CS/pom.xml" -pl module-02-ears-slice -Dtest=RequirementExamplesTest test` | Four green tests: actual RFC and EARS single Judges, requirement-first satisfaction, ready-Jury assertion, PASS conclusion with ESCALATE application action. Singles replay verbatim selected archive lines. |
| 3. `module-02-ears-slice` → `ears/EarsSliceDemo.java`, especially `jury()` and `vote()` | `./mvnw -f "$CS/pom.xml" -pl module-02-ears-slice exec:java` | Six requirements, six PASS, overall PASS. One backend invocation for the selected roster. |
| 4. `module-03-ears-usecase` → `ears/EarsUseCaseDemo.java`; UC6-AC41 in `criteria.md` | `./mvnw -f "$CS/pom.xml" -pl module-03-ears-usecase exec:java` | 51 PASS, UC6-AC41 ABSTAIN, overall INCONCLUSIVE. Completeness changes what can be established. |
| 5. `module-04-rfc2119-rules` → `rules/Rfc2119RulesDemo.java`; RULE-4 in `rules.md` | `./mvnw -f "$CS/pom.xml" -pl module-04-rfc2119-rules exec:java` | Five PASS, eight FAIL, overall FAIL. A passing build did not establish architectural conformance. |
| 6. The three `ShouldIMerge*Demo.java` JUnit files | Commands below | Slice green; behavioral and architecture assertions intentionally red. Ordinary regression tests verify these expected populations and stay green. |
| 7. `module-05-investigation` → `investigation/InvestigationDemo.java`; `tutorial-support/src/main/resources/recordings/rule-4-investigation.txt` | `./mvnw -f "$CS/pom.xml" -pl module-05-investigation exec:java` | One separate RULE-4 investigation, archived CONFIRMED / REACHABLE. It consumes the actual failed result and preserves the complete argument. |

The public construction is visible in the Java shown on stage:

> Here is the requirement. Here is who judges it. Here is the evidence. Here is how my application
> is willing to act on the judgment. Now establish satisfaction.

In `RequirementExamplesTest`, show `.requirement(...).evidence(...).build().judge()` and then
`assertThat(requirement).judgedBy(...).withEvidence(...).isSatisfied()`. Prepared evidence is read
from real source/test files. The support backend only selects existing recorded text and retains
the original. Then show `.withPolicy(...)`: an application may request review while the original
Verdict remains PASS. The ready-Jury assertion and policy example create two separate evaluation
stages; each vote enters the entire roster once. Repeated terminals on one stage use its cache.

Run the explicit JUnit gates individually; the last two commands are expected to exit 1:

```bash
./mvnw -f "$CS/pom.xml" -pl module-02-ears-slice -Dtest=ShouldIMergeSliceDemo test
./mvnw -f "$CS/pom.xml" -pl module-03-ears-usecase -Dtest=ShouldIMergeBehaviorDemo test
./mvnw -f "$CS/pom.xml" -pl module-04-rfc2119-rules -Dtest=ShouldIMergeArchitectureDemo test
```

Each red gate must report **one assertion failure and zero errors**, with INCONCLUSIVE or FAIL
in the failure. A missing dependency, recording or workspace is an infrastructure problem,
not the intended rejection. The rehearsal script checks the new Surefire XML reports explicitly.

## Open the evidence

The spec root is
`fixtures/petclinic/appointment-scheduling-spec-with-usecases/spec/smart-appointment-scheduling/`.
Open `rules.md` for RULE-3/RULE-4 and `manage-appointment-lifecycle/criteria.md` for UC6-AC7/AC41.
The evaluated Java copy is under `fixtures/petclinic/build/large-candidate/`.

| Requirement | Supporting source/test navigation | Archived response |
|---|---|---|
| RULE-3 | `scheduling/model/Reservation.java` fields and table; `scheduling/model/Appointment.java` | `architecture-rules.txt`, the RULE-3 line |
| UC6-AC7 | `scheduling/service/AppointmentService.java`, `cancelAppointmentByOwner`; `scheduling/AppointmentServiceTests.java`, owner cancellation tests | `ears-uc6-cancellation.txt`, UC6-AC7 line |
| UC6-AC41 | Read the requirement, then its answer; follow the recorded `Vet.java`, `setActive` reference and inspect its callers | `spec-conformance-uc6.txt`, UC6-AC41 line |
| RULE-4 | `scheduling/service/StaffFallbackService.java`, `LockCoordinator.java`, `LifecycleProcessor.java`; follow the investigation's actual references | `architecture-rules.txt` and `rule-4-investigation.txt` |

Source/test paths above are relative to the candidate's `src/main/java/org/springframework/samples/petclinic/`
and `src/test/java/org/springframework/samples/petclinic/`. Recordings are under
`tutorial-support/src/main/resources/recordings/`. Use the archive's own citations; the runbook
does not create new line references or turn the archive's "today" into the rehearsal date.
Full Verdict/EvaluationResult data, native answers and policy decisions remain available even when
console output is abbreviated. Existing regression tests reopen V6 and assert retention without
producer or policy execution.

## IntelliJ and fallback

Open the tutorial root with JDK 21. Link the root `pom.xml` and the separate `$CS/pom.xml` as Maven
projects. In **Settings → Build Tools → Maven**, use the wrapper and set **Local repository** to
the absolute prepared `$EVAL_M2` path, then reload both projects. Enable **Work offline** after
preparation. Also set `-Dmaven.repo.local=<absolute EVAL_M2>` in Maven runner VM options.
Do not use the default `~/.m2/repository` for the case-study import.

For portable Maven run configurations, use tutorial root as the working directory and copy the
goals above (for example `-o -Dmaven.repo.local=<absolute EVAL_M2> -f case-studies/spec-driven-petclinic/pom.xml
-pl module-02-ears-slice -Dtest=RequirementExamplesTest test`). Set the replay environment shown
above. Maven run configurations keep the child build on the same repository through `MAVEN_ARGS`.
If using direct Java/JUnit run configurations, also set their working directory to the tutorial
root and `MAVEN_ARGS=-o -Dmaven.repo.local=<absolute EVAL_M2>` for child builds/materialization.

Run `LoadedArtifactsTest` with
`-Dmaven.repo.local=<absolute EVAL_M2> -Deval.artifacts=<absolute tutorial root>/.petclinic-demo/artifacts.sha256`
to compare actual class locations/hashes with preparation. This is more reliable than a snapshot
version label. CLI commands were rehearsed; interactive IntelliJ operation has not been verified.
Use the prepared terminal sequence if an IDE import or runner is uncertain.

For a credential-free fallback, keep `recorded` selected and run the same offline sequence; do
not improvise a live run. The existing opt-in live console path is documented in CONFIGURED-RUN,
but was not invoked here. Module 06 remains deferred. `DRY-RUN.md` is the historical 0.17 script.
