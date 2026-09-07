# Roadmap: Running the Spec You Already Wrote

> All module paths are relative to `case-studies/spec-driven-petclinic/`.

> **Created**: 2026-09-07T10:40-04:00
> **Last updated**: 2026-09-07T14:25-04:00
> **Design version**: 2026-09-07T14:15-04:00

---

# ⚠️ Conference critical path — September 8, 11:00

**Everything in this box must be finished, offline-replayable and rehearsed. Nothing outside it may
block it.**

- [ ] **Module 01 — build.** `BuildSuccessJudge` reused from `agent-judge`, materialized candidate,
      **PASS**. No formatting content. The candidate's 290 existing tests are *provenance*, stated
      separately — the judge does not discover or report test counts.
- [ ] **Module 02 — six EARS criteria.** UC6-AC7…AC12 read verbatim from
      `manage-appointment-lifecycle/criteria.md`. Every criterion and every result readable on a
      slide. Expect 6/6.
- [ ] **Module 03 — all 52 EARS criteria.** Same file, same judge, larger scale. Completeness
      enforced by the roster guard. Expect 51 PASS / 0 FAIL / 1 ABSTAIN, and an **overall verdict of
      ABSTAIN** — cannot establish UC6-AC41 (DD-7). PASS means every requirement was affirmatively
      established.
- [ ] **Module 04 — the 13 feature-wide MUSTs.** `rules.md`, `Rfc2119Judge`. Expect 5 pass / 8 fail
      / 0 undetermined. **This is where behavioural success and structural failure separate.**

Each must: replay offline from a committed recording · run in under 10 seconds · require no API key
· work from a clean clone · print concise output with no numeric score · introduce at most one new
concept.

**If time permits, and only then**: module 05 (investigate one or more real failures) and module 06
(deterministic promotion). They deepen the idea and are especially valuable if ready — but modules
01–04 must not depend on them.

---

## Overview

Two acts, six modules, built in dependency order.

**Act 1 — read the documents they already wrote.** Anton's branch ships numbered requirements in two
public formats. `EarsJudge` reads his acceptance criteria; `Rfc2119Judge` reads his architectural
MUSTs. Neither document is rewritten.

**Act 2 — turn findings into evidence, then into policy.** A FAIL is an address. The investigation
tier turns it into a consequence with a reachability argument. The mechanisable minority becomes
deterministic rules that never need a model again.

There is no Act 3. Architectural knowledge we would have to author ourselves is out of scope; see
*Future Directions* in `VISION.md`.

Every judge runs inside a JUnit test as well as a demo `main()`. The test is what a real project
would write; the demo is what a stage needs.

> **Before every commit**: verify ALL exit criteria for the current step are met. Do NOT remove exit
> criteria to mark a step complete — fulfill them.

> ⚠️ **Until modules 01–04 are done, generic elegance is subordinate.**
> **First make Anton's two actual documents work end to end. Refactor only when the second
> implementation proves what the common abstraction needs to be.**
> If `Requirement`/`Obligation` starts consuming time, do not perfect it — two concrete judges that
> work beat one elegant abstraction that is not finished. `Rfc2119Judge`'s synthetic `SHOULD`
> behaviour is good library design and is **not** needed to show Anton's 13 MUSTs tomorrow.

---

## Stage 1: Consolidate and freeze the evidence

### Step 1.0: Design review

**Entry criteria**:
- [ ] Read `plans/VISION.md`, `plans/DESIGN.md`
- [ ] Read `plans/learnings/what-we-know.md`

**Work items**:
- [ ] REVIEW design against vision success criteria
- [ ] VERIFY every claimed measurement in DESIGN.md is reproducible by a command
- [ ] CONFIRM the six-module arc appears identically in all three documents

**Exit criteria**:
- [ ] Create `plans/learnings/step-1.0-design-review.md`
- [ ] Update `ROADMAP.md` checkboxes

---

### Step 1.1: Consolidate the repository

**Entry criteria**:
- [ ] Step 1.0 complete

