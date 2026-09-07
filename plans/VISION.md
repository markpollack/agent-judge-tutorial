# Vision: Agent Judge Tutorial — Running the Spec You Already Wrote

> **Created**: 2026-09-07T10:15-04:00
> **Last updated**: 2026-09-07T14:05-04:00
> **Status**: Draft
> **Immediate deadline**: JetBrains recording, 2026-09-08 11:00

## Problem Statement

Spec-driven development produces, as a side effect, the most valuable evaluation asset a project
will ever have: a numbered, traceable, machine-readable statement of what the system must do and
how it must be built. Nobody runs it as one.

We have direct evidence. Anton Arhipov's `appointment-scheduling-spec-with-usecases` branch of
Spring PetClinic carries **438 numbered requirements across 15 markdown documents** — 374 EARS
acceptance criteria, 51 use-case technical constraints, and 13 feature-wide architectural rules —
all written before the code, all with stable identifiers and traceability links. The pipeline that
produced them also produced 166 Java files and 290 passing tests, and passed its own review with
zero findings.

Pointing a judge at two of those fifteen documents found eight architectural violations, every one
of which we verified by hand: an ABBA lock-ordering deadlock, raw owner text written to the
application log, an optimistic-concurrency guard threaded through four layers and read in none.

The requirements were in the repository the whole time. Nothing independent ever read them back
against the code.

There is a second, deeper problem the first one exposes. When an AI system reports a finding, the
report is prose — and **a claim that was measured and a claim that was guessed are typographically
identical.** Our own judge cited files and lines with perfect accuracy across 65 requirements in two
runs, and got every count in its surrounding prose wrong: "270 tests" for 290, "six classes" for 72,
"eight callbacks" for 13. It had a shell open the entire time and never ran `grep -c`.

That is the failure this tutorial exists to teach engineers to design around: **plausible is not
true, and confidence is not evidence.** A judge must be built so that what it asserts can be
checked without trusting it.

## Stakeholders and Personas

| Persona | Top concerns | What they must be able to DO |
|---------|--------------|------------------------------|
| Presenter | A short live narrative that builds from first principles; no ceremony, no theory slides | Run modules 01–04 in order, introduce one concept each, stop after any module without losing the thread |
| Conference attendee (Java developer) | "Is this real, or an LLM wrapper?"; does it work on code like mine | See real defects in real generated code, with file and line, and be able to reproduce them |
| Tutorial learner (after the talk) | A path that still works offline, months later | Run every module with no API key, inspect the committed evidence, understand why each instrument was chosen |
| Spec-driven practitioner | "I already write specs. What do I get?" | Point an existing EARS or RFC 2119 document at a judge and get a verdict, with no rewriting |
| Agent Judge maintainer | Reuse real library capability; discover product gaps without destabilising the library | Watch tutorial-local judges earn promotion into `agent-judge` by working end to end first |
| Operator / CI user | Deterministic, offline, explainable results | Run every module without model access; distinguish FAIL, ERROR and ABSTAIN; replay committed recordings |
| Reviewer acting on findings | "Which of these do I fix first, and how do I know it's real?" | Receive findings carrying an address they can open, and a consequence they can act on |

## The Teaching Progression

Code-first. The concepts are taught by executable Java, never by a theory section.

```
The agent says "done."
        ↓
Does it build?
        ↓
The tests pass — but is that the whole definition of done?
        ↓
Anton already wrote acceptance criteria before the code.
        ↓
Run 6 of them.
        ↓
Run all 52.
        ↓
Now run the architectural MUSTs.
        ↓
Behaviour can conform while architecture does not.
        ↓
A FAIL is an address.
        ↓
Investigate the consequence.
        ↓
If the rule can be mechanised, remove the model from that job permanently.
```

## Evaluation Versus Experimentation

Two disciplines, deliberately separated. This tutorial builds only the first.

