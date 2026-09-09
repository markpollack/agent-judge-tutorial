# Dry run — IntelliJ + JUnit

## ⭐ The color arc

```
B2   six requirements           GREEN      PASS
B4   all 52 UC6 requirements    RED        ABSTAIN · UC6-AC41 not established
B5   13 architectural MUSTs     RED        FAIL · 8 violated

B6   read RULE-4 in Anton's rules.md
B7   show the opposing lock orders

B8   RULE-4 investigation       SHOWN      CONFIRMED · REACHABLE   (not run)

     → back to slides: prefer deterministic, use AI for what remains
```

**The last merge gate you execute is RED, and it stays red.** The talk asks *"should I merge this?"*
and the answer is no; nothing after B5 changes that, because B6–B8 are diagnosis rather than
remediation.

**B8 is shown, not run.** Asserting that the investigation succeeded is a meta-result the audience
can already read off `CONFIRMED` / `REACHABLE` — it would add a green tick and no information, and a
green at the end reads as *"problem found, problem fixed"* when nothing was fixed.

The shape is: **gate decides → investigation explains → engineering responds.**

**You run the `ShouldIMerge*Demo` classes on stage, not the `*Test` classes.** They assert
`assertPass` — PASS is the merge policy — so ABSTAIN and FAIL go red, which is what a gate does.

The `*Test` classes are regression contracts: they assert the *recorded* outcome, so their ABSTAIN
and FAIL are green. Both are correct and their subjects differ — one tests the evaluator, the other
tests the subject. **Do not run the `*Test` classes on stage**; a green test labelled ABSTAIN is the
confusion this rework exists to remove.

```
PART A   presenter preflight        terminal, before the room
PART B   the live demo              IntelliJ, on stage
PART C   emergency fallback         terminal, only if IntelliJ misbehaves
```

---

# PART A — presenter preflight

## A1 · The one check

```bash
cd ~/projects/agent-judge-tutorial
./case-studies/spec-driven-petclinic/dry-run-check.sh
```

✅ `READY. Go to Step 3.` — continue to A2.
⚠️ `READY, but warm up first` — it prints one line to copy. Run it, re-run the check. ~1 minute.
❌ `STOP — do not present` — hand the output over. **Do not fix it yourself.**

## A2 · Open the project in IntelliJ

**Use `File → Open` and paste this exact path.** Not the repository root — this one:

```
/home/mark/projects/agent-judge-tutorial/case-studies/spec-driven-petclinic/pom.xml
```

IntelliJ will ask **Open as Project** — say yes. It imports the case study as a standalone Maven
project and every file the demo needs is in it.

> **Why the `pom.xml` and not the folder above it.** Opening
> `~/projects/agent-judge-tutorial` gives you the **fundamentals** tutorial — `module-01-oracle-boundary`
> through `module-11-langchain4j-evaluation`. The case study is a deliberately separate Maven project,
> so **none of the four stage classes appear** and the demo cannot run. Opening the case study's own
> `pom.xml` avoids the problem entirely rather than fixing it afterwards.

### Verify before you go on stage

The Project pane must show these four files. If it does not, stop:

```
module-02-ears-slice/src/test/java/.../ShouldIMergeSliceDemo.java
module-03-ears-usecase/src/test/java/.../ShouldIMergeBehaviorDemo.java
module-04-rfc2119-rules/src/test/java/.../ShouldIMergeArchitectureDemo.java
module-05-investigation/src/test/java/.../InvestigationReplayTest.java
```

### If you opened the repository root by mistake

Don't start over. Add the case study alongside it:

1. **Maven** tool window (right edge, or `Ctrl+Shift+A` → "Maven")
2. **+** (*Add Maven Project*)
3. Paste the same path as above
4. Wait for import — a second root appears, *Case Study - Running the Spec You Already Wrote*

### Only if you want the optional `JudgeAssertions` tab (B9)

That file lives in the fundamentals project, so it is not in the case study. Either skip B9, or open
it as a standalone file with `File → Open`:

```
/home/mark/projects/agent-judge-tutorial/judge-junit/src/main/java/io/github/markpollack/judge/junit/JudgeAssertions.java
```

## A3 · Prove the gutter works — and rehearse the RED

