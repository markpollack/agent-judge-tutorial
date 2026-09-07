# Design: Agent Judge Tutorial — Running the Spec You Already Wrote

> **Created**: 2026-09-07T10:20-04:00
> **Last updated**: 2026-09-07T14:15-04:00
> **Vision version**: 2026-09-07T14:05-04:00

## Overview

One subject, one judge shape, three documents, two tiers.

The subject is Anton Arhipov's spec-driven PetClinic branch, vendored unmodified. The rubrics are
his own numbered requirement documents, read verbatim. The judge answers every requirement in a
document or errors. When it says FAIL it produces an **address**; a second tier turns that address
into a **consequence**. Findings that turn out to be mechanisable leave the model behind entirely
and become deterministic rules.

The tutorial's shape follows the evidence: behaviour conforms and structure does not, and no single
document could have asked both questions.

The guiding rule throughout is **use the least interpretive instrument that can reliably answer the
question**, and its corollary — **deterministic where possible, AI when necessary.** Agent Judge does
not compete with JUnit, ArchUnit, JaCoCo, Checkstyle, static analysis or the compiler; it extends the
testing discipline to criteria whose oracle needs richer evidence or judgment.

## Build Coordinates

- **groupId**: `io.github.markpollack`
- **artifactId**: `agent-judge-tutorial`
- **Java**: 21 · **Build**: Maven wrapper (`./mvnw`)
- **Base package**: `io.github.markpollack.judge.tutorial`

### Module Structure

```
module-01-build/            does it build and pass its tests?  BuildSuccessJudge, no model
module-02-ears-slice/       6 readable EARS criteria (UC6)     EarsJudge
module-03-ears-usecase/     all 52 UC6 EARS criteria           EarsJudge, completeness enforced
module-04-rfc2119-rules/    the 13 feature-wide MUSTs          Rfc2119Judge
module-05-investigation/    addresses -> consequences          fan-out
module-06-promotion/        mechanisable findings leave the    ArchUnit + Java + file assertions
                            model path
```

**The arc is exactly these six.** Modules 01-04 are the conference path; 05 and 06 deepen the idea
and are not prerequisites for 01-04.

Supporting, not in the arc:

```
fixtures/petclinic/         vendored subjects, never modified
tutorial-support/           parsers, backends, recordings shared by modules
integration-testing/        jbang harness, one config per module
```

### Key Dependencies

| Dependency | Version | Why |
|---|---|---|
| `agent-judge-core` / `-ai` / `-exec` | 0.16.0-SNAPSHOT | `Judgment`, `Check`, `ModelBackedJudge`, `AllMustPassStrategy`, `BuildSuccessJudge` |
| `agent-judge-agent-client` | 0.16.0-SNAPSHOT | `AgentClientJudgeModel` — the only path to a model |
| `agent-client` | 0.29.3 | `ClaudeAgentModel`, `ClaudeAgentOptions` |
| `archunit` | 1.5.0 | deterministic bytecode rules in module 06 |
| `junit-jupiter` | via parent | verdict tests |

## Architecture

### Components

| Component | Responsibility | Model? |
|---|---|---|
| `EarsCriterion` | parses an EARS acceptance-criteria document into `(id, template, trigger, response)` | no |
| `Rfc2119Constraint` | parses a MUST/SHOULD/MAY document into `(id, keyword, requirement, reason)` | no |
| `Requirement` | the interface both implement: `id()`, `title()`, `asPrompt()`, `obligation()` | — |
| `EarsJudge` | answers every EARS criterion against a workspace | yes |
| `Rfc2119Judge` | answers every constraint, honouring keyword obligation in the rollup | yes |
| `Investigation` | for one FAIL, establishes consequence and reachability | yes |
| `ArchUnitJudge` | runs promoted rules over compiled bytecode | no |
| `ConfigRules` | file-level assertions for facts bytecode cannot see | no |
| `JudgeBackends` | live via AgentClient, or replay a committed recording | — |

### Data Flow

```
    criteria.md ──► EarsCriterion ──┐
                                    ├──► List<Requirement> ──► Judge ──► Judgment
    rules.md    ──► Rfc2119Constraint┘                                      │
                                                                            │
                                              ┌── PASS ────────────────────┤
                                              │                             │
                                              └── FAIL ──► Check(id, location, narrative)
                                                              │
                                                              ▼
                                                        Investigation
                                                              │
                                              consequence + reachability + severity
                                                              │
                                                     ┌────────┴────────┐
                                                mechanisable?      judgment
                                                     │                 │
                                              ArchUnit / file      stays with
                                               assertion            the judge
```