**Agent Judge** answers: *what does good mean, and did this run meet that bar?*

**Agent Experiment** answers: *holding that bar fixed, which intervention actually improves the
system?*

```
run → record → evaluate against a fixed bar → diagnose
                                                  ↓
                              change ONE thing (model, prompt, skill,
                              toolset, workflow step, context strategy)
                                                  ↓
                                  rerun against the SAME bar
```

The judge and its rubric are **not** changed at the same time as the intervention. If both the
agent and the definition of success move, there is no controlled experiment.

> **Self-improvement without a stable external measure is self-modification, not demonstrated
> improvement.**

This matters for spec-driven development generally. An `execute ↔ converge` loop is a useful shape,
and Anton's specifications already supply much of the external bar it needs. The complementary
question this tutorial answers is: *converge toward what, measured by whom, against what fixed
evidence?* The refinement is to make evaluation external and fixed while the agent changes:

```
execute → evaluate against fixed specification → diagnose deviation
       → choose an intervention → change skill / prompt / model / workflow → execute again
```

Agent Experiment is the downstream discipline. It is **not** an implementation dependency of this
tutorial, and it is out of scope here.

## Success Criteria

1. **Every rubric in the tutorial comes from a document Anton's pipeline wrote before the code
   existed.** No criterion is authored by us, derived from a judge's own output, or fitted to a
   known answer.
2. **Every module replays offline from a committed recording**, deterministically, with no API key,
   from a clean clone. The live path is one environment variable away.
3. **The tutorial finds real defects in real generated code**, each verified by hand and each citing
   a file and line a viewer can open.
4. Two judges for **public requirement standards** — EARS and RFC 2119 — read Anton's existing
   documents without those documents being rewritten, and are promoted into `agent-judge` once
   proven end to end.
5. **No numeric score, mean, percentage or rating appears in any acceptance decision.** Every
   requirement stays individually visible and a failure names the binding requirement and its
   location.
6. **Deterministic promotion is demonstrated**: at least one finding leaves the model path entirely
   and becomes a rule that runs in under a second, and the tutorial states plainly which findings
   must *not* be promoted, and why.
7. Each module introduces at most one new concept and is independently runnable with concise output.
8. Every module's judge also runs inside a JUnit test, so the tutorial shows the ordinary way a team
   would adopt this.

## Scope

### In Scope — the frozen six-module arc

```
module-01-build          does it build?                                BuildSuccessJudge, no model
module-02-ears-slice     six readable EARS criteria from UC6          EarsJudge
module-03-ears-usecase   all 52 UC6 EARS criteria                     EarsJudge, PASS requires all established
module-04-rfc2119-rules  the 13 feature-wide architectural MUSTs      Rfc2119Judge
module-05-investigation  addresses become consequences                fan-out
module-06-promotion      mechanisable findings leave the model path   ArchUnit / Java / file assertions
```

Modules 01–04 are the conference path. Modules 05 and 06 deepen the idea and must not be
prerequisites for 01–04.

- Anton's pinned branch as the single subject under judgment, vendored and never modified.
- `EarsJudge` and `Rfc2119Judge`, reading his documents verbatim.
- An investigation tier converting a FAIL into a consequence with a reachability argument.
- Deterministic promotion: ArchUnit over bytecode, and file-level assertions for facts bytecode
  cannot see.
- Reuse of existing `agent-judge` capability — `BuildSuccessJudge`, `ModelBackedJudge`,
  `AgentClientJudgeModel`, `AllMustPassStrategy`, `Check`, `Judgment`. **`BuildSuccessJudge` is
  reused, not reimplemented.**
- `judge-junit` as the thin JUnit bridge for every arc module.
- Committed verbatim recordings so every module replays offline.
- Promotion of the new judges into `agent-judge` after they work end to end here.

### Out of Scope

- **Architectural shape detection, a reusable archetype catalogue, rules we author ourselves, and
  remediation generation.** Any rubric that does not already exist in Anton's repository is out.