Open `ShouldIMergeSliceDemo.java`, click the gutter arrow next to `shouldMergeThisSlice`, confirm
green. Working directory needs no configuring — fixtures are located relative to the module, verified.

⚠️ **Then run `ShouldIMergeBehaviorDemo` and look at what red actually does to the screen.** A failing
JUnit test expands a stack trace and rearranges the Run window far more than a green one. You need to
know, before the room, where the useful text sits and how much scrolling it takes to reach it.

The line that matters is the **first** one:

```
Expected judgment PASS but was ABSTAIN  (no PASS/FAIL conclusion; see reasoning)
  reasoning: 51 of 52 established, 1 could not be established: UC6-AC41
```

If the stack trace crowds it out, collapse it in the Run window now. **Do not change the assertion to
make the UI prettier.**

## A4 · Tabs to pre-open, left to right

Open them in this order so the tab bar *is* your running order:

| # | File | Where to leave the caret |
|---|---|---|
| 1 | `ShouldIMergeSliceDemo.java` | `shouldMergeThisSlice` |
| 2 | `ShouldIMergeBehaviorDemo.java` | `shouldMergeUc6Behavior` |
| 3 | `ShouldIMergeArchitectureDemo.java` | `shouldMergeArchitecture` |
| 4 | `rules.md` | **line 38**, `### RULE-4` |
| 5 | `StaffFallbackService.java` | **line 248** |
| 6 | `LifecycleProcessor.java` | **line 166** |
| 7 | `InvestigationReplayTest.java` | `theRecordedInvestigationReplaysToTheSameAnswer` |
| 8 *(optional)* | `JudgeBackends.java` | only if you explain live-vs-replay |

Tabs 4–6 are the **vendored, unmodified** subject. Inside the opened project they are under
`fixtures/petclinic/appointment-scheduling-spec-with-usecases/`. Full paths, if you would rather
paste them into `File → Open`:

```
/home/mark/projects/agent-judge-tutorial/case-studies/spec-driven-petclinic/fixtures/petclinic/appointment-scheduling-spec-with-usecases/spec/smart-appointment-scheduling/rules.md

/home/mark/projects/agent-judge-tutorial/case-studies/spec-driven-petclinic/fixtures/petclinic/appointment-scheduling-spec-with-usecases/src/main/java/org/springframework/samples/petclinic/scheduling/service/StaffFallbackService.java

/home/mark/projects/agent-judge-tutorial/case-studies/spec-driven-petclinic/fixtures/petclinic/appointment-scheduling-spec-with-usecases/src/main/java/org/springframework/samples/petclinic/scheduling/service/LifecycleProcessor.java
```

And the recording, if you show it in B3:

```
/home/mark/projects/agent-judge-tutorial/case-studies/spec-driven-petclinic/tutorial-support/src/main/resources/recordings/spec-conformance-uc6.txt
```

## A5 · Make it legible

- Editor font **up** — presentation preset if you have one.
- **Test runner font up too.** The runner is part of the demo now; its `@DisplayName` lines carry
  the story.
- Collapse every tool window except Project and Run.
- Turn off inlay hints if they clutter.

---

# PART B — the live demo, in IntelliJ

## 🎙️ Opening

> **"The agent says it's done. Should I merge?"**

> **"Everything I'm about to show is just Java and JUnit. This is the same way you'd use it in a
> normal IntelliJ project."**

## B1 · Module 01 — say it, don't run it

**Do not run the 45-second Maven build on stage.** The audience already understands builds; it would
spend their attention on the part they know.

> **"We already ran the real build. It takes about 45 seconds and all 290 tests pass."**
>
> **"That's the deterministic part. It builds. But the build has no opinion about whether it did
> what was asked."**

## B2 · Module 02 — the first Agent Judge API

**Tab 1 · `ShouldIMergeSliceDemo.java`** → `shouldMergeThisSlice`

The whole method body is three lines. **This is the most important 30 seconds in the talk.**

```java
var requirements = EarsCriterion.select(EarsCriterion.from(CRITERIA), SLICE);

var judge = EarsJudge.create("appointment-cancellation", requirements, model);

assertPass(judge, context);
```

Point at each line as you say its sentence:

```
requirements  →  "These are six requirements from Anton's spec, written before the code."
judge         →  "This judge evaluates them against the repository."
assertPass    →  "And this is the merge policy. JUnit requires PASS."
```

> **"I don't mean JUnit-like. I mean JUnit."**

**Click the gutter arrow.** Expect **GREEN**.

> **"Good."**

Don't over-explain and don't linger in the runner. This green is here to establish the audience's
normal expectation — PASS is green — *before* the next one turns red.

**Do not** explain `model` and `context` yet. They are fields above the method precisely so they stay
out of the first thing anyone sees.

## B3 · What normally happens, and why this is instant — ~60 seconds

**Do this after the first green, and give it real time.** A Java developer who watches 52 requirements
evaluate in two seconds is right to be suspicious, and the answer is a pattern they already know.

### First, the two fields, one sentence each

> **"`context` is just the PetClinic workspace being evaluated."**
>
> **"`model` is where the judgment answer comes from."**

### Then: are we calling an AI right now? No.

> **"There is no API key set and no network call in this demo. Not one."**

### What normally happens — the live path

> **"Normally `model` is an `AgentClientJudgeModel`. It runs a real agent through AgentClient: it
> greps the repository, opens files, runs commands, and then answers all 52 requirements one line at
> a time. That takes about fourteen minutes."**
>
> **"I'm not making you watch that. I ran it, I captured exactly what it said, and I committed it."**

### Show the recording — it is 56 lines, open it

**Optional tab 9**, and worth it if the room looks sceptical:

```
case-studies/spec-driven-petclinic/tutorial-support/src/main/resources/
  recordings/spec-conformance-uc6.txt
```

Line 1 is the provenance:

```
Captured 2026-09-06 from a live AgentClient run. Verbatim agent output follows the blank line.
```

Then scroll to **line 45**, which is the line that turned the gate red:

```
UC6-AC41: CANNOT_DETERMINE - no main-source path ever sets a vet inactive (`Vet.setActive` at
`Vet.java:58` is called only by test fixtures, always with `true`), so nothing here exercises
deactivation or its effect on existing appointments.
```

> **"That's the agent's own sentence. `CANNOT_DETERMINE`. The judge parsed that into ABSTAIN, and
> ABSTAIN is why JUnit went red."**

### The analogy that makes it stop being suspicious

> **"This is a recorded HTTP interaction. Same idea as WireMock or VCR — capture the real response
> once, replay it so the test is fast, offline and deterministic. Except what I recorded isn't an
> HTTP response, it's what an agent said after reading the repository."**

### What is *not* recorded — the important half

> **"Only that one method is swapped. Everything else runs for real, every time: the prompt is
> rendered, the parser reads all 52 answers, the roster guard checks that 52 arrived, and the verdict
> is computed in Java. That's why it takes two seconds and not zero."**
>
> **"The verdict isn't stored anywhere. ABSTAIN and UC6-AC41 are recomputed from those words on every
> run. If I broke the rollup, the recording wouldn't hide it."**

And the honest caveat, which costs nothing and buys credibility:

> **"What a recorded run proves is the wiring and the semantics — not that the judge is right. To
> re-earn that I set one environment variable and wait fourteen minutes."**

> 💡 **Available if you want it, distraction risk if you don't:** that same recording opens by saying
> *"Existing surefire reports show 270 tests"*. There are 290. **Every file-and-line citation in that
> recording is correct and its prose count is wrong** — which is the tutorial's own thesis, visible in
> the raw evidence. Powerful, but it opens a topic. Only take it if you have time.

## B4 · Module 03 — the same gate, on the whole document · expect RED

**Tab 2 · `ShouldIMergeBehaviorDemo.java`** → `shouldMergeUc6Behavior`

Same three lines. One difference — no `select`:

```java
var requirements = EarsCriterion.from(CRITERIA);

var judge = EarsJudge.create("appointment-lifecycle", requirements, model);

assertPass(judge, context);
```

> **"Same judge. Same policy. This time I'm not sampling six — I'm running the whole document."**

**Run it.** Expect **RED**. Read the top of the failure out loud:

```
Expected judgment PASS but was ABSTAIN  (no PASS/FAIL conclusion; see reasoning)
  reasoning: 51 of 52 established, 1 could not be established: UC6-AC41
  1 of 52 checks failed:
    - UC6-AC41: could not be established: no main-source path ever sets a vet inactive
      (`Vet.setActive` at `Vet.java:58` is called only by test fixtures, always with `true`)
```

