# Step 3.1: Auditing 52 acceptance criteria, and the negative control that makes it mean something

Everything below was measured on the pinned candidate at `fc9df4af`, materialized and formatted,
with the live AgentClient backend. Both recordings are committed verbatim.

## The result

| Workspace | Verdict | Pass | Fail | Undetermined |
|---|---|---|---|---|
| `build/large-candidate` (as delivered) | PASS | 51 | 0 | 1 (UC6-AC41) |
| `build/seeded-candidate` (one operator changed) | FAIL | 50 | **1 (UC6-AC8)** | 1 (UC6-AC41) |

## Why the first row on its own is worth nothing

A judge that has only ever been shown working code has not been shown to work. 51 of 52 on a
well-built system is exactly what a judge that always says yes would produce, and the two are
indistinguishable from that row alone.

So the second row is the module. The seed is one comparison operator in `AppointmentService`:

```java
- if (!now.isBefore(appointment.getStartTime())) {   // rejects AT the start instant
+ if (now.isAfter(appointment.getStartTime())) {     // permits it
```

UC6-AC8 requires the rejection at the exact start instant. UC6-AC9 requires it strictly after.
**The audit flipped exactly one criterion.** AC8 went red with a correct citation to line 154 and a
correct account of the consequence; AC9 stayed green, because the seeded operator still rejects the
strictly-after case; the same single criterion stayed undetermined in both runs. That is watched
red on a judge, and it is what the first row was missing.

## The finding that justifies the whole module

**Both workspaces pass all 290 tests.**

The suite's boundary test, `testCancelAppointmentByOwnerFailsIfPastStartTime`, sets the start to
`NOW - 10 minutes`. The defect lives at exactly zero. Ten minutes away from the boundary, the test
cannot see it, and it was never going to: the test checks what somebody thought to write down, and
the criterion is what was actually asked for.

Green tests and an unmet acceptance criterion are not a contradiction. They are two different
oracles answering two different questions, and this fixture shows the gap between them without any
staging beyond a single character.

## Denominator discipline held, and was load-bearing twice

1. **The roster guard fired in practice.** The live agent answered AC1–AC46, then AC48–AC52, then
   AC47 last. Order is not part of the contract; completeness is. A parser keyed on document order
   would have reported 51 answers and a missing AC47.
2. **The all-abstain case is not vacuous.** `AllMustPassStrategy` returns ABSTAIN over an empty
   eligible population rather than PASS, and the library's own source says why. The classifier
   depends on that, so it is asserted in a test rather than assumed: a run in which nothing could be
   determined must not report that everything is satisfied.

## Two defects found in my own judge

- **A missing recording said "The auditing agent did not complete its run."** True of the run,
  false about the code. The `successful=false` check ran before the `NO_RECORDING` check, so a
  misconfigured judge read as a finding about the subject. Now it names the missing recording and
  the command that captures one.
- **`AGENT_JUDGE_TUTORIAL_CAPTURE` re-earned every other recording.** Refreshing one recording ran
  every other live judge in the process at ~20 minutes apiece. `JudgeBackends.backendFor` now
  replays what the run is not capturing.

Neither would have surfaced without running the recorded path end to end after the live capture.

## An inaccuracy in the agent's own prose

The audit's opening line claims "270 tests, 0 failures". The measured figure is **290** (53
surefire reports, 4 skipped). The verdicts were unaffected and every citation checked out, but the
number was wrong, and it is a reminder that a judge's reasoning text is unverified narration
sitting next to verified per-criterion answers. The `Check`s are the evidence; the prose is not.

## What carried forward

`SpecConformanceJudge` became `CriteriaAuditJudge`, because the capability is "answer every one of
a numbered list of written requirements against a workspace" and module 07 feeds it the 13
architectural rules instead of the 52 acceptance criteria. Same instrument, different document.
How differently it answers them is step 3.2's subject.
