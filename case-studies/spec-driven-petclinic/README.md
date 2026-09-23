# Running the Spec You Already Wrote

> **Documentation**: https://lab.pollack.ai/docs/agent-judge/tutorial | [API Reference](https://lab.pollack.ai/docs/agent-judge/api-reference)

A case study, not a tutorial. The progressive tutorial at the repository root teaches Agent Judge
one concept at a time; this evaluates one real system end to end.

## The situation

An agent was given a written specification and produced a working Spring application — 166 Java
files, 290 passing tests. It says it is done. Should you merge?

The specification is not ours. It ships inside the branch under evaluation, was written **before**
the code, and carries **438 numbered requirements across 15 documents**: 374 EARS acceptance
criteria, 51 use-case constraints, and 13 feature-wide architectural rules, all with stable
identifiers and traceability.

Nothing was ever running it as an evaluation.

## The modules

Numbering is local to this case study.

| Module | Question | Instrument |
|---|---|---|
| `module-01-build` | Does it build and pass its existing tests? | `BuildSuccessJudge`, no model |
| `module-02-ears-slice` | Six readable acceptance criteria | `EarsJudge` |
| `module-03-ears-usecase` | All 52 criteria for one use case | `EarsJudge` |
| `module-04-rfc2119-rules` | The 13 architectural MUSTs | `Rfc2119Judge` |
| `module-05-investigation` | What does one failure actually mean, and can it happen? | investigation tier |
| `module-06-promotion` | Which findings can leave the model path? | deterministic tooling |

**Modules 01–05 are the conference path.** Module 06 is deliberately not implemented: once a
finding is precise enough to encode as an ArchUnit rule or an ordinary deterministic test, the model
should leave that loop — but establishing that idea does not require building it, and the case study
says so rather than shipping a half-built version of it.

**Module 05 investigates exactly one of module 04's eight failures**, the lock-ordering rule. One
investigation, not eight: the concept is that a failed requirement is an address and that
establishing its consequence is a separate question asked by a separate call. The eighth repetition
of that teaches nothing the first did not, and a fan-out over all eight would turn a finding back
into a backlog.

## Run it

This checkout uses released **Agent Judge 0.17.0**. Start at the repository root with Java 21
and the committed Maven wrapper. The first build downloads dependencies, including those needed
by the real PetClinic build:

```bash
./mvnw install
./mvnw -f case-studies/spec-driven-petclinic/pom.xml install
./mvnw -q -f case-studies/spec-driven-petclinic/pom.xml exec:java -pl module-02-ears-slice
```

The root install supplies `judge-junit`, which the separate case-study reactor consumes.
The first demo invocation also caches the execution plugin.
After preparation, replay without model credentials and with Maven offline (including child builds):

```bash
unset ANTHROPIC_API_KEY AGENT_JUDGE_TUTORIAL_AGENT
export MAVEN_ARGS="${MAVEN_ARGS:-} -o"
for module in module-01-build module-02-ears-slice module-03-ears-usecase module-04-rfc2119-rules module-05-investigation; do
  ./mvnw -q -f case-studies/spec-driven-petclinic/pom.xml exec:java -pl "$module"
done
```

Module 01 executes a real build; modules 02–05 replay committed model responses. Expected results:

| Module | Result |
|---|---|
| 01 | PASS |
| 02 | Six established; PASS |
| 03 | 51 PASS, zero FAIL, one ABSTAIN (`UC6-AC41`); overall ABSTAIN |
| 04 | Five PASS, eight FAIL; overall FAIL |
| 05 | One RULE-4 investigation; recorded CONFIRMED / REACHABLE |

With JBang installed, verify all five demo outputs:

```bash
./integration-testing/scripts/run-integration-tests.sh --case-study
# After the harness dependencies are cached:
./integration-testing/scripts/run-integration-tests.sh --case-study --offline
```

Ordinary tests assert these recorded outcomes and pass. The separate `ShouldIMerge*Demo` tests
require PASS: the slice passes, the full behavioral specification rejects on ABSTAIN, and the
architecture rejects on FAIL. See the walkthrough for exact commands. Investigation is diagnosis;
it does not turn a rejected merge gate green.

## Presenting it

**[`DRY-RUN.md`](DRY-RUN.md) is the authoritative walkthrough** — one preflight command, the
IntelliJ run, expected results, and a terminal fallback.

## Learn the concepts

The model assesses individual requirements; Java does everything else — the roster comes from the
specification, the verdict is computed in code, and there is no score anywhere.

**The website carries the teaching text:**
[PetClinic case study](https://lab.pollack.ai/docs/agent-judge/petclinic-case-study) ·
[requirements judges](https://lab.pollack.ai/docs/agent-judge/requirements-judges) ·
[tutorial](https://lab.pollack.ai/docs/agent-judge/tutorial) ·
[design philosophy](https://lab.pollack.ai/docs/agent-judge/design-philosophy) ·
[custom judges](https://lab.pollack.ai/docs/agent-judge/custom-judge) ·
[API reference](https://lab.pollack.ai/docs/agent-judge/api-reference)

## The subject

`fixtures/petclinic/` — Anton Arhipov's `spring-petclinic-fork`, branch
`appointment-scheduling-spec-with-usecases` at `fc9df4af`, vendored byte-identical and **never
modified**. Provenance and licensing in `fixtures/petclinic/PROVENANCE.md`.

`superseded/` holds earlier exploratory modules kept for reference. They are not in the reactor.