### 🎙️ Then the line. The IDE has just made the argument for you.

> **"This is exactly what I want from a merge gate. It did not establish the criterion, so the gate
> does not go green."**
>
> **"PASS means I established every required criterion.
> I don't turn 51 out of 52 into 98% and call it done."**

**Pause.**

Notice what you no longer have to do: there is no explaining why a green test means something bad.
Red means not yet. The tool agrees with the sentence.

### 🎙️ Then the transition nothing on screen gives you

> **"Behaviour isn't the only thing Anton specified."**

## B5 · Module 04 — same gate, different specification · expect RED

**Tab 3 · `ShouldIMergeArchitectureDemo.java`** → `shouldMergeArchitecture`

Point at the symmetry. **It is the same three lines with the document changed** — that is the point of
this tab:

```java
var requirements = Rfc2119Constraint.from(RULES);

var judge = Rfc2119Judge.create("architecture", requirements, model);

assertPass(judge, context);
```

> **"Same implementation. Another document, written before the code by the same author. Same JUnit
> policy: PASS is required."**

**Run it.** Expect **RED**:

```
Expected judgment PASS but was FAIL
  reasoning: 5 of 13 hold, 8 violated
  8 of 13 checks failed:
    - RULE-1: No centralized transition policy exists anywhere; SchedulingRequest.setStatus …
    - RULE-2: … current time is not all Clock-derived: UserPrincipal.java:93 …
    - RULE-4: … StaffFallbackService takes a PESSIMISTIC_WRITE lock on SchedulingRequest at :247
              before locking Owner/Pet/Vet at :258 …
    - RULE-5 · RULE-8 · RULE-10 · RULE-11 · RULE-12
```

Every one carries a file and a line, because `JudgeAssertions` keeps the evidence rather than
reducing it to a boolean.

### 🎙️ Headline the engineering, not the arithmetic

> **"The build is green. Its 290 tests pass. The behavioural specification mostly held. And eight
> feature-wide architectural rules do not hold — locking, transaction boundaries, time handling,
> authorization, what's allowed across a boundary, idempotency, build consistency."**

> ⚠️ Say those as a list of **areas**, not as a claim that each maps to exactly one rule. That mapping
> is not verified rule by rule.

Then scroll to **RULE-4** in the failure — it is the one you are about to open.

## B6 · Show the rule that was actually violated

**Tab 4 · `rules.md` at line 38.** Do not jump from `RULE-4 FAIL` to `CONFIRMED`.

Highlight the requirement itself:

```
locks MUST be acquired in this global order, with identifiers ascending within a type:
Owner, Pet, Vet, SchedulingRequest, Appointment, Reservation
```

> **"The judge didn't invent this. Anton's design explicitly says there is one global lock order."**

## B7 · Show the two code paths

**Tab 5 · `StaffFallbackService.java`, line 248.**

```
248   SchedulingRequest request = lockCoordinator.lockRequest(requestId);   ← SchedulingRequest
258   lockCoordinator.lockResources(owner, pet, vet, request, ...)          ← then Owner
```

> **"This path takes SchedulingRequest, then Owner."**

**Tab 6 · `LifecycleProcessor.java`, line 166.**

```
166   lockCoordinator.lockResources(owner, pet, vet, request, ...)          ← Owner, then Request
```

> **"This path takes Owner, then SchedulingRequest."**

Then, on screen, the contrast:

```
staff fallback        Request → Owner
lifecycle processing  Owner → Request
```

**Pause.**

> **"That's not lint."**

## B8 · Module 05 — why a second evaluation exists

Say the *reason* before opening the test:

> **"The gate has already done its job — it stopped the merge. Now I'm asking a different question."**
>
> **"The architecture judge is a screening step. It says RULE-4 doesn't hold and points me at this
> code. But that alone doesn't prove the bug matters — maybe those two paths can never run at the
> same time."**
>
> **"So I ask a second agent a deliberately different question: what is the consequence, and is it
> actually reachable?"**

**Tab 7 · `InvestigationReplayTest.java`** → `theRecordedInvestigationReplaysToTheSameAnswer`

