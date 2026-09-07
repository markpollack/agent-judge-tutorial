# What we know: our judges, and Anton's experiment under them

State of understanding as of 2026-09-07. Every number here was produced by a command or verified by
hand against the pinned tree. Numbers taken from a judge's own prose are marked as such and are not
trusted.

---

# Part 1 — What we know about our judges

## 1.1 Claim-type asymmetry

**A judge is reliable about what it can point at and unreliable about what it must aggregate.**

Measured across ~65 requirements in two live runs (52 acceptance criteria, then 13 architecture
rules), every finding hand-verified afterwards:

| Class of claim | Result |
|---|---|
| file-and-line citations, method names, which operator appears where | correct, no exceptions found |
| counts, prevalence, "how many classes / callbacks / tests" | wrong every time |

Observed errors: "270 tests" (actual 290); "six classes" re-encoding transitions (actual 72 call
sites across 8 classes); "eight `@PrePersist`/`@PreUpdate` callbacks" (actual 13 static clock reads,
12 of them callbacks).

The agent had shell access throughout and never ran `grep -c`. The framing that survived peer
review: **it is not that the model cannot count, it is that the model does not know which of its own
claims need a tool.** That makes it a design instruction, not a limitation.

⚠️ Limits: n = 2 runs, one codebase, one model, and the verification was unblinded and performed by
the author. Striking effect size, anecdotal status.

Prior art (see `judge-best-practices-to-harvest.md`): the retrieval-vs-aggregation *shape* is RULER
(closed-book synthetic long-context, not agentic); the "numbers from a tool" fix is PAL /
Program-of-Thoughts; per-claim provenance marking is PaperTrail and others. What appears unclaimed
is the **conjunction** — tools present and sufficient, task grounded, and the model estimated anyway.

## 1.2 The judge is a localizer, not an assessor

It answers *where* well and *how bad* badly. Corollary with evidence: its output ordering carries no
severity information. The architecture audit emitted RULE-1, 2, 4, 5, 8, 10, 11, 12 — document
order. The severity order after investigation was 10, 4, 5, 12, 8, 1, 11, 2, and is not derivable
from anything the judge said.

**Treat a judge's output as leads (address + hypothesis), not findings (consequence + rank).**

## 1.3 Rubric carving instability, and the fix

Left to choose its own criteria, the conformance judge agreed on the facts every run and disagreed
on how to carve them:

| | before | after |
|---|---|---|
| criteria per run | 5, 6, 5 | identical, 5 of 5 runs |
| criterion names shared by all runs | **0** (16 distinct across 3) | 5 of 5 |
| verdict on identical input | 2 PASS / 2 FAIL (n=4) | 5 FAIL / 5 |
| runtime | 24-117s | ~24s |

The fix is two parts and both are required: **supply the criteria**, and **take the verdict away from
the model** (per-criterion `Judgment`s rolled up by `AllMustPassStrategy` in Java).

**Specificity converts a judgment into a comparison.** A generic principle forces the judge to infer
the standard from the code it is judging; a named standard reduces the check to a comparison with
nothing left to be inconsistent about.

## 1.4 Denominator discipline, and where it actually fired

- **Roster guard, load-bearing in practice.** The live agent answered UC6-AC1 through AC46, then
  AC48-AC52, then AC47 last. A parser keyed on document order would have reported a missing answer.
  A criterion the audit skipped is not a criterion that passed.
- **The empty case is not vacuous.** `AllMustPassStrategy` returns ABSTAIN over an empty eligible
  population, never PASS — verified in the library source, and asserted in a test rather than
  assumed. A run in which nothing could be determined must not report that everything is satisfied.
- **Abstentions leave the aggregation**, so the count of undetermined criteria is carried in the
  reasoning and the metadata, or a requirement nobody could check silently stops affecting the
  verdict.

## 1.5 Four outcome states, and blaming the right party

A missing recording originally rendered as *"The auditing agent did not complete its run"* — true of
the run, false about the code. The judge's own misconfiguration read as a finding about the subject.
Now it names the missing recording and the command that captures one.

⭐ **An unrunnable judge is an ERROR about the judge, never a FAIL about the subject.**

## 1.6 Negative controls

A judge that has only ever been shown working code has not been shown to work. Two forms, both used:

