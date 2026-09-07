# Roadmap: Agent Judge Tutorial — Running the Spec You Already Wrote

> **Created**: 2026-09-07T10:40-04:00
> **Last updated**: 2026-09-07T10:40-04:00
> **Design version**: 2026-09-07T10:20-04:00

## Overview

Three acts, built in dependency order.

**Act 1 — read the documents they already wrote.** Anton's branch ships 438 numbered requirements
in two public formats. Two judges read them: `Rfc2119Judge` first (the shortest path — 20 MUST-form
constraints already parsed), then `EarsJudge` (374 criteria, and the decomposition experiment).

**Act 2 — turn findings into evidence, then into policy.** A FAIL is an address. The investigation
tier turns it into a consequence with a reachability argument. The mechanisable minority becomes
deterministic rules that never need a model again.

**Act 3 — bring architectural knowledge they did not write.** Detect the codebase's architectural
shape, select the matching archetype from our catalogue, judge against rules that transfer between
projects, investigate, and remediate. This is the only act whose rubric is ours, and the only one
that produces a change rather than a verdict.

Every judge runs inside a JUnit test as well as a demo `main()`. The test is what a real project
would write; the demo is what a stage needs.

> **Before every commit**: verify ALL exit criteria for the current step are met. Do NOT remove
> exit criteria to mark a step complete — fulfill them.

---

## Stage 1: Groundwork and discovery

### Step 1.0: Design review

**Entry criteria**:
- [ ] Read `plans/VISION.md`, `plans/DESIGN.md`
- [ ] Read `plans/learnings/what-we-know.md`

**Work items**:
- [ ] REVIEW design against vision success criteria
- [ ] VERIFY every claimed measurement in DESIGN.md is reproducible by a command
- [ ] DOCUMENT open questions

**Exit criteria**:
- [ ] Create `plans/learnings/step-1.0-design-review.md`
- [ ] Update `ROADMAP.md` checkboxes

---

### Step 1.1: Run UC6's seven use-case rules — the last unknown

The middle rung between behaviour and feature architecture. Nothing has read it. Its result decides
whether act 1 has two levels or three.

**Entry criteria**:
- [ ] Step 1.0 complete
- [ ] Candidate materialized at `fixtures/petclinic/build/large-candidate`

**Work items**:
- [ ] WIDEN `ArchitectureRule.HEADING` to match `UC\d+-RULE\d+` as well as `RULE-\d+`
- [ ] ADD a parser test asserting 7 rules from `manage-appointment-lifecycle/rules.md`
- [ ] RUN live against the candidate, capture the recording
- [ ] VERIFY every FAIL by hand against the source before recording any conclusion
- [ ] RECORD the result and whether the rung is interesting

**Exit criteria**:
- [ ] 7 of 7 answered, or ERROR naming the missing
- [ ] Every finding hand-verified with a file and line
- [ ] Recording committed
- [ ] Create `plans/learnings/step-1.1-uc6-rules.md`

---

### Step 1.2: Consolidate the repository

**Entry criteria**:
- [ ] Step 1.1 complete

**Work items**:
- [ ] MOVE the pre-redesign module set (`module-01-oracle-boundary` … `module-11-langchain4j-evaluation`) out of the reactor into `attic/`, preserving Koog and LangChain4j for the library tour
- [ ] DEMOTE `module-03-ai-architecture-judge` and `module-04-agentic-architecture-judge` out of the arc (DD-1, DD-2); keep the sources
- [ ] EXTRACT shared parsers, `JudgeBackends`, `RecordedJudgeModel` and `PetClinic` into `tutorial-support/`
- [ ] VERIFY `./mvnw -o test` is green and the reactor has one module per arc position
- [ ] COMMIT

**Exit criteria**:
- [ ] No duplicate module numbers in the reactor
- [ ] Full offline build green
- [ ] Create `plans/learnings/step-1.2-consolidation.md`

---

## Stage 2: Act 1 — the format judges