```java
assertEquals(Investigation.Outcome.CONFIRMED, investigation.outcome());
assertEquals(Investigation.Reachability.REACHABLE, investigation.reachability());
```

**Show the two assertions. Do not run this as your closing act.**

```java
assertEquals(Investigation.Outcome.CONFIRMED, investigation.outcome());
assertEquals(Investigation.Reachability.REACHABLE, investigation.reachability());
```

> **"CONFIRMED. REACHABLE."**
>
> **"The first judge found the violation. The investigation found the path that makes it real."**

**Don't run it.** It is a meta-result — it asserts that the investigation did its job, which the
audience can already read off those two values. It would add a green tick and no information, and a
green here reads as *"problem fixed"* when nothing was fixed.

Optional, and it is what makes the second tier more than an echo:

> **"It was allowed to de-escalate this. It didn't."**

> ⛔ **Do NOT say the deadlock was reproduced.** The recording reports an H2 reproduction; that was
> deliberately not hand-verified and the talk does not need it. Omit unless asked.
>
> The line-number movement (`:247` → `:248`) is a **supporting observation, not the headline.**
> The headline is the opposing reachable lock orders.

### 🎙️ Then hand off to the engineering question

> **"The merge decision was already made — the gate made it. This second evaluation isn't another
> vote. It's diagnosis. It tells me which failure matters, and why."**

> **"So now the engineering question: which of these judgments should stay AI judgments, and which
> can we turn into deterministic checks?"**

**Nothing else runs.** The last gate you executed was red and it stays red — you were investigating,
not remediating. B9 is an optional 20-second aside; B10 is spoken, and is where that engineering
question gets answered.

## B9 · Optional — `JudgeAssertions`, 20 seconds

Only if the room is with you.

> **"This adapter decides nothing. It takes the Agent Judge result and turns it into a JUnit
> assertion without throwing away the evidence."**

The contrast that lands with Java developers:

```java
assertTrue(judgment.pass());          // fails as: expected true, was false
JudgeAssertions.assertStatus(...);    // fails with status, reasoning, and every failed check
```

Show only `assertPass` / `assertFail` / `assertStatus`. Skip the Jury overloads.

## B10 · Module 06 — spoken, nothing to run

Picks up directly from B8's handoff. Nothing runs here, and nothing needs to be on screen.

> **"Now that we've localized these findings, we can classify them."**
>
> **"Some can become ArchUnit rules — layer and dependency constraints. Some become ordinary JUnit
> or plain Java checks. Some would need concurrency-specific tooling or static analysis. And some
> stay judgment."**
>
> **"The point isn't that RULE-4 becomes ArchUnit. The point is that whenever one of these
> judgments becomes mechanically expressible, the model should stop doing that job."**

> ⛔ **Do NOT claim** the tutorial implemented any ArchUnit rule, or that `ArchUnitJudge` exists.
> ⛔ **Do NOT** present ArchUnit as *the* destination for RULE-4 — a dynamic global lock order is
> not obviously an ArchUnit rule, and someone in that room will know it.

### 🎙️ And then close on why any of it was in JUnit

> **"This is why I wanted it in JUnit. The merge gate isn't a score, and it isn't a report I
> remember to read later. If the required bar isn't established, IntelliJ goes red."**

---

# PART C — emergency fallback

**Only if IntelliJ misbehaves.** This was the primary demo until 2026-09-08 and it still works
exactly as before.

```bash
cd ~/projects/agent-judge-tutorial
for m in module-01-build module-02-ears-slice module-03-ears-usecase \
         module-04-rfc2119-rules module-05-investigation; do
  echo; echo "########## $m ##########"; echo
  ./mvnw -q -o -f case-studies/spec-driven-petclinic/pom.xml exec:java -pl $m
done
```

Or one at a time:

```bash
./mvnw -q -o -f case-studies/spec-driven-petclinic/pom.xml exec:java -pl module-02-ears-slice
```

Module 01 takes 40–50s; the rest are ~2s each.

Terminal notes that only matter here: outputs run 22–64 lines, and module 02 prints one 172-column
line (the criteria file path).

---

# If anything looks wrong

**Don't debug it.** Say what you saw and hand it over.

These are the only things that matter:

