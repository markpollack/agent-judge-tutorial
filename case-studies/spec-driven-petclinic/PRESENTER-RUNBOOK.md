# PetClinic presenter runbook

Say up front: **the build is real; model answers are committed-response replay.** This demonstrates
current Agent Eval APIs over archived responses, without fresh inference or a new claim of subject
truth. The subject is Anton Arhipov's `appointment-scheduling-spec-with-usecases` at
`fc9df4af46171bf7b6146d0477cc68d70e8532ad`. The producer is released Agent Eval
`io.github.markpollack:agent-judge-*:0.18.0`, resolved from Maven Central.

Prepare from the tutorial root: `./scripts/prepare-petclinic-demo.sh`.
Verify the offline fallback: `./scripts/rehearse-petclinic-demo.sh`.
Both select replay even if the calling shell was set to live. Java 21, Python 3 and JBang are needed.
Keep the ordinary tutorial checkout, prepared `~/.m2/repository`, materialized candidate and
wrapper/JBang caches on the presentation machine. [CONFIGURED-RUN.md](CONFIGURED-RUN.md) explains artifact identity.

For the short commands below, from the tutorial root:

```bash
export MAVEN_ARGS="-o"
export AGENT_JUDGE_TUTORIAL_AGENT=recorded
unset AGENT_JUDGE_TUTORIAL_CAPTURE AGENT_JUDGE_TUTORIAL_AI_VALIDATE
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

1. Run preparation from the normal tutorial checkout; it resolves released Agent Eval 0.18.0, so no
   producer checkout is needed. Open **the tutorial directory**, rather than only a case-study module.
2. In **Settings → Build Tools → Maven**, select the wrapper. Leave **Local repository** at its default
   `~/.m2/repository` and remove any old `-Dmaven.repo.local` runner override. Use JDK 21 for the project,
   Maven importer and runner. No special repository setting is required.
3. Link root `pom.xml`. Then use **Maven → Link Maven Projects** (the `+` button) to add
   `case-studies/spec-driven-petclinic/pom.xml`. Alternatively, open that POM in the editor and use
   **Add as Maven Project**. The root reactor does not include PetClinic.
   Reload both Maven projects after installation; enable **Work offline** once prepared.
4. Select the shared configurations from the run dropdown. All use `$PROJECT_DIR$` as working
   directory, set recorded mode, clear capture/AI validation and set `MAVEN_ARGS=-o` for child builds.
   They use the project's JDK and imported Maven classpaths, without a custom local repository.

| Shared configuration | Expected result |
|---|---|
| `PetClinic - Loaded artifacts` | One green test; actual class locations/hashes match `.petclinic-demo/artifacts.sha256` in the standard repository |
| `PetClinic - Requirement examples` | Four green JUnit examples: RFC/EARS Judges, requirement-first assertions and conclusion/policy separation |
| `PetClinic - EARS6` | Console replay: 6 PASS → PASS |
| `PetClinic - EARS52` | Console replay: 51 PASS, 1 ABSTAIN → INCONCLUSIVE |
| `PetClinic - RFC13` | Console replay: 5 PASS, 8 FAIL → FAIL |
| `PetClinic - ShouldIMerge slice` | One green JUnit test |
| `PetClinic - ShouldIMerge behavior` | One intentionally red assertion, INCONCLUSIVE; zero errors |
| `PetClinic - ShouldIMerge architecture` | One intentionally red assertion, FAIL; zero errors |

Run **Loaded artifacts** first after import. It compares actual loaded producer JAR bytes with the
preparation manifest; a version label alone cannot detect stale or locally rebuilt bytes. For the terminal equivalent:

```bash
./mvnw -o -f "$CS/pom.xml" -pl module-02-ears-slice -Dtest=LoadedArtifactsTest \
  -Deval.artifacts="$PWD/.petclinic-demo/artifacts.sha256" test
```

The optional `--isolated` mode requires explicit cache settings and its separate manifest; it is
not the ordinary IDE setup. All eight shared configurations were directly rehearsed in IntelliJ
IDEA 2026.2.3 with Java 21, both Maven projects linked and the default repository. The loaded-JAR
check matched the preparation manifest. Module 01 and module 05 were verified through the CLI
harness. CLI rehearsal is the offline fallback if import or an IDE runner fails.

For a credential-free fallback, keep `recorded` selected and run the same offline sequence; do
not improvise a live run. The existing opt-in live console path is documented in CONFIGURED-RUN,
but was not invoked here. Module 06 remains deferred. `DRY-RUN.md` is the historical 0.17 script.