## The Subject

`fixtures/petclinic/appointment-scheduling-spec-with-usecases/` — `antonarhipov/spring-petclinic-fork`
at `fc9df4af`, imported by `git archive`, byte-identical, Apache 2.0 preserved.

- 166 main Java files, 290 tests across 53 classes (4 skipped: the MySQL and Postgres Testcontainers
  classes).
- The materialize script applies `spring-javaformat` so it compiles, and reports what it changed.
  **The formatting finding is recorded in provenance and never taught.**
- `baseline/` at `88e37c15` is kept as the provenance anchor: upstream PetClinic, 17 test classes,
  71 tests, no `spec/` directory.

## The Rubrics

All fifteen documents ship inside the branch. Identifiers are globally unique, which is what makes
the roster guard work across any combination.

```
spec/smart-appointment-scheduling/
├── rules.md ──────────────────────────── RULE-1..13                13   MUST-form
├── secure-scheduling-access/       UC1   criteria.md  47   rules.md   6
├── configure-clinic-scheduling/    UC2   criteria.md  51   rules.md   6
├── interpret-appointment-request/  UC3   criteria.md  83   rules.md  13
├── guide-appointment-selection/    UC4   criteria.md  48   rules.md   6
├── resolve-staff-fallback/         UC5   criteria.md  48   rules.md   6
├── manage-appointment-lifecycle/   UC6   criteria.md  52   rules.md   7
└── notify-audit-and-recover/       UC7   criteria.md  45   rules.md   7
                                          ───────────────   ──────────
                                                     374           51
                                                        + 13 feature = 438
```

Measured grammar: UC6's 52 criteria are **28 `When` / 20 `If` / 4 `While`** — fully EARS, no
ubiquitous forms. The 13 feature rules are **13 of 13 `**MUST**`**.

The arc reads exactly two of these fifteen documents: `manage-appointment-lifecycle/criteria.md`
(modules 02 and 03) and `rules.md` (module 04). The remaining thirteen stay in the vendored evidence
because they are part of Anton's repository; they are not part of the active arc. In particular the
**51 use-case rules are not a prerequisite, module, or research question** for this tutorial.

## Interfaces

### `Requirement`

```java
public interface Requirement {
    String id();                 // UC6-AC5, UC6-RULE1, RULE-4 — globally unique
    String title();              // short label for terminal output
    String asPrompt();           // the requirement as the judge is asked to assess it
    Obligation obligation();     // MUST | SHOULD | MAY | ACCEPTANCE
}
```

`obligation()` is the design's load-bearing addition. RFC 2119 encodes the aggregation policy in the
requirement itself: a `MUST` violation fails the gate, a `SHOULD` violation is reported and does
not. **The document tells us how to aggregate it; we do not choose.**

### `EarsJudge`

```java
EarsJudge.create(Path criteriaFile, Path workspace, String recording)
EarsJudge.create(List<EarsCriterion> criteria, JudgeModel model)   // the test seam
```

### `Rfc2119Judge`

```java
Rfc2119Judge.create(Path rulesFile, Path workspace, String recording)
Rfc2119Judge.create(List<Rfc2119Constraint> constraints, JudgeModel model)
```

Both return a `Judgment` whose `checks()` carry one entry per requirement and whose `reasoning()` is
computed in Java from parsed outcomes — never asserted by the model.

## Data Models

### `Check`, as this design uses it

Today `Check.message` is a single blob of model prose containing both the reliable address and the
unreliable narrative. This design separates them:

```
Check
  name        UC6-AC8
  status      FAIL
  locations   [AppointmentService.java:154]      the class that survived verification
  narrative   "rejects only when now.isAfter…"   the class that did not
```

Until `agent-judge` carries structured locations, the tutorial parses them out of the model's line
and stores them in `Judgment` metadata keyed by requirement id.

## Design Decisions

### DD-1: The subject is Anton's branch only; no hand-authored example changes

**Decision**: every module judges the vendored generated system. The previously used hand-written
`city-search.patch` is demoted out of the arc.

**Why**: the patch was authored to follow PetClinic's conventions and then judged for conformance.
That is development, not evaluation. Keeping it would put the weakest evidence at the front of the
tutorial.

**Cost**: modules need the candidate materialized (one-time, cached). Accepted.

### DD-2: Rubrics come from documents, never from a judge's own output

**Decision**: no criterion in the tutorial is authored by us or derived from what a judge produced.