| Run | Class | Expect | Because |
|---|---|---|---|
| B2 | `ShouldIMergeSliceDemo` | 🟢 **GREEN** | six requirements PASS |
| B4 | `ShouldIMergeBehaviorDemo` | 🔴 **RED** | ABSTAIN — `UC6-AC41` not established |
| B5 | `ShouldIMergeArchitectureDemo` | 🔴 **RED** | FAIL — 8 of 13 violated |
| B8 | `InvestigationReplayTest` | *shown, not run* | CONFIRMED · REACHABLE |

**Two of these are supposed to be red**, and the last gate you execute is one of them. That is the
demo working. If B4 or B5 goes *green*, something
is wrong — you are probably running the `*Test` regression class instead of the `*Demo` merge gate.

**The ordinary build stays green.** `./mvnw -o -f case-studies/spec-driven-petclinic/pom.xml test`
runs 49 tests and passes; Surefire does not discover `*Demo` classes. Verified from a clean clone.

**Normal, not errors:** a `spring-javaformat` line about `AccountBootstrapRunner.java` if the
candidate re-materializes (harmless provenance — let it finish, don't explain it unless asked).


---

# Regression suite vs merge gate — the distinction, once

This is not a workaround. **The subject of the test is different**, and that is the whole reason both
exist.

| | Regression contract (`*Test`) | Merge gate (`*Demo`) |
|---|---|---|
| Question | Did the evaluator reproduce the known recorded result? | Did the implementation meet the required bar? |
| Subject | the evaluator | the subject under evaluation |
| Assertion | `assertStatus(ABSTAIN, …)` | `assertPass(…)` |
| ABSTAIN | 🟢 green — expected | 🔴 red — bar not met |
| FAIL | 🟢 green — expected | 🔴 red — bar not met |
| Runs in CI | yes, 49 tests | no — Surefire skips `*Demo` |
| Runs on stage | **no** | **yes** |

Why `*Demo` and not `*Test`: two of these gates are *supposed* to be red. A build that is
intentionally red teaches people to ignore red, so they stay out of the ordinary suite while IntelliJ
still offers a gutter arrow beside `@Test`.

Why keep the regression tests at all: they are what catches a promotion defect or a broken recording.
They earned their place on 2026-09-08 when the library swap had to be proven not to move a verdict.

---

# Timing

| | |
|---|---|
| Module 01 | spoken only, ~30s |
| Module 02 | ~1 min |
| B3 · what normally happens + the recording | ~1 min |
| Module 03 | ~2 min |
| Module 04 | ~2 min |
| RULE-4 + the two code paths | ~2 min |
| Module 05 investigation (shown) | ~1½ min |
| Module 06 spoken | ~1 min |

**~11–12 minutes.** Slightly longer than the terminal version, because reading code aloud is the
point rather than an overhead. Test runs themselves are instant.

---

# Presenter notes

**Where the code is** — never give a bare repo URL; `main` is a different, older tutorial with no
case study.

```
repo    github.com/markpollack/agent-judge-tutorial
branch  petclinic-evidence-arc
path    case-studies/spec-driven-petclinic
tag     conference-merge-gate
```

**The library claim, this exact wording:**

> "The EARS and RFC 2119 judges are now on Agent Judge main and available in the published
> 0.16.0 snapshot."

⛔ **Not** "Agent Judge 0.16.0 has been GA released." Nothing is on Maven Central under 0.16.0.

**If asked why `EarsJudge` doesn't just use `AllMustPassStrategy`:**

> "Agent Judge's existing abstention semantics are jury semantics — abstain means the judge doesn't
> apply, so it doesn't vote. A specification is different. Every requirement applies by
> construction, so 'couldn't establish' has to block PASS. The tutorial exposed that distinction,
> and we'll likely make that aggregation policy explicit in the library."

**If asked about the other thirteen documents:** 15 documents carry the 438 requirements; the arc
reads two. Nobody has run the other thirteen back either — that is the scale of the problem, not a
gap in the demo. And `UC6-RULE1`, in a document the arc does *not* read, covers `UC6-AC41` — the one
criterion module 03 could not establish. The spec was already saying where to look. *(A lead, not a
resolution — reading the rule would not have settled the criterion, because the judge abstained on
the code.)*
