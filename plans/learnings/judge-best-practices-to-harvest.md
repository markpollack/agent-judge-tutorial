# Future task: harvest this session's best practices into the judge docs

> **Prior-art triage completed 2026-09-06** by the agent-judge steward (internal corpus and
> `writing-judges.md`) and the research session (academic literature, 4 web searches at
> abstract level, no paper read in full — every negative below is bounded search, not
> established absence).
>
> **The novelty claim is much narrower than first drafted.** Read the triage before writing
> any of this up as new.

## Prior art — cite, do not claim

| Element | Status | Prior art |
|---|---|---|
| Retrieval-vs-aggregation asymmetry, as a *shape* | **not new** | RULER (Hsieh et al. 2024) separates retrieval (NIAH family) from aggregation (Common/Frequent Word Extraction). Our reading of the split is confirmed; the widely-repeated "aggregation degrades far faster" magnitude is **not** verified — check the paper before asserting it. RULER is closed-book synthetic long-context, so it is prior art for the shape, **not** for an agentic grounded setting. |
| "Numbers must come from a command" | **not new** | A re-derivation of PAL / Program-of-Thoughts — offloading arithmetic to a tool, restated for judging. Cite it as such. |
| Per-claim provenance marking | **not new** | PaperTrail (CHI 2026, arXiv 2602.21045) decomposes answer and sources into claims and maps supported / unsupported / omitted. Also GenProve (2601.04932), TROVE (2503.15289), CiteCheck (2605.27700). An established convention already marks each sentence `[n]` or `[unverified]` with coverage as the metric. |
| Counting / cardinality weakness | **not new** | Established and uncontroversial. |
| LLM-as-judge bias literature | **correctly excluded** | Position / verbosity / self-preference concern preference formation across outputs, not reliability by claim type within one. Keep the exclusion. |

### Papers closer to this setup than anything originally listed — READ BEFORE WRITING

- **arXiv 2603.00539**, *"Are LLMs Reliable Code Reviewers? Systematic Overcorrection in
  Requirement Conformance Judgement"* — requirement-conformance judgement of code, which is this
  experiment's genre almost exactly. Read first; most likely to have scooped this or to supply a
  named failure mode to build on.
- **SHERLOC, arXiv 2606.24820** — scores code-repair findings on three independent dimensions:
  root-cause correctness, **location accuracy**, and solution actionability. That is
  "reliability varies by claim type within one output", already decomposed, in code. Closest
  thing found to the localizer claim, and it partially anticipates leads-vs-findings.
- **arXiv 2606.19544**, *"Reliability without Validity"* — across 21 models, raw agreement
  overstates chance-corrected discrimination by 33-41 points, and high test-retest reliability
  masks severe position bias. The general warning that a consistent judge is not a correct one.
- **arXiv 2605.15184**, *"Is Grep All You Need? How Agent Harnesses Reshape Agentic Search"* —
  for the agentic tool-use angle.

### Where the line actually is

Two candidates survived both reviews:

1. **The conjunction, not the asymmetry.** Tools present and sufficient, task fully grounded, one
   second of `grep -c` would have settled it — **and the model estimated anyway**, emitting a wrong
   aggregate in the same sentence and the same register as a correct file-and-line citation.
   Existing work on tool underuse treats it as an architecture or decision-logic problem, not as
   *claim-type-conditional behaviour*. The steward's framing: it is not "the model cannot count",
   it is **"the model does not know which of its own claims need a tool"** — a design instruction
   rather than a limitation.
2. **Leads versus findings** (address + hypothesis, versus consequence + rank). Not found named
   anywhere by either reviewer. SHERLOC is adjacent but stops at scoring dimensions without drawing
   the operational conclusion. Sharpest candidate.

**Do not coin "point-at vs aggregate"** before checking whether *extractive vs abstractive* or
*grounded vs synthesised* already covers it. Borrowed vocabulary travels further.

## Two methodological objections that must be stated before a reviewer states them

1. **n = 2 runs on one codebase.** "100% of pointable claims correct, 0% of aggregates correct"
   across ~65 requirements is an anecdote with a striking effect size, not a result. It cannot
   separate "this is how LLM judges behave" from "this is how this model behaved on this Java
   repository on this day."
2. **The verification was unblinded and performed by the author.** Claims were hand-verified by
   the person who already knew which claim was of which type — which is precisely the
   classification being measured. Cheap fix: classify claim type mechanically (does the sentence
   carry a `file:line`? does it carry a bare number?) before verifying, or have a second party
   verify. Not yet done.

## Status of each practice after triage

| Practice | Verdict |
|---|---|
| Point-at vs aggregate asymmetry | **new in this setting**; not in `writing-judges.md`, not in the corpus. Nearest internal neighbour is §1 "Prefer evidence to opinion" (Findeis et al., ACL 2025), which is advice about *which judge to reach for* — this is a claim about one judge's *internal* reliability. Belongs in §1, converting that advice from "choose the right judge" into "constrain what the judge may assert." |
| Localizer, not assessor; output ordering carries no severity | **new** — the manual has nothing on output ordering. §5's "report the binding criterion" is about which criterion held an aggregate down, not about ranking findings. |
| Leads vs findings | **new**, and judged the most reusable item in the list. |
| Second tier: "establish consequence and reachability", never "verify this finding" | **new as stated, but write it as a second instance** of §7c, *"A proof needs a negative control, or it degenerates into a naming check."* The generalisation is stronger with two unrelated instances than as a new principle. |
| A review is worth what its rubric is worth | **already in the manual, §5** ("Conjunction is only safe over a criteria set you fixed"), built from this project's own carving measurement. The new numbers *upgrade the evidence*; the claim is already there. |
| Require a location per claim; numbers from a command | **location half is half-known** (present in older experiment-code-coverage judge-design prior art, never reached the manual); **command half follows from the asymmetry finding** and is its actionable form. Keep the two halves as one prompt rule. |

### Do not cite for this

The `bud-spring-actualize` memory — `2026-05-25-paired-phase1-complete.md`, *"2/9 runs hit
CodeQualityJudge JSON parse errors (LLM returned prose instead of JSON)"* — is a **format** failure
fixed by retry and a stronger prompt. It is prose-instead-of-JSON, not prose-claims-are-wrong.
Different failure, same word.

---


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