**Why**: the retired conformance judge used five criteria derived from its own prior runs — the
answer shaping the question. Every rubric now has an author, a file, and a date preceding the code.

**Consequence**: the judge's own instability is a stated measurement rather than a demonstration
(see DD-3), because the fixed criteria set it needs is no longer ours to invent.

### DD-3: Assert the search; show the result

**Decision**: experiments that discovered the right approach are compressed into a sentence with
numbers. Only working instruments are run on stage.

**Why**: three live runs to establish "the judge carves inconsistently" costs minutes and teaches
what one sentence teaches. The tutorial shows the destination; the journey is in
`plans/learnings/`.

**Applies to**: carving instability, mutation testing, the shadow ArchUnit rules, the degenerate
count-and-name oracles.

### DD-4: Judges are named for public standards

**Decision**: `EarsJudge` and `Rfc2119Judge`, not a generic `CriteriaAuditJudge`.

**Why**: EARS and RFC 2119 are public requirement standards. A judge named for a standard works on
anyone's conforming document; a judge named for our abstraction works on ours. It is also the
honest answer to "what do you bring that the spec pipeline doesn't already have."

**Consequence**: `Rfc2119Judge` genuinely uses its format — the keyword supplies the obligation
level, which is the aggregation policy (DD-12), read from the document rather than chosen by us.
That is shipping behaviour.

For `EarsJudge`, using the template's trigger/response structure is *desirable, not required*. Ship
the smallest honest judge that reliably evaluates Anton's existing criteria; if parsing the template
improves it, keep that. **No A/B comparison between prompt strategies before the conference** — the
teaching fact is that structured acceptance criteria written before the code can be run back as an
independent oracle, not that we found the optimal prompting strategy.

### DD-5: Two tiers — the judge localizes, the investigation establishes consequence

**Decision**: judge output is a set of **leads** (address + hypothesis). A separate tier produces
**findings** (consequence + reachability + rank).

**Why**: measured. Across 65 requirements in two runs every file-and-line citation was correct and
every aggregate claim was wrong. And the judge emitted findings in document order, which carries no
priority — the severity order established by investigation bears no relation to it.

**Framing constraint**: the investigation is asked to *"establish the consequence and whether it is
reachable"*, never *"verify this finding"*. The latter produces an agent that agrees. Downgrading
must be explicitly invited, or the second tier is an expensive echo.

### DD-6: The verdict is computed in Java; the model only assesses

**Decision**: the model returns a status and evidence per requirement. Rollup is
`AllMustPassStrategy` in code. Denominators and counts in `reasoning()` are computed from parsed
outcomes.

**Why**: asking a model to invent criteria, apply them, and roll them up is unstable in the first
and third of those. Removing both stabilised the judge (5/6/5 criteria with zero shared names and a
2-2 verdict split, to 5 of 5 identical runs).

### DD-7: A roster guard, and an abstention that cannot pass

**Decision**: a document with N requirements yields N answers or the judge returns ERROR naming what
is missing. Abstentions leave the aggregation population and are named in metadata.
`AllMustPassStrategy` returns ABSTAIN over an empty population.

**Why**: the live agent answered UC6-AC1..46, then AC48..52, then AC47 — order is not part of the
contract, completeness is. And a pass over an empty set is an abstention wearing a pass.

### DD-8: An unrunnable judge is an ERROR about the judge

**Decision**: a missing recording, an unbuilt workspace, or an incomplete agent run yields ERROR
with a message naming the operator's problem — never FAIL.

**Why**: a missing recording once rendered as "the auditing agent did not complete its run", which
blames the subject for the author's misconfiguration.

### DD-9: Formatting findings are excluded from teaching

**Decision**: the materialize script applies `spring-javaformat` and records what it changed;
no module teaches the formatting failure.

**Why**: one file with a cuddled `else` is fixed by one IDE command. It is not intellectually
interesting and it distracts from coding defects.

### DD-10: Mutation testing lives in the test suite, not the tutorial

**Decision**: the seeded operator (`!now.isBefore(start)` → `now.isAfter(start)`, flipping UC6-AC8
alone) becomes an assertion in the judge's own test suite.

**Why**: leading with "here is how we discovered our judge might be broken" undersells the
instrument. `assertEquals` has unit tests; nobody teaches `assertEquals` by showing them. The arc
does not need a manufactured defect because the judge produces eight real failures on real code.

### DD-11: Incubate here, promote to `agent-judge`

