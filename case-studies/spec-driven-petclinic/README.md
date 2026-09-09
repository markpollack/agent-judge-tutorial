# Running the Spec You Already Wrote

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

From the repository root:

```bash
./mvnw -f case-studies/spec-driven-petclinic/pom.xml install -DskipTests
./mvnw -f case-studies/spec-driven-petclinic/pom.xml exec:java -pl module-01-build
./mvnw -f case-studies/spec-driven-petclinic/pom.xml exec:java -pl module-02-ears-slice
```

No API key and no network: every module replays a committed recording of a real agent run. To run
against a live agent instead:

```bash
AGENT_JUDGE_TUTORIAL_AGENT=live ./mvnw -f case-studies/spec-driven-petclinic/pom.xml \
    exec:java -pl module-02-ears-slice
```

Module 01 runs a real Maven build of the candidate and takes about 40 seconds. That is deliberate —
it is a real build, not a simulation. Everything after it replays instantly. This case study is
**not** part of the root reactor for that reason.

## Presenting it

**[`DRY-RUN.md`](DRY-RUN.md) is the authoritative walkthrough** — one preflight command, the
IntelliJ run, expected results, and a terminal fallback.

## What each judge may and may not do

The model assesses individual requirements. Java does everything else:

- the roster comes from the specification, and a document with N requirements yields N answers or
  the judge returns `ERROR` naming what is missing;
- the verdict is computed in Java, never asserted by the model;
- **PASS means every required requirement was affirmatively established** —
  `any ERROR → ERROR, else any FAIL → FAIL, else any ABSTAIN → ABSTAIN, else PASS`;
- there is no score. `Judgment.score()` is null, and a test asserts it stays that way.

A judge may also record a **non-binding observation** — something useful noticed while establishing
a requirement that the requirement did not itself demand. Observations never enter the roster and
never change a verdict.

## The subject

`fixtures/petclinic/` — Anton Arhipov's `spring-petclinic-fork`, branch
`appointment-scheduling-spec-with-usecases` at `fc9df4af`, vendored byte-identical and **never
modified**. Provenance and licensing in `fixtures/petclinic/PROVENANCE.md`.

`superseded/` holds earlier exploratory modules kept for reference. They are not in the reactor.