- Modules beyond the six above.
- Modifying the vendored fixture. The subject is evidence and stays byte-identical.
- Hand-authored example changes, and any rubric derived from a judge's own output.
- Mutation testing as a teaching topic. It belongs in our test suite, not on stage.
- Code-formatting findings. One file with a cuddled `else` is fixed by one IDE command; it is not
  intellectually interesting and it distracts from coding defects.
- Agent Experiment implementation.
- Direct model-provider integration. Every AI judge reaches a model through AgentClient.
- Competing with JUnit, ArchUnit, JaCoCo, JApiCmp, Checkstyle, static analysis or the compiler. The
  guiding rule is **use the least interpretive instrument that can reliably answer the question**.

## Unknowns and Research Questions

Only questions that bear on Acts 1 and 2 remain. None of them gates the conference path.

1. **How much of the claim-type asymmetry survives a prompt that requires numbers to come from a
   command?** We have ground truth from two runs to measure against.
2. **Does the investigation tier hold up with fresh agents at arm's length?** Measured once, by the
   author, with full context: 7 escalations, 1 de-escalation, 0 fabrications. Unverified blind.
3. **What is the smallest API for `EarsJudge` and `Rfc2119Judge`** that serves any conforming
   document without becoming PetClinic-specific or Anton-specific?
4. **Does `Check` need a structured location field** in `agent-judge`, given that the reliable half
   of a judge's output is currently mixed with the unreliable half in one prose blob?

## Assumptions

1. Anton's branch at `fc9df4af` remains the subject; the vendored copy means upstream movement
   cannot affect us.
2. The acceptance criteria are fully EARS-conformant and the feature rules fully MUST-form —
   measured, not assumed: 28 When / 20 If / 4 While in UC6, and 13 of 13 `**MUST**` in `rules.md`.
3. `agent-judge` remains at or compatible with `0.16.0-SNAPSHOT`; the tutorial always builds against
   the latest snapshot.
4. A judge's per-item `Check` results are the evidence and its reasoning prose is narration. This is
   a measured property of this model on this corpus, not a law.
5. Presenting live is preferred, so every module must have a recorded path that is byte-for-byte
   reproducible and a live path that is one variable away.

## Constraints

- **Technology**: Java 21, Maven, `agent-judge` 0.16.0-SNAPSHOT, AgentClient for all model access,
  ArchUnit 1.5.0 for deterministic rules. No API key required for any module.
- **Timeline**: the JetBrains recording is 2026-09-08 11:00. Modules 01–04 must be finished,
  offline-replayable and rehearsed before then. Nothing outside that path may block it.
- **Repository safety**: work on `petclinic-evidence-arc`; never modify `main` directly; never
  modify anything under `fixtures/`.
- **Presentation**: one concept per module, concise output, no noisy logs, no theory-first detours.
  Assert the search; show the result.
- **Incubation**: new judges are developed in the tutorial, proven end to end against real
  documents, and only then promoted into `agent-judge`.

## Future Directions

Broader architectural knowledge — shape detection, a reusable archetype catalogue whose rules
transfer between codebases, and remediation generation — may be reconsidered **after Acts 1 and 2
are implemented and measured**. We deliberately do not know yet what the right next step is, and
committing to one before the current arc has produced evidence would repeat the mistake this
revision corrects.

---

## Revision History

| Timestamp | Change | Trigger |
|-----------|--------|---------|
| 2026-09-07T14:05-04:00 | Scope reduction: Act 3 removed entirely; six-module arc frozen; middle-rung and EARS A/B unknowns removed; evaluation-vs-experimentation boundary added | Review pass ahead of the 2026-09-08 recording |
| 2026-09-07T10:15-04:00 | Initial draft | Arc rebuilt around Anton's documents after rubric-fitting and staged-fixture problems were identified |