### Step 2.0: `Requirement` and `Obligation`

**Work items**:
- [ ] CREATE `Requirement` interface: `id()`, `title()`, `asPrompt()`, `obligation()`
- [ ] CREATE `Obligation` enum: `MUST`, `MUST_NOT`, `SHOULD`, `SHOULD_NOT`, `MAY`, `ACCEPTANCE`
- [ ] DOCUMENT on `Obligation` that it carries the aggregation policy, not merely a label

**Exit criteria**:
- [ ] Javadoc states why obligation is read from the document rather than chosen by us
- [ ] Create `plans/learnings/step-2.0-requirement-model.md`

---

### Step 2.1: `Rfc2119Judge`

Shortest path to something running: 20 MUST-form constraints already exist and parse.

**Work items**:
- [ ] CREATE `Rfc2119Constraint.from(Path)` — parses `### <ID>`, `**Covers:**`, `**<KEYWORD>**`, `**Reason:**`
- [ ] CREATE `Rfc2119Judge` over `ModelBackedJudge`, roster guard, verdict computed in Java
- [ ] IMPLEMENT obligation-aware rollup: `MUST` violations fail the gate; `SHOULD` violations are reported as checks and do not
- [ ] WRITE verdict tests **from the rubric, not the code** — including: all-MUST-pass, one-MUST-fail, one-SHOULD-fail-still-passes, unanswered-is-ERROR, all-abstain-is-ABSTAIN, repeated-answer-counts-once, out-of-order-answers-still-complete
- [ ] REPLAY the committed `architecture-rules` recording and assert 5/13