**Work items**:
- [x] PRESERVE the existing progressive tutorial modules at their historical repository-root locations. Do **not** renumber them, move them to `attic/`, or rewrite their contents
- [x] MOVE only the case-study modules and their case-study-specific support, fixtures, recordings and integration configs into `case-studies/spec-driven-petclinic/`
- [x] KEEP the case study out of the root reactor — module 01 runs a real 40-second build and an ordinary fundamentals build should not pay for it
- [x] EXTRACT shared parsers, `JudgeBackends`, `RecordedJudgeModel` and `Candidate` into the case study's `tutorial-support/`
- [x] VERIFY both builds: the original tutorial at root, and the case study via `-f case-studies/spec-driven-petclinic/pom.xml`
- [x] UPDATE the root README so neither path looks abandoned; give the case study its own README
- [x] COMMIT as one clearly structural change

**Exit criteria**:
- [x] Original tutorial present, unrenumbered, not in `attic/`
- [x] Case-study numbering is local to the case study
- [ ] Full offline build green
- [ ] `fixtures/` untouched
- [ ] Create `plans/learnings/step-1.1-consolidation.md`

---

## Stage 2: `EarsJudge` and modules 01–03

### Step 2.0: `Requirement` and `Obligation`

**Work items**:
- [ ] CREATE `Requirement` interface: `id()`, `title()`, `asPrompt()`, `obligation()`
- [ ] CREATE `Obligation` enum: `MUST`, `MUST_NOT`, `SHOULD`, `SHOULD_NOT`, `MAY`, `ACCEPTANCE`
- [ ] DOCUMENT on `Obligation` that it carries the aggregation policy, not merely a label

**Exit criteria**:
- [ ] Javadoc states why obligation is read from the document rather than chosen by us
- [ ] Create `plans/learnings/step-2.0-requirement-model.md`

---

### Step 2.1: `EarsJudge`

Implement the **smallest honest** judge that reliably evaluates Anton's existing EARS requirements.
If parsing the template into trigger and response makes the implementation better, keep it. **Do not
run an A/B comparison between prompt strategies** — that is not a prerequisite for anything here.

**Work items**:
- [ ] CREATE `EarsCriterion.from(Path)` parsing `### <ID>: <title>`, `**Covers:**`, and the
      requirement sentence
- [ ] ASSERT the parser finds 52 in UC6 and classifies 28 `When` / 20 `If` / 4 `While`
- [ ] CREATE `EarsJudge` over `ModelBackedJudge`: roster guard, verdict computed in Java, one `Check`
      per criterion, no numeric score
- [ ] IMPLEMENT the DD-7 rollup — ERROR > FAIL > ABSTAIN > PASS. An unestablished required criterion
      must **not** be absorbed into a passing population
- [ ] WRITE verdict tests **from the rubric, not the code**: all-pass, one-fail, unanswered-is-ERROR,
      all-abstain-is-ABSTAIN, repeated-answer-counts-once, out-of-order-answers-still-complete,
      missing-recording-blames-the-judge, **one-abstain-makes-the-whole-ABSTAIN**
- [ ] REPLAY the committed `spec-conformance-uc6` recording and assert 51/52
- [ ] ADD the mutation assertion to the test suite (flip the AC8 operator, assert AC8 and only AC8
      flips) — a test, never a module (DD-10)

**Exit criteria**:
- [ ] Every test asserts `status()`, never `pass()`
- [ ] No numeric score in any output
- [ ] Create `plans/learnings/step-2.1-ears-judge.md`

---

### Step 2.2: Modules 01, 02, 03

**Work items**:
- [ ] CREATE `module-01-build/` — reuse `BuildSuccessJudge` from `agent-judge`; **do not reimplement it**.
      Materialized tree, no formatting content (DD-9). Closes on the open question: the build is
      green and it has no opinion on whether the agent did what was asked
- [ ] CREATE `module-02-ears-slice/` — UC6-AC7…AC12, `EarsJudge`, every criterion printed, expect 6/6
- [ ] CREATE `module-03-ears-usecase/` — all 52; expect 51 PASS / 0 FAIL / 1 ABSTAIN, overall **ABSTAIN**, naming UC6-AC41
- [ ] FOR EACH: a concise demo `main()` **and** a JUnit test using `JudgeAssertions` that runs the
      same judge and asserts the same verdict (DD-13)