- **Seed a defect you understand.** One operator in `AppointmentService`
  (`!now.isBefore(start)` → `now.isAfter(start)`) flipped exactly one criterion — UC6-AC8 — while
  AC9 stayed green and the same single criterion stayed undetermined. 290 tests pass in both
  workspaces.
- **Fix what it found.** The conventions judge fails the real city-search change on
  `persistence-tests`; adding the missing `ClinicServiceTests` case should turn it green. This is
  the better demonstration for the tutorial because it is what a developer would do anyway.

## 1.7 The second tier

Framing decides whether investigation does work:

- *"Verify this finding"* → an agent that agrees with you.
- *"Establish the consequence and whether it is reachable"* → real work.

**The second tier needs its own negative-control discipline or it is an expensive echo.** Measured
over eight parallel investigations of the eight architectural findings:

**7 escalate, 1 de-escalate, 0 confirmed-as-stated, 0 fabrications found on spot-check.** Four
downgrades occurred *inside* escalations. Details in `tier-two-fanout-experiment.md`.

## 1.8 Absolute versus comparative questions

```
ABSOLUTE      "UC6-AC5: adjacent appointments must not overlap"
              answerable from the artifact alone      → goal + workspace

COMPARATIVE   "follow the conventions already in the codebase"
              meaningless without a before state      → goal + workspace + baseline
```

`JudgmentContext` is `(goal, workspace, executionTime, startedAt, agentOutput, status, error,
metadata)` — **no baseline field**. Comparative judges receive it through `metadata`, and
`CoveragePreservationJudge` shows the correct pattern: absent baseline → **ABSTAIN**, never PASS.

This is why written acceptance criteria scale better than "follow the conventions": 374 written
criteria are all absolute.

## 1.9 The judges that exist

| Judge | Where | Model? |
|---|---|---|
| `BuildSuccessJudge` | agent-judge | no |
| `CoveragePreservationJudge` / `CoverageImprovementJudge` | agent-judge | no |
| `ArchitecturalConformanceJudge` | tutorial, 5 fixed criteria | yes |
| `CriteriaAuditJudge` | tutorial, any numbered requirement document | yes |
| `ArchUnitJudge` | tutorial, promoted rules over bytecode | no |
| `ConfigRules` | tutorial, properties and workflow files | no |

`CriteriaAuditJudge` is the promotion candidate: "answer every one of a numbered list of written
requirements against a workspace" is a general capability, and it serves two different documents
unchanged.

## 1.10 Known gaps in our own instruments

1. **`Check.message` is an undifferentiated blob.** The model's evidence text carries both the
   reliable address and the unreliable count, with nothing distinguishing them. It should be
   structured — `locations[]` and `narrative` as separate fields.
2. **The format is obtained by persuasion.** `ClaudeAgentOptions` supports `jsonSchema`;
   `AgentClientJudgeModel` does not expose it. The fixed criteria set is currently requested in a
   prompt and parsed with a tolerant regex.
3. **No baseline in `JudgmentContext`.** Comparative judging goes through untyped metadata.

---

# Part 2 — The subjects

| Fixture | Provenance | Size |
|---|---|---|
| `baseline/` | `antonarhipov/spring-petclinic-fork` @ `88e37c15`, upstream PetClinic on Boot 4.1.0 | 17 test classes, **71 tests** |
| `city-search.patch` | written for this tutorial | 4 files, 85 lines |
| `appointment-scheduling-spec-with-usecases/` | same fork @ `fc9df4af`, Anton's SDD branch | **166 main files, 290 tests** |
| `build/seeded-candidate` | the above, one comparison operator changed | 290 tests, all green |

Both trees byte-identical to upstream, imported by `git archive`, no `.git` carried.

**Test composition of the generated system**, measured from 53 surefire reports:

```
 73  (17 classes)  inherited PetClinic classes — but the agent edited 5 of them, +9 / -7
217  (36 classes)  wholly new

111  Mockito unit          mocks, no Spring, no database
110  @WebMvcTest           MockMvc slice, services mocked
 41  plain JUnit
 17  @DataJpaTest          real H2
 11  @SpringBootTest       (4 skipped: MySQL + Postgres Testcontainers)
```