**Decision**: `EarsJudge`, `Rfc2119Judge` and the investigation tier are developed in the tutorial
against real documents, and moved into `agent-judge` once they work end to end.

**Why**: the tutorial is the forcing function — a judge that cannot answer 438 real requirements
from a real spec is not ready to be a library API. Promotion criteria are in the roadmap.

### DD-12: No score, ever. Individual criteria and a named binding requirement

**Decision**: no judge in this tutorial emits a numeric score, a percentage, a rating, or a mean.
Every judgment carries one `Check` per requirement and a conjunctive rollup. When it fails, it names
the requirement that bound it and where.

**Why — the actionability test**:

> **A score of 6 out of 10 is meaningless if you do not know what would make it 7.**

That is the Mandela Effect in numeric form. `6/10` is plausible, confident, unanimous-sounding, and
carries no receipt. You cannot open it, re-run it, or act on it. Nobody can say which criterion
moved, in which direction, or what would move it back.

Compare what this design emits:

```
    51 of 52 requirements pass, 0 fail, 1 could not be determined
    could not be determined: UC6-AC41
```

You know exactly what would make it 52, and exactly which sentence in which file to go and read.

**Aggregate scores hide a world of sins.** The failure is mechanical, not stylistic:

| What gets stored | What it destroys |
|---|---|
| a mean over 7 criteria | *which criterion binds* — the diagnosis, and the thing you act on |
| `placement 3/5` | *which entries* — three different sets produce the same 3 |
| a verdict from the mean | the per-criterion `Check`s the judge had **already recorded** |

A mean lets `{3,3,3,3,3,3,0}` score `0.857` and pass while one criterion was, by the rubric's own
words, missed entirely. If the rubric states a per-criterion acceptability line, a mean contradicts
the rubric.

**Rules that follow**:

- Aggregate at **read** time, never write time. An aggregate is cheap to recompute from parts;
  parts cannot be recovered from an aggregate.
- Every per-item result computed on the way to a verdict is persisted as a `Check`.
- `AllMustPassStrategy` — a conjunction over outcomes. No threshold, no `passingAt`, no
  `effectiveScore` routing a status through a numeric channel.
- The failure message names the **binding requirement** and its location. That is the diagnosis, and
  it points at the intervention. A mean cannot produce either.

**Enforcement**: an arc module that prints a score is a defect, caught in its integration config's
expected output. `judge-junit`'s assertion messages append the full roster and every check, so a
failing gate in CI shows all N criteria rather than a number.

### DD-13: Every judge runs in a JUnit test, not only a demo

**Decision**: each arc module ships a `main()` for the stage **and** a JUnit test that constructs the
same judge, runs it against the same workspace, and asserts the same verdict through
`JudgeAssertions`.

**Why**: "should I merge?" is a CI question. A judge that only ever runs in a demo is a presentation
artifact; a judge in a test is a gate that fails a build. `judge-junit` is the thin adapter — no new
concepts, just `assertPass(judge, context)` — and its failure message is where DD-12 becomes
visible, because it prints the roster and every check rather than a status line.

**Consequence**: `judge-junit` is a test-scope dependency of every arc module, and the tutorial
demonstrates the ordinary way a team would actually adopt this.

### DD-14: Evaluation stays fixed while the agent changes

**Decision**: the rubric, judge configuration and acceptance policy are held **fixed** across an
experiment. Agent Experiment may vary one model, prompt, skill, toolset, workflow step or
intervention at a time.

**Why**: changing both the agent and the definition of success destroys the causal interpretation of
the result. If the bar moves with the subject, an improvement claim is unfalsifiable.

> **Self-improvement without a stable external measure is self-modification, not demonstrated
> improvement.**

**Consequence**: this tutorial produces the fixed evaluation bar. Agent Experiment is downstream and
**outside the implementation scope of this tutorial**. The boundary is:

```
Agent Judge        what does good mean, and did this run meet that bar?
Agent Experiment   holding that bar fixed, which intervention actually improves the system?
```

This also states the complement to an `execute ↔ converge` loop: converge *toward what*, measured
*by whom*, against *what fixed evidence?* Anton's specifications already supply much of that
external bar — which is the whole premise of this tutorial. The refinement is that the same system
must not simultaneously modify its own instructions, redefine success, and declare itself improved.

### DD-15: Deterministic promotion is a conclusion, not garnish

**Decision**: module 06 is part of the arc, not an appendix. Once a property can be reliably
expressed with ArchUnit, plain Java, a file assertion, compiler tooling or static analysis, **the
model stops evaluating that property.**

