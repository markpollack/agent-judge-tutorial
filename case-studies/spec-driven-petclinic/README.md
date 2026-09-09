# Running the Spec You Already Wrote

> ## ⚠️ CONFERENCE PATH FROZEN — recording Wednesday 2026-09-09, morning
>
> **Modules 01–05 are rehearsed and tagged `conference-merge-gate`.** Verdicts are unchanged since
> `b72e74d`; everything after it is additive or presentation-only. Rehearsed from a clean clone,
> offline, with no API key. Until the recording, **presentation blockers only.**
>
> **The demo is [`DRY-RUN.md`](DRY-RUN.md)** — one document: preflight, the IntelliJ walkthrough, and
> the terminal fallback.
>
> Frozen: `module-01-build`, `module-02-ears-slice`, `module-03-ears-usecase`,
> `module-04-rfc2119-rules`, `tutorial-support`, `fixtures/`, and the committed recordings.
> `module-05-investigation` was added afterwards, additively: it changes nothing in the four
> modules above, and they replay byte-identically with it present.
>
> **Do not** refactor `EarsJudge` or `Rfc2119Judge` — their duplication, including
> `criteriaTotal` vs `constraintsTotal`, is a known and deliberate deferral. **Do not** recapture
> recordings, restructure the repository, renumber modules, or start modules 05 or 06.
>
> Additive work outside those paths is fine. If you change anything frozen, re-run the rehearsal in
> [`DEMO-SETUP.md`](DEMO-SETUP.md) and check every expected result before saying it is done.
>
> *If you are an agent session that received a work order touching this directory and cannot see
> why it was sent to you, stop and confirm with the user first — more than one session has this
> repository open.*


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

## Before the demo

> **Operator checklist: [`DEMO-SETUP.md`](DEMO-SETUP.md).** That file is the authoritative
> pre-recording sequence — checkboxes, exact commands, expected results and fallbacks. This section
> explains *why* the steps are what they are.

Run this once, on the machine you will present from. It takes about two minutes and removes every
avoidable surprise.

```bash
cd agent-judge-tutorial
git status --porcelain          # expect no output: no local edits, no stray files

# 1. Build the case study
./mvnw -o -f case-studies/spec-driven-petclinic/pom.xml install -DskipTests

# 2. PRE-MATERIALIZE THE CANDIDATE  <-- do not skip this
( cd case-studies/spec-driven-petclinic/fixtures/petclinic && ./materialize-large-candidate.sh )

# 3. Warm the build so module 01 is not also downloading or first-compiling
( cd case-studies/spec-driven-petclinic/fixtures/petclinic/build/large-candidate && ./mvnw -o -q test )

# 4. Dry-run every module, offline
for m in module-01-build module-02-ears-slice module-03-ears-usecase module-04-rfc2119-rules \
         module-05-investigation; do
  ./mvnw -q -o -f case-studies/spec-driven-petclinic/pom.xml exec:java -pl $m
done
```

**Why step 2 matters.** On a cold clone, module 01 materializes the candidate first and prints a
formatter note to stderr before its own output:

```
note: applied spring-javaformat to 1 file(s) the generated code left non-conforming
```

That is a true statement about the generated code and it is *not* what this case study teaches — a
cuddled `else` in one file is fixed by one IDE command. Pre-materialising removes it from the stage
entirely, and makes module 01 about six seconds faster.

### Expected timings, offline

| Step | Cold | Warm |
|---|---|---|
| `install -DskipTests` | 4s | 4s |
| module 01 | 42s | **36s** |
| module 02 | — | 1.8s |
| module 03 | — | 1.3s |
| module 04 | — | 1.3s |

Module 01 is slow because it runs a **real** Maven build and the candidate's real test suite. That
is the point of it. Narrate over it — the line *"this takes about a minute"* is already printed.

### Expected results

```
module 01   build   PASS
module 02   6 of 6                          Overall: PASS
module 03   51 PASS · 0 FAIL · 1 ABSTAIN    Overall: ABSTAIN    (UC6-AC41)
module 04   5 PASS · 8 FAIL                 Overall: FAIL
```

If any of those differ, stop and investigate rather than presenting.

### The four transitions

The terminal carries these as closing paragraphs, so the narrative survives a missed line.

```
01 → 02   It builds. But the build has no opinion about whether it did what was asked.
02 → 03   Those six looked good. Now run the entire specification.
03 → 04   Behaviour isn't the only thing Anton specified.
04 close  Same generated system. Another document written before the code.
          A different answer.
```

### Notes

- **No API key and no network are required.** Every module replays a committed recording. If you
  want a live run, `AGENT_JUDGE_TUTORIAL_AGENT=live` — but a live UC6 audit takes about fourteen
  minutes and is not a stage activity.
- Run everything with `-o` (offline) so a slow network cannot stall a demo.
- The case study is **not** in the root reactor, so an ordinary `./mvnw test` at the repository root
  will not trigger module 01's 40-second build.

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