- [ ] ADD `judge-junit` as a test-scope dependency of each
- [ ] ADD one integration-testing config per module with exact expected output lines, including a
      guard that no score appears

**Exit criteria**:
- [ ] All three replay offline, deterministically, in under 10 seconds each
- [ ] Each has a passing JUnit test that would fail the build if the verdict changed
- [ ] Create `plans/learnings/step-2.2-modules-01-03.md`

---

## Stage 3: `Rfc2119Judge` and module 04

### Step 3.0: `Rfc2119Judge`

**Work items**:
- [ ] CREATE `Rfc2119Constraint.from(Path)` — parses `### <ID>`, `**Covers:**`, `**<KEYWORD>**`,
      `**Reason:**`; the `**Reason:**` travels with the rule into the prompt
- [ ] ASSERT the parser finds 13 in `rules.md`
- [ ] CREATE `Rfc2119Judge`: roster guard, verdict in Java, one `Check` per constraint
- [ ] IMPLEMENT obligation-aware rollup — `MUST` violations fail the gate; `SHOULD` violations are
      reported and do not
- [ ] WRITE verdict tests from the rubric, including one-SHOULD-fail-still-passes on a synthetic
      fixture (Anton's 13 are all MUST)
- [ ] REPLAY the committed `architecture-rules` recording and assert 5/13

**Exit criteria**:
- [ ] A SHOULD violation demonstrably does not fail a gate
- [ ] Create `plans/learnings/step-3.0-rfc2119-judge.md`

---

### Step 3.1: Module 04

**Work items**:
- [ ] CREATE `module-04-rfc2119-rules/` — the 13 feature-wide rules, `Rfc2119Judge`, expect 5 pass / 8 fail / 0 undetermined
- [ ] PRINT each rule id with its status and, for failures, the location — never an average
- [ ] MAKE the separation explicit in the module's closing text: **the same generated system,
      another document written before the code, and the opposite answer.** Modules 02/03 use
      `EarsJudge`; this uses `Rfc2119Judge` — the point is that different authoritative artifacts
      ask different questions of one implementation:
      `criteria.md → EarsJudge → behaviour mostly conforms` versus
      `rules.md → Rfc2119Judge → architecture does not`
- [ ] JUnit test and integration config as in step 2.2

**Exit criteria**:
- [ ] Offline replay under 10 seconds
- [ ] **Conference critical path complete** — modules 01–04 rehearsed end to end from a clean clone
- [ ] Create `plans/learnings/step-3.1-module-04.md`

---

## Stage 4: Investigation and module 05

> Not on the conference critical path. Valuable if ready.

### Step 4.0: `Investigation`

**Work items**:
- [ ] CREATE `Investigation` taking one failed `Check` and the workspace, returning
      `(verdict: ESCALATE|CONFIRM|DE_ESCALATE, severity, oneLine, reachability, citations[])`
- [ ] ENCODE the framing constraint: *establish the consequence and whether it is reachable*; never
      *verify this finding*. Downgrading is explicitly invited (DD-5)
- [ ] REQUIRE a `file:line` on every claim and a command behind every number
- [ ] IMPLEMENT parallel fan-out over a judgment's failed checks

**Exit criteria**:
- [ ] A de-escalation is representable and does not read as failure
- [ ] Create `plans/learnings/step-4.0-investigation-tier.md`

---

### Step 4.1: Module 05

**Work items**:
- [ ] RUN the fan-out over module 04's eight failures; capture every recording
- [ ] VERIFY each investigation's load-bearing claims by hand before committing
- [ ] CREATE `module-05-investigation/` — render findings with consequence and reachability, showing the judge's input order alongside
- [ ] JUnit test asserting the output is stable on replay

**Exit criteria**:
- [ ] At least one de-escalation present and explained
- [ ] Create `plans/learnings/step-4.1-module-05.md`

---

## Stage 5: Deterministic promotion and module 06

> Not on the conference critical path. One of the tutorial's main conclusions (DD-15).

### Step 5.0: Module 06

> The mechanism is **not** chosen. Candidates include ordinary JUnit, ArchUnit, an existing
> deterministic Agent Judge class, plain Java, file or semantic comparison, and compiler or
> static-analysis tooling. Decide after inspecting existing architecture-and-style work.

**Work items**:
- [ ] CREATE `module-06-promotion/` — mechanism chosen at implementation time, not now
- [ ] ASSERT the shadow rules in one paragraph rather than demonstrating them (DD-3), stating
      plainly which findings must **not** be promoted and why
- [ ] SHOW the decision explicitly: mechanisable → deterministic policy; not → remains a judgment
      oracle
- [ ] JUnit test: the promoted rules run in the test phase and fail the build on violation

**Exit criteria**:
- [ ] Sub-second, offline, no model
- [ ] Create `plans/learnings/step-5.0-module-06.md`

---

## Stage 6: Conference path, library tour, and handoff

### Step 6.0: The presentation path

- [ ] DEFINE the talk path and time budget per module
- [ ] VERIFY every module replays offline from a clean clone
- [ ] REHEARSE end to end

### Step 6.1: The library tour (not the arc)

- [ ] Jury and aggregation; error / abstain / escalation semantics
- [ ] Coverage judges parsing the **pinned** JaCoCo report — no instrumentation ceremony
- [ ] Point at the preserved original tutorial modules `module-10-koog-evaluation` and `module-11-langchain4j-evaluation` at the repository root

### Step 6.2: Documentation and Agent Experiment handoff

- [ ] REWRITE `README.md` for the final six-module arc
- [ ] WRITE a data-only handoff for the docs steward once the arc is stable
- [ ] WRITE a short handoff describing the fixed evaluation bar this tutorial produces, as the input
      Agent Experiment needs (DD-14). **No Agent Experiment implementation here.**

**Exit criteria**:
- [ ] Clean clone → every module runs offline
- [ ] Create `plans/learnings/step-6-handoff.md`

---

## Stage 7: Promotion into `agent-judge`

> After the tutorial proves the APIs. Not before.

### Step 7.0: Promote what earned it

Promotion criteria, all required (DD-11):

1. The judge answered a real document of ≥ 40 requirements it did not author.
2. Verdict tests written from the rubric, asserting `status()`, covering the degenerate inputs.
3. No PetClinic, Anton, or tutorial identifier in its API.
4. A second, structurally different document parsed by the same code.
5. The failure message names the binding requirement and its location.

**Work items**:
- [ ] PROMOTE `Requirement`, `Obligation`, `EarsJudge`, `Rfc2119Judge` and their parsers
- [ ] PROPOSE `Check.locations` as a structured field, with this tutorial's evidence attached
- [ ] PROPOSE `Obligation`-aware aggregation for `AllMustPassStrategy`
- [ ] RAISE the `jsonSchema` gap: `ClaudeAgentOptions` supports it, `AgentClientJudgeModel` does not
- [ ] UPDATE the tutorial to depend on the promoted APIs and delete its local copies

**Exit criteria**:
- [ ] Tutorial builds against promoted library APIs with no local duplicates
- [ ] Create `plans/learnings/step-7.0-promotion.md`

---

## Step Exit Criteria Convention

Every step, in addition to its own:

- [ ] Full offline build green: `./mvnw -o test`
- [ ] Every claimed number reproducible by a command recorded in the learnings file
- [ ] Every judge finding hand-verified before it is written down as fact
- [ ] Learnings file created under `plans/learnings/`
- [ ] `ROADMAP.md` checkboxes updated
- [ ] Committed

## Standing Constraints

- **Never modify anything under `fixtures/`.** The subject is evidence.
- **No rubric authored by us.** Every requirement the tutorial judges was written by Anton's
  pipeline before the code existed.
- **No numeric score anywhere.** Individual requirements, a conjunctive gate, and the binding
  requirement named. If a mean appears, it is a defect.
- **Deterministic where possible, AI when necessary.** Reuse `BuildSuccessJudge`; do not reimplement
  what the library or the compiler already does.
- **Assert the search; show the result.**
- **Every judge runs in a JUnit test**, not only in a demo `main()`.
- **The evaluation bar stays fixed** while an agent changes. Improvement work is Agent Experiment's
  job, downstream and out of scope here.
