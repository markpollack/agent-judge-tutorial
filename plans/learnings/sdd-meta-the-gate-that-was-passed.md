# The SDD meta-finding: the gate was not missing, it was passed

This is not a step learning. It is what the `appointment-scheduling-spec-with-usecases` fixture
turned out to be evidence *of*, which is a different and larger thing than any module in the
tutorial. Every claim below was verified against the vendored tree at `fc9df4af`.

## The pipeline, as it actually exists in the repository

```
proposal.md      97 lines, human-written prose
      |
spec.md         126 lines
criteria.md     374 acceptance criteria across 7 use cases (UC6 has 52)
rules.md        114 lines, 13 numbered architectural rules, MUST form, each with a Reason
      |
review.md        32 lines.  Verdict: PASS.  0 blockers, 0 majors, 0 minors
      |
tasks.yaml      953 lines, 6 phases, 40 tasks, 6 human checkpoints (cp-1 .. cp-6)
      |
code            166 main Java files, 290 tests
```

## The scores

Same code, same judge, two rubrics, one afternoon:

| Rubric | Result |
|---|---|
| UC6's 52 acceptance criteria | **PASS** — 51 pass, 0 fail, 1 undetermined |
| The 13 architectural rules | **FAIL** — 5 pass, **8 fail**, 0 undetermined |

All 8 failures were verified by hand against the code. Four were verified deeply. See
`step-3.2-3.3-architecture-and-promotion.md` for the per-rule evidence.

## What I got wrong, and why the corrected version is worse

My first reading was "the review ran before the code existed, so nothing ever checked the code
against the rules." The first half is true. **The second half is false**, and I wrote it in three
places before checking `status.md`.

```
## Phase Approvals
- phase-1: APPROVED
- phase-2: APPROVED
- phase-3: APPROVED
- phase-4: APPROVED
- phase-5: APPROVED
- phase-6: PENDING
```

There are six human checkpoints with written review criteria, and five were approved. So the gate
was not missing. A person read a summary and said yes, five times.

And the criteria were **right**. They name the exact properties that failed:

| Checkpoint criterion (approved) | What shipped |
|---|---|
| cp-1: "Shared time, transition, operation, reservation, privacy, and **global lock-order primitives** are covered by focused tests" | `LockCoordinator` exposes six unordered escape hatches beside the ordered one; `lockRequest` used at 18 sites, 2 inverting the order |
| cp-3: "Staff claims, offers, direct booking, cancellation, and expiry remain **atomic under opposing owner/staff operations**" | `createStaffOffer` locks Request→Owner while `acceptGuidedHold` locks Owner→Request: ABBA on the same request |
| cp-3: "Timefold owns **every specified hard constraint**" | `exactRejectedPair` is a hard constraint the live revalidation path has never heard of |
| cp-4: "**Concurrent lifecycle actions commit at most one transition**" | `requestVersion` is threaded through four layers and read in none |
| cp-5: "Every required notification and audit event is ... **free of sensitive data**" | the full AI prompt, embedding the owner's raw text, is logged at INFO |
| cp-5: "**Maven passes the H2/MySQL/PostgreSQL migration and concurrency matrix**" | `maven-build.yml` runs `java: ['21']`; both vendor test classes skip every run |

Six correctly-worded criteria, approved, each describing something that is not true of the code.

## The single artifact that explains all of it

`LockCoordinatorTests` contains exactly one test:

```java
void locksResourcesInExactStrictGlobalOrderAndAscendingIds() { ... }
```

It is a good test. It passes. `LockCoordinator.lockResources` really does acquire
Owner → Pet → Vet → Request → Appointment → Reservation with `TreeSet`-sorted ids, and this test
proves it.

It also **fully satisfies cp-1's criterion as written**: the global lock-order primitive is
covered by a focused test.

The violation is in the *callers*, which the test does not look at, and which the criterion did
not ask about. A correct test, on a correct component, satisfying a correctly-worded criterion,
approved in good faith — while the property the criterion exists to protect is broken elsewhere in
the same codebase.

That is the whole finding in one file.

## Why the approvals were given

The `Notes` section shows what approval was based on:

```
Phase 1 verified across Maven and Gradle with 100 passing tests.
Phase 3 verified across Maven (198 passing tests) and Gradle.
Phase 4 verified across Maven (235 passing tests) and Gradle.
Phase 5 verified across Maven (255 passing tests) and Gradle.
```

A growing green suite, at every checkpoint. That is real evidence and it is evidence of the wrong
thing. **No test in that suite can fail because locks were taken in the wrong order, because a
parameter is never read, because a prompt was logged, or because a CI workflow lacks a matrix.**

The human checkpoint inherited precisely the blind spot of the automated one. The reviewer was
shown the strongest available signal, in good faith, and that signal could not bear on four of the
six criteria they were approving.

## The one that was decided, not drifted

Buried in `Deviations`:

> "Follow-up request explicitly enables full Ollama prompt and response logging at INFO,
> **overriding the original sensitive-log restriction for AI traffic**."

RULE-10's violation is not an accident. Somebody wanted to see the prompts, asked for it, got it,
and it was recorded. That is a defensible short-term decision.

What is not defensible is that it is filed in the same undifferentiated list as *"Spring Boot 4
requires `spring-boot-starter-flyway`"*. A deviation that voids an architectural rule and a
deviation about a Maven coordinate are recorded identically, and nothing links the entry back to
RULE-10 or reopens it. The register captured the decision and lost its significance.

## The one that was predicted three times

RULE-5's failure — solver and live revalidation applying different feasibility — appears in the
repository **three times before the code was written**:

1. `review.md` risk hotspot 4: *"Feasibility drift / solver and live hold validation may read
   different owner-window representations / make materialized intervals the sole shared source and
   test both paths."*
2. `rules.md` RULE-5: feasibility defined once as a pure policy, both paths applying it.
3. `tasks.yaml` cp-6 criterion 4: *"Timefold and live hold revalidation enforce the same allowed,
   preferred, and excluded intervals."*

It shipped anyway. The mitigating detail: cp-6 is the checkpoint that never ran —
`status.md` says `AWAITING_APPROVAL`. So this one specific failure had a gate designed for it that
had not yet opened. The other seven survived gates that were approved.

Note also that the note was correct and unactionable at the same time. Slash-delimited triples,
nominalised nouns, no example, no consequence a person could picture, no file named. It was
written to fill a template field called *Risk Hotspots*, and no stage of the pipeline consumes a
risk hotspot. Correct information, in a form no human would act on, with no machine that would.

## The finding, stated

Not "SDD skipped review." SDD reviewed, six ways, and approved.

**Every check in the pipeline is a check the pipeline could pass.**

- `review.md` checks the spec against itself, before code exists. It cannot be about the artifact.
- The test suite checks behaviour, and 226 of the 290 tests were written by the same pipeline that
  wrote the code, from the same premises. Zero tests written by an independent author cover the
  new feature.
- The checkpoints check what the tests report, so they inherit the test suite's blind spot.
- `rules.md` — the one artifact that describes the architecture, in numbered MUST form with
  reasons and traceability — is the only thing in the pipeline nothing consumes at all.

The rubric for the audit that would have caught eight defects was sitting in the repository the
entire time, written by the pipeline itself, and no stage was pointed at it.

## What it means for the tutorial

The arc already had "behaviour conformed, structure did not." This gives it the second half: the
process had a place for structure and it was a document, not a check. Module 07 is that document
being read back for the first time, and module 08 is deciding which parts of it should never have
needed a person again.

It also names the failure mode the tutorial exists to prevent. `review.md` is an LLM judging its
own output, with no held-out answer and no negative control, returning PASS with zero findings at
the moment of least available information. That is exactly what module 06's seeded defect rules
out for our own judge, and the reason it costs a second twenty-minute run to do it.

## Honest limits

- The branch is a snapshot mid-flight: phase 6 is pending approval, so this is not a claim about a
  finished, signed-off system.
- I cannot see who approved the phases or what they were shown beyond what `status.md` records.
- RULE-8's violation is latent: the entity boundary is absent, but the templates currently render
  only role-appropriate fields, so no disclosure has occurred.
- RULE-2's 13 violations are 12 `@PrePersist` audit stamps and one real bug. The rule as written is
  violated; the severity is not uniform and a human should decide the exemption.