**24 of 290 tests execute against a real database**, all H2. **226 of 290 were written or edited by
the pipeline that wrote the code.** Independent human-authored tests covering the new feature: **0**
— the 64 untouched inherited tests all cover the original owner/pet/vet domain.

The agent deleted four inherited `VisitControllerTests` cases and replaced them with two asserting
the removed routes are gone.

---

# Part 3 — Anton's SDD method, as vendored

The method itself ships in the branch at `.claude/skills/`.

```
proposal → spec → criteria → rules → review → tasks → execute
  human     01      02        03      04       05       06
```

| Skill | Role | Lines |
|---|---|---|
| `01-spec` | interviews the user one question at a time down a decision tree | 78 |
| `02-criteria` | acceptance criteria in **EARS** form, traceable to behaviours | 147 |
| `03-rules` | design decisions and constraints. *"Constraints specify HOW the system should be built, complementing acceptance criteria which specify WHAT"* | 198 |
| `04-review` | stress-tests the spec artifacts against each other **and the existing codebase** | 220 |
| `05-tasks` | ordered, atomic, AC-traceable task list | 166 |
| `06-execute` | task by task, validates against ACs, applies `covers.rules`, halts at checkpoints | 228 |
| `grilling` / `grill-me` | adversarial design-tree interview, worked in rounds | 28 / 7 |

## Artifacts produced

```
spec/status.md                    progress; the only file 06-execute may write
spec/smart-appointment-scheduling/
  proposal.md    97 lines   human prose
  spec.md       126         summary, use cases, cross-cutting boundary decisions
  rules.md      114         13 numbered RULE-N, MUST form, each with Reason + Covers
  review.md      32         the gate: PASS, 0 blockers / 0 majors / 0 minors
  tasks.yaml    953         6 phases, 40 tasks, 6 checkpoints
  <7 use cases>/            each: spec.md, criteria.md, rules.md
```

## The traceability chain

```
proposal.md   prose
  ↓
UC spec.md    "Behaviors to verify"                    UC6-B4
  ↓
criteria.md   EARS criteria, "Covers: UC6-B4"          UC6-AC5
              374 total (51/48/83/52/45/48/47)
              UC6's 52: 28 "When", 20 "If", 4 "While"
  ↓
rules.md      13 RULE-N, "Covers: <AC ranges>"
  ↓
tasks.yaml    40 tasks, covers: {acs: [...], rules: [RULE-9, RULE-11]}
  ↓
code
```

Stronger than most human teams maintain. `review.md` asserts all 300 behaviour IDs resolve from the
374 criteria (its number, unverified by us).

## Where the gates are, and what they can see

- `04-review` examines spec artifacts and the *pre-existing* codebase. The feature's code does not
  exist yet. **This is a position, not a flaw** — do not call the document deficient for naming no
  source files.
- `06-execute` validates each task against **its acceptance criteria** — the WHAT.
- Constraint compliance — the HOW — is reported per checkpoint as
  **"Constraints (this phase's RULES): n/n satisfied"**, asserted by the agent that wrote the code,
  and **`status.md` retains none of those counts.**
- Phase approvals: 1-5 `APPROVED`, 6 `PENDING`. The record carries **no actor, no date, no evidence
  link** — we cannot say who or what marked them, or on what basis.

**The finding, with no appeal to anyone's state of mind:** every check this pipeline records is one
the pipeline itself produced the evidence for. The rubric that would catch the defects existed, was
mapped onto every task, and was reported against — by the implementer, about itself.

---

# Part 4 — Every pass we ran, and what it returned

| Pass | Instrument | Subject | Result |
|---|---|---|---|
| build | `BuildSuccessJudge` | large candidate, as vendored | **FAIL** — spring-javaformat, 1 file, 28 lines, while 290 tests pass |
| conventions | AI, 5 fixed criteria | city-search change | **FAIL** — 4/5; `persistence-tests`: the new finder is mocked in all 3 new tests, never executed |
| spec conformance | AI, 52 EARS criteria | large candidate | **PASS** — 51 pass, 0 fail, 1 undetermined (UC6-AC41) |
| spec conformance | same | seeded candidate | **FAIL** — 50 pass, 1 fail (UC6-AC8 alone), 1 undetermined |
| architecture | AI, 13 rules | large candidate | **FAIL** — 5 pass, 8 fail, 0 undetermined |
| ArchUnit promoted | bytecode, no model | large candidate | RULE-1 → 5 violations; RULE-2 → 13 (scoped; 16 unscoped, 3 in legacy code). 180 classes, <1s |
| ArchUnit shadows | bytecode, no model | large candidate | RULE-4 shadow **green** on broken code; RULE-8 shadow 2 false positives, 5 real violations missed |
| config rules | file reads | large candidate | RULE-9 **PASS**; RULE-11 **FAIL** |
| tier two | 8 parallel agents | the 8 findings | 7 escalate, 1 de-escalate, 0 confirmations |

