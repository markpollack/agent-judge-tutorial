# Vision: Agent Judge Tutorial — Running the Spec You Already Wrote

> **Created**: 2026-09-07T10:15-04:00
> **Last updated**: 2026-09-07T10:15-04:00
> **Status**: Draft

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
| Presenter | A 10–15 minute live narrative that builds from first principles; no ceremony, no theory slides | Run modules in order, introduce one concept each, stop after any module without losing the thread |
| Conference attendee (Java developer) | "Is this real, or an LLM wrapper?"; does it work on code like mine | See real defects in real generated code, with file and line, and be able to reproduce them |
| Tutorial learner (after the talk) | A path that still works offline, months later | Run every module with no API key, inspect the committed evidence, understand why each instrument was chosen |
| Spec-driven practitioner | "I already write specs. What do I get?" | Point an existing EARS or RFC 2119 document at a judge and get a verdict, with no rewriting |
| Agent Judge maintainer | Reuse real library capability; discover product gaps without destabilising the library | Watch tutorial-local judges earn promotion into `agent-judge` by working end to end first |
| Operator / CI user | Deterministic, offline, explainable results | Run every module without model access; distinguish FAIL, ERROR and ABSTAIN; replay committed recordings |
| Reviewer acting on findings | "Which of these do I fix first, and how do I know it's real?" | Receive findings ranked by consequence, each carrying an address they can open |

## Success Criteria

1. **Every rubric in the tutorial comes from a document somebody else wrote before the code
   existed.** No criterion is authored by us, derived from a judge's own output, or fitted to a
   known answer.
2. **Every module runs offline from committed recordings**, and the live path is a single
   environment variable away.
3. **The tutorial finds real defects in real generated code**, each verified by hand and each
   citing a file and line a viewer can open.
4. Two named judges for **public requirement standards** — EARS and RFC 2119 — work on any
   conforming document, not only on this fixture, and are promoted into `agent-judge` once proven.
5. **Findings arrive ranked by consequence, not by document order**, via an investigation tier that
   turns an address into a reachability argument.
6. At least one finding is promoted into a deterministic rule that runs in under a second with no
   model, and the tutorial states plainly which findings must *not* be promoted, and why.
7. Each module introduces at most one new concept and is independently runnable with concise output.
8. The arc is honest about its own instruments: where the judge is reliable, where it is not, and
   what structure keeps the two apart.

## Scope

### In Scope

- Anton's pinned branch as the single subject under judgment, vendored and never modified.
- `EarsJudge` — reads any EARS-conformant acceptance criteria document.
- `Rfc2119Judge` — reads any MUST/SHOULD/MAY constraint document, honouring the keyword's
  obligation level as the aggregation policy.
- An investigation tier that converts each FAIL into a consequence with a reachability argument.
- Deterministic promotion: ArchUnit rules over bytecode, and file-level assertions for facts
  bytecode cannot see.
- Reuse of existing `agent-judge` capability — `BuildSuccessJudge`, `ModelBackedJudge`,
  `AgentClientJudgeModel`, `AllMustPassStrategy`, `Check`, `Judgment`.
- Committed verbatim recordings so every module replays offline and deterministically.
- Promotion of the new judges into `agent-judge` after they work end to end here.

### Out of Scope

- Modifying the vendored fixture. The subject is evidence and stays byte-identical.
- Hand-authored example changes, and any rubric derived from a judge's own output.
- Mutation testing as a teaching topic. It belongs in our test suite, not on stage.
- Code-formatting findings. One file with a cuddled `else` is not intellectually interesting and
  distracts from coding defects.
- A reusable catalogue of architectural archetypes. Desirable, not yet designed, and not required
  by this arc.
- Direct model-provider integration. Every AI judge reaches a model through AgentClient.
- Teaching agent control theory, or renaming Agent Judge, before the talk.

## Unknowns and Research Questions

1. **What do UC6's seven use-case rules return?** `manage-appointment-lifecycle/rules.md` is the
   middle rung between behaviour and feature architecture, and nothing has ever read it. If it is
   flat, the arc has two levels; if it is interesting, it has three.
2. **Does decomposing an EARS criterion into (trigger, response) produce better findings** than
   assessing the sentence whole — specifically, can it report *which half* failed?
3. **Does the investigation tier work with fresh agents and no context?** Measured once, by hand,
   with full context: 7 escalations, 1 de-escalation, 0 fabrications. Unverified at arm's length.
4. **How much of the claim-type asymmetry survives a prompt that requires numbers to come from a
   command?** We have ground truth from two runs to measure against and have not run the experiment.
5. **What is the smallest API for `EarsJudge` and `Rfc2119Judge`** that serves any conforming
   document without becoming PetClinic-specific or Anton-specific?

## Assumptions

1. Anton's branch at `fc9df4af` remains the subject; the vendored copy means upstream movement
   cannot affect us.
2. The 374 acceptance criteria are fully EARS-conformant and the 64 rules fully MUST-form —
   measured, not assumed: 28 When / 20 If / 4 While in UC6, and 20 of 20 `**MUST**` across the two
   rules files we have parsed.
3. `agent-judge` remains at or compatible with `0.16.0-SNAPSHOT`; the tutorial always builds against
   the latest snapshot.
4. A judge's per-item `Check` results are the evidence and its reasoning prose is narration. This is
   a measured property of this model on this corpus, not a law.
5. Presenting live is preferred, so every module must have a recorded path that is
   byte-for-byte reproducible and a live path that is one variable away.

## Constraints

- **Technology**: Java 21, Maven, `agent-judge` 0.16.0-SNAPSHOT, AgentClient for all model access,
  ArchUnit 1.5.0 for deterministic architecture rules. No API key required for any module.
- **Timeline**: Optimised for the JetBrains presentation, but the full learning path is implemented,
  not just the demo path.
- **Repository safety**: work on `petclinic-evidence-arc`; never modify `main` directly; never
  modify anything under `fixtures/`.
- **Presentation**: one concept per module, concise output, no noisy logs, no theory-first detours.
  Assert the search; show the result.
- **Incubation**: new judges are developed in the tutorial, proven end to end against real
  documents, and only then promoted into `agent-judge`.

---

## Revision History

| Timestamp | Change | Trigger |
|-----------|--------|---------|
| 2026-09-07T10:15-04:00 | Initial draft | Arc rebuilt around Anton's documents after rubric-fitting and staged-fixture problems were identified in the previous design |
