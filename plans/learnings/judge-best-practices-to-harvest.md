# Future task: harvest this session's best practices into the judge docs

Targets, in order of authority:

1. `~/projects/agent-judge/writing-judges.md` — the full manual
2. `~/.claude/skills/writing-agent-judges/SKILL.md` — the gate and checklist
3. `~/.claude/skills/writing-agent-judges/references/` — the expanded audits

None of the practices below are in those documents today. All of them are supported by measurements
taken in this session against the pinned `fc9df4af` candidate. Each needs its evidence carried with
it — the manual marks provenance per claim and these should be no exception.

## 1. Point-at versus aggregate

**A judge is reliable about what it can point at and unreliable about what it must aggregate.**

Measured across 65 requirements in two live runs: every file-and-line citation held up under hand
verification; every count in the surrounding prose was wrong.

| Claimed | Actual |
|---|---|
| "270 tests" | 290 |
| "six classes" re-encoding transitions | 72 inline sites |
| "eight `@PrePersist`/`@PreUpdate` callbacks" | 13 static clock reads, 12 of them callbacks |

A citation is grounded: producing it required going to the file. A count is a summary over a
population the model sampled and never enumerated. In prose the two are indistinguishable.

**Practice:** in the prompt, require a location for every claim, and require any number to come
from a command rather than from reading. In the result type, put citations in `Check`s and treat
the reasoning string as unverified narration.

## 2. The judge is a localizer, not an assessor

It answers *where* well and *how bad* badly. Corollary: its output ordering carries no information.
The architecture audit emitted findings in rule-number order — RULE-1, 2, 4, 5, 8, 10, 11, 12 —
which is document order, not severity order. The severity order established by investigation was
10, 4, 5, 12, 8, 1, 11, 2, and is not derivable from anything the judge said.

**Practice:** treat a judge's output as a set of *leads* — address plus hypothesis — not a set of
findings. A finding has a consequence and a rank, and producing those is a second job.

## 3. Ask for consequence, not verification

The framing of a second-tier investigation determines whether it does work:

- "Verify this finding" → an agent that agrees with you. Confirmation with extra steps.
- "Establish the consequence and whether it is reachable" → real work.

**The second tier needs its own negative-control discipline or it is just an expensive echo.**
Concretely: invite the downgrade explicitly, and treat a de-escalation as a success rather than a
failure of the first tier.

Evidence from the hand-run pass over eight findings: two escalated, one de-escalated, one resized
12×. The de-escalation was only reachable because the question asked whether disclosure had
*occurred*, not whether the finding was *true*. Same starting line, different answers.

## 4. A review is worth what its rubric is worth

The same judge, same code: with self-chosen criteria it was self-inconsistent (2–2 verdict split on
identical input); with 13 supplied numbered rules it answered all 13 with zero abstentions and
eight hand-verified findings.

**Specificity converts a judgment into a comparison.** A generic principle ("locks must be acquired
in a consistent order") forces the judge to infer the standard from the code being judged, which is
the carving-instability failure. A named order — Owner, Pet, Vet, Request, Appointment, Reservation
— reduces the check to a mechanical comparison with nothing left to be inconsistent about.

## 5. Every check in a pipeline should be a check the pipeline could fail

See `learnings/sdd-meta-the-gate-that-was-passed.md`. The generalisation for judge authors: enumerate
what each gate is *capable* of failing on, not what it is nominally about. A checkpoint criterion
that says "lock-order primitives are covered by focused tests" is satisfied by a correct test on a
correct primitive while every caller violates the order.

## 6. A review's value is bounded by what exists when it runs

Placing the only gate before the artifact means the gate can never be about the artifact. Reviewing
early is fine; reviewing *only* early and treating that as discharged is the failure.