**Exit criteria**:
- [ ] 13 feature rules and 7 UC6 rules both parse and judge
- [ ] A SHOULD violation demonstrably does not fail a gate (fixture constraint, since Anton's are all MUST)
- [ ] Every test asserts `status()`, never `pass()`
- [ ] Create `plans/learnings/step-2.1-rfc2119-judge.md`

---

### Step 2.2: `EarsJudge`, and whether decomposition earns its keep

**Work items**:
- [ ] CREATE `EarsCriterion.from(Path)` parsing the four templates into `(id, template, trigger, response)`
- [ ] ASSERT the parser finds 52 in UC6 and classifies 28 `When` / 20 `If` / 4 `While`
- [ ] CREATE `EarsJudge` with two prompt strategies: whole-sentence, and decomposed trigger/response
- [ ] RUN both live over the same 52 criteria; compare verdicts and the quality of failure messages
- [ ] DECIDE on evidence whether decomposition ships; record the comparison either way

**Exit criteria**:
- [ ] 52 of 52 answered by both strategies
- [ ] Comparison recorded with the specific claim: does decomposition report *which half* failed?
- [ ] Whichever ships, the other is deleted rather than left as a flag
- [ ] Create `plans/learnings/step-2.2-ears-judge.md`

---

### Step 2.3: Modules 01–04, with JUnit harnesses

**Work items**:
- [ ] MODULE 01 `does it build?` — `BuildSuccessJudge`, materialized tree, no formatting content (DD-9)
- [ ] MODULE 02 `six criteria you can read` — UC6-AC7..AC12, `EarsJudge`, expect 6/6
- [ ] MODULE 03 `the whole use case` — all 52, expect 51 pass / 0 fail / 1 undetermined
- [ ] MODULE 04 `a different document` — UC6's 7 rules and the feature's 13, `Rfc2119Judge`
- [ ] FOR EACH: a demo `main()` for the stage **and** a JUnit test using `JudgeAssertions` that runs the same judge and asserts the same verdict
- [ ] ADD `judge-junit` as a test dependency of every arc module
- [ ] ADD one integration-testing config per module with exact expected output lines

**Exit criteria**:
- [ ] Every module runs offline from a committed recording, deterministically
- [ ] Every module has a passing JUnit test that would fail the build if the verdict changed
- [ ] No numeric score appears anywhere in any module's output (DD-12)
- [ ] Create `plans/learnings/step-2.3-act-one-modules.md`

---

## Stage 3: Act 2a — the investigation tier

### Step 3.0: `Investigation`

**Work items**:
- [ ] CREATE `Investigation` taking one failed `Check` and the workspace, returning
      `(verdict: ESCALATE|CONFIRM|DE_ESCALATE, severity, oneLine, reachability, citations[])`
- [ ] ENCODE the framing constraint in the prompt: *establish the consequence and whether it is
      reachable*; never *verify this finding*. Downgrading is explicitly invited (DD-5)
- [ ] REQUIRE a `file:line` on every claim and a command behind every number
- [ ] IMPLEMENT parallel fan-out over a judgment's failed checks

**Exit criteria**:
- [ ] A de-escalation is representable and does not read as failure
- [ ] Create `plans/learnings/step-3.0-investigation-tier.md`

---

### Step 3.1: Module 05 — addresses become consequences

**Work items**:
- [ ] RUN the fan-out over module 04's failures; capture every recording
- [ ] VERIFY each investigation's load-bearing claims by hand before committing
- [ ] RENDER findings ranked by severity, with the input order shown alongside to make the point
- [ ] JUNIT test asserting the ranking is stable on replay

**Exit criteria**:
- [ ] Ranked output demonstrably differs from document order
- [ ] At least one de-escalation present and explained
- [ ] Create `plans/learnings/step-3.1-module-05.md`

---

## Stage 4: Act 2b — promotion to policy

### Step 4.0: Module 06 — two rules leave the model behind

**Work items**:
- [ ] PORT `ArchUnitJudge`, `CandidateClasses`, `PromotedRules`, `ConfigRules` into the arc module
- [ ] KEEP the promotion test (`PromotionCarveTest`) asserting 5 and 13 violations and the scope bound
- [ ] ASSERT the shadow rules in one paragraph rather than demonstrating them (DD-3)
- [ ] JUNIT test: the promoted rules run in the test phase and fail the build on violation

**Exit criteria**:
- [ ] Sub-second, offline, no model
- [ ] The paragraph names what must *not* be promoted and why
- [ ] Create `plans/learnings/step-4.0-module-06.md`

---

## Stage 5: Act 3 — architectural archetypes

> ⚠️ **This act's rubric does not exist yet.** Everything before it reads a document somebody else
> wrote; this act requires us to write one. Treat step 5.0 as a go/no-go.

### Step 5.0: Can we name the architectural shape from evidence?

**Work items**:
- [ ] ENUMERATE detectable structural signals: package topology, dependency direction, annotation
      populations, layer naming, cycle presence
- [ ] MEASURE them on the candidate — package-by-domain at the top (`owner`, `vet`, `scheduling`,
      `security`, `system`), package-by-layer inside `scheduling` (controller 12, service 25,
      model 31, repository 15, dto 15, matching 9)
- [ ] DETERMINE whether shape detection can be deterministic, or needs judgment
- [ ] GO/NO-GO: if detection is unstable, act 3 stops here and is recorded as a negative result

**Exit criteria**:
- [ ] A written, evidence-based statement of this codebase's architectural shape
- [ ] A decision, with reasons, on whether detection is deterministic
- [ ] Create `plans/learnings/step-5.0-shape-detection.md`

---

### Step 5.1: Author one archetype catalogue

**Work items**:
- [ ] ASK the bud steward what architectural pattern material exists and what may be reused
      (high-level vocabulary only; implementation details are out of scope per VISION)
- [ ] AUTHOR `archetypes/spring-layered-monolith.md` in **RFC 2119 form**, so `Rfc2119Judge` reads
      it unchanged — each rule with a Reason and the shape signals it presumes
- [ ] Rules must transfer: no PetClinic names, no Anton-specific identifiers
- [ ] VALIDATE the catalogue against the *baseline* PetClinic as a control — a rule that fails on
      stock PetClinic is probably wrong

**Exit criteria**:
- [ ] The catalogue parses with the existing `Rfc2119Constraint` parser, unmodified
- [ ] Every rule states its shape precondition
- [ ] Baseline control run recorded
- [ ] Create `plans/learnings/step-5.1-archetype-catalogue.md`

---

### Step 5.2: Judge, investigate, remediate

The same two-tier pattern, plus a third tier that produces a change.

**Work items**:
- [ ] RUN `Rfc2119Judge` over the archetype catalogue against the candidate
- [ ] RUN the investigation tier over its failures
- [ ] CREATE `Remediation`: for one investigated finding, produce a patch and a rationale
- [ ] RE-JUDGE the remediated workspace with the same judge — the loop closes back to act 1
- [ ] NEVER modify the vendored fixture; remediate a materialized copy

**Exit criteria**:
- [ ] A finding traced end to end: address → consequence → patch → re-judged green
- [ ] The re-judge uses the identical judge and rubric, not a relaxed one
- [ ] Create `plans/learnings/step-5.2-remediation.md`

---

### Step 5.3: Modules 07–09

**Work items**:
- [ ] MODULE 07 `what shape is this?` — evidence-based shape detection
- [ ] MODULE 08 `rules you didn't write` — the archetype catalogue judged
- [ ] MODULE 09 `fix one, and prove it` — remediation and re-judgement
- [ ] JUNIT harness for each, as in step 2.3

**Exit criteria**:
- [ ] Offline replay for all three
- [ ] Create `plans/learnings/step-5.3-act-three-modules.md`

---

## Stage 6: Promotion into `agent-judge`

### Step 6.0: Promote what earned it

Promotion criteria, all required (DD-11):

1. The judge answered a real document of ≥ 40 requirements it did not author.
2. Verdict tests written from the rubric, asserting `status()`, covering the degenerate inputs.
3. No PetClinic, Anton, or tutorial identifier in its API.
4. A second, structurally different document parsed by the same code.
5. The failure message names the binding requirement and its location.

**Work items**:
- [ ] PROMOTE `Requirement`, `Obligation`, `Rfc2119Judge`, `EarsJudge` and their parsers
- [ ] PROPOSE `Check.locations` as a structured field, with this tutorial's evidence attached
- [ ] PROPOSE `Obligation`-aware aggregation for `AllMustPassStrategy`
- [ ] RAISE the `jsonSchema` gap: `ClaudeAgentOptions` supports it, `AgentClientJudgeModel` does not
- [ ] UPDATE the tutorial to depend on the promoted APIs and delete its local copies

**Exit criteria**:
- [ ] Tutorial builds against promoted library APIs with no local duplicates
- [ ] Create `plans/learnings/step-6.0-promotion.md`

---

## Stage 7: Talk path, library tour, handoff

### Step 7.0: The presentation path

**Work items**:
- [ ] DEFINE the talk path and the time budget per module
- [ ] VERIFY every module in it replays offline in under 10 seconds
- [ ] REHEARSE the full path end to end from a clean clone

### Step 7.1: The library tour (not the arc)

**Work items**:
- [ ] Jury and aggregation; error / abstain / escalation semantics
- [ ] Coverage judges parsing the **pinned** JaCoCo report — no instrumentation ceremony
- [ ] Koog and LangChain4j integration modules from `attic/`

### Step 7.2: Documentation handoff

**Work items**:
- [ ] REWRITE `README.md` for the final module set
- [ ] WRITE a data-only handoff for the docs steward once the arc is stable

**Exit criteria**:
- [ ] Clean clone → every module runs offline
- [ ] Create `plans/learnings/step-7-handoff.md`

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
- **No rubric authored by us in acts 1 and 2.** Act 3's catalogue is the single, explicit exception,
  and it is validated against a control codebase.
- **No numeric score anywhere.** Individual criteria, a conjunctive gate, and the binding requirement
  named. If a mean appears, it is a defect.
- **Assert the search; show the result.**
- **Every judge runs in a JUnit test**, not only in a demo `main()`.