## The eight architectural findings, after investigation

Ordered by severity established in tier two, not by rule number.

| Rule | Finding | Reachable |
|---|---|---|
| **10** | raw owner text + raw AI response logged at INFO, unconditionally, even when Ollama is down. `RetentionService` cannot reach logs and `full_ai_response` is never persisted, so **the log is the only durable copy** and the 30-day `DATA_PURGED` attestation is false. A test at `AIInterpretationClientTests:252-255` **asserts the text is present**, inverting the guardrail `tasks.yaml:172` required | yes |
| **4** | `StaffFallbackService` locks Request before Owner/Pet/Vet at `:248`/`:258` and `:336`/`:346`, inverting the mandated order against 13 correct sites — ABBA deadlock with `ReservationService.acceptGuidedHold:243`. No retry, no timeout, no test, no logging | same request row only |
| **12** | `rejectGuidedHold` checks only `REJECTED`; accept → Back → reject orphans a BOOKED appointment, and re-matching yields a second row on a `request_id` with **no UNIQUE constraint in any vendor**, permanently breaking `Optional findByRequestId` | yes, one user, no race |
| **5** | two of eleven solver hard constraints unenforced on transactional paths: `exactRejectedPair` absent entirely, and owner ALLOWED/EXCLUDED windows skipped when `excludeRequestId` is null — which `rescheduleAppointment` passes | yes |
| **1** | no transition policy exists; 5 unguarded public status setters, **72 inline assignments** across 8 classes and 5 enums. Two shipped defects: `STAFF_OFFERED` is read by 6 guards and **written only by a test**; `ownerHorizonEnd` unset on 2 intake paths | yes |
| **11** | `maven-build.yml` runs `java: ['21']` only; the 4 skipped tests are exactly MySQL + Postgres. Gradle *also* violates clause 2 by re-running them. The concurrency dimension does not exist for non-H2 anywhere | yes |
| **8** | 7 sites bind JPA entities into templates; 2 DTOs shared across roles. **Latent** — zero staff-only fields render in owner views, `fullAiResponse` never populated. One live leak: staff usernames via `AuditRecordDto` at `requestTimeline.html:26` | conditional |
| **2** | 13 static clock reads; 12 are `@PrePersist` audit stamps (6 of them dead fallbacks). The one that matters: `UserPrincipal:93` reads the account-lock deadline off the system clock while `AccountService:243` writes it clock-derived — **fail-open**, zero tests, untestable without static mocking | yes |

## The promotion carve

```
ArchUnit          RULE-1, RULE-2      structural, decidable from bytecode, caught real violations
file assertions   RULE-9, RULE-11     deterministic, and not ArchUnit's business
stays with judge  the other 9         including the two most serious findings
```

A finding earns promotion when it is decidable from bytecode, caught something real, **and its
mechanised form covers the finding rather than a convenient shadow of it.** The third test
disqualifies most candidates. Scope is load-bearing: the unscoped time rule reported 16, three of
them in legacy PetClinic code the rule does not govern.

---

# Part 5 — Open and unverified

- `review.md`'s "300 behaviour IDs" is its own number; we have not checked it.
- We cannot establish who or what wrote the five `APPROVED` lines, or what was shown to them.
- Authorship of `rules.md` (model-generated versus hand-written) is not recorded and **is not
  material** — it is part of Anton's package, produced by his method, most likely AI-assisted under
  his direction. Treat every spec artifact as his. The only place the distinction would matter is
  a conversation with him about which design calls were his judgment.
- Every absence claim here is bounded search of the vendored tree.
- Tier-two severity labels are the agents' own and were not independently calibrated.
- No repeat runs anywhere, so nothing here speaks to variance.