```
        AI judgment discovers / localizes something useful
                            ↓
                  can this fact be mechanised?
                    ↓                     ↓
                   yes                    no
                    │                      │
             deterministic            remains a
                policy               judgment oracle
```

**Why**: this is the concrete meaning of *deterministic where possible, AI when necessary*, and it
is one of the tutorial's main conclusions. A rule that runs in under a second, offline, on every
build is worth more than the same rule re-decided by a model every time.

**The counter-obligation is equally load-bearing**: the tutorial must state which findings *must
not* be promoted. A judgment replaced by a check that cannot express it has not been promoted; it
has been lost, and the reassuring green makes the loss hard to notice.

## Error Handling Strategy

Four states, chosen deliberately per judge:

```
PASS      checked, satisfied
FAIL      checked, not satisfied
ABSTAIN   not applicable, or nothing determinable   ← leaves the aggregation population
ERROR     could not complete                        ← the judge's problem, not the subject's
```

`ErrorPolicy.PROPAGATE` throughout: a requirement that could not be evaluated does not quietly pass.
Every assertion in the test suite is on `status()`, never `pass()` — `pass()` is false for FAIL,
ERROR and ABSTAIN alike.

## Testing Strategy

| Test class | Question |
|---|---|
| `<Judge>VerdictTest` | given these per-requirement answers, is the verdict right? Written from the rubric, never from the code |
| `<Parser>Test` | does the parser find every requirement in the document? The parser is the denominator |
| `MutationTest` | does flipping the AC8 operator flip AC8 and only AC8? |
| `RecordingReplayTest` | does every committed recording still parse and produce the documented verdict? |

Integration: one jbang config per module asserting exact expected output lines, run offline.

## Evaluation Architecture

### Recorded and live paths

Every AI judge runs through `JudgeBackends.backendFor(workspace, timeout, recording)`:

- not live → replay the committed recording
- live, and `AGENT_JUDGE_TUTORIAL_CAPTURE` names a *different* recording → replay
- otherwise → live via AgentClient, capturing if named

Recordings are verbatim agent output with a provenance header, committed as resources. A run that
refreshes one recording does not re-earn the others at twenty minutes apiece.

### Judges and what each answers

| Module | Document | Judge | Expected |
|---|---|---|---|
| 01 | — | `BuildSuccessJudge` | PASS, 290 tests |
| 02 | `manage-appointment-lifecycle/criteria.md` AC7–AC12 | `EarsJudge` | 6 / 6 |
| 03 | same file, all 52 | `EarsJudge` | 51 pass, 0 fail, 1 undetermined |
| 04 | `rules.md` — the 13 feature-wide rules | `Rfc2119Judge` | 5 pass, 8 fail, 0 undetermined |
| 05 | the FAILs from 04 | `Investigation` | consequences, ranked |
| 06 | 2 of the failed rules | `ArchUnitJudge`, `ConfigRules` | 5 and 13 violations, < 1s |

## Requirements Traceability

| Vision success criterion | Design element |
|---|---|
| 1 — rubrics authored elsewhere | DD-1, DD-2; every module reads a vendored document |
| 2 — offline | `JudgeBackends`, committed recordings, integration configs |
| 3 — real defects with addresses | modules 03–05; `Check.locations` |
| 4 — public standards, promoted | DD-4, DD-11; `EarsJudge`, `Rfc2119Judge` |
| 5 — no numeric score | DD-12; conjunctive rollup, binding requirement named |
| 6 — deterministic promotion, and what must not be | DD-15; module 06; shadow rules asserted per DD-3 |
| 7 — one concept per module | module structure, frozen at six |
| 8 — JUnit harness per module | DD-13; `judge-junit` as test-scope dependency |

## Open Questions

1. Should `Obligation` live in `agent-judge` from the start, since `AllMustPassStrategy` would need
   to understand SHOULD to aggregate correctly?
2. `Check` has no structured location field, so the reliable half of a judge's output is mixed with
   the unreliable half in one prose blob. Tutorial-local workaround now; library change later.
3. Does the investigation tier hold up with fresh agents at arm's length? Measured once, by the
   author, with full context: 7 escalations, 1 de-escalation, 0 fabrications.
4. How much of the claim-type asymmetry survives a prompt requiring numbers to come from a command?

None of these gates the conference path.

---

## Revision History

| Timestamp | Change | Trigger |
|-----------|--------|---------|
| 2026-09-07T10:20-04:00 | Initial draft | Rebuild around Anton's documents; judges named for public standards; two-tier reporting |
