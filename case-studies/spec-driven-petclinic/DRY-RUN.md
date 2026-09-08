# Dry run — IntelliJ + JUnit

> **The demo is now IDE-first.** The audience is Java developers, and the message is that Agent
> Judge is ordinary Java you run from JUnit in IntelliJ. The terminal is setup, rehearsal and
> emergency fallback — it is no longer the stage.

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

## A2 · ⚠️ IntelliJ setup — the step that will break the demo if skipped

**Opening `~/projects/agent-judge-tutorial` gives you the WRONG project.** The root Maven reactor is
the *fundamentals* tutorial — `module-01-oracle-boundary` through `module-11-langchain4j-evaluation`.
**The case study is a deliberately separate Maven project and its tests will not appear at all.**

So after opening the repository:

1. Open the **Maven** tool window (right edge, or `⌘⇧A` / `Ctrl+Shift+A` → "Maven").
2. Click **+** (*Add Maven Project*).
3. Select:
   ```
   ~/projects/agent-judge-tutorial/case-studies/spec-driven-petclinic/pom.xml
   ```
4. Wait for import. You should now see a second root, **Case Study - Running the Spec You Already
   Wrote**, with `tutorial-support` and `module-01-build` … `module-05-investigation` under it.

**Verify before you go on stage** — the Project pane must show:

```
case-studies/spec-driven-petclinic/
  module-02-ears-slice/src/test/java/.../EarsSliceTest.java
  module-03-ears-usecase/src/test/java/.../EarsUseCaseTest.java
  module-04-rfc2119-rules/src/test/java/.../Rfc2119RulesTest.java
  module-05-investigation/src/test/java/.../InvestigationReplayTest.java
```

If those four files are not there, **the demo cannot run.** Fix it now, not at 9am.

## A3 · Prove the gutter works, before the room

Open `EarsSliceTest.java`, click the green gutter arrow next to
`theCancellationSliceIsFullyEstablished`, and confirm it goes green. **If the gutter run works once,
it will work on stage.** Working directory does not need configuring — the fixtures are located
relative to the module, and this was verified.

## A4 · Tabs to pre-open, left to right

Open them in this order so the tab bar *is* your running order:

| # | File | Where to leave the caret |
|---|---|---|
| 1 | `EarsSliceTest.java` | `theCancellationSliceIsFullyEstablished` |
| 2 | `EarsUseCaseTest.java` | `theCompleteSpecificationCannotBeEstablished` |
| 3 | `Rfc2119RulesTest.java` | `theArchitecturalDesignIsViolated` |
| 4 | `rules.md` | **line 38**, `### RULE-4` |
| 5 | `StaffFallbackService.java` | **line 248** |
| 6 | `LifecycleProcessor.java` | **line 166** |
| 7 | `InvestigationReplayTest.java` | `theRecordedInvestigationReplaysToTheSameAnswer` |
| 8 *(optional)* | `JudgeBackends.java` | only if you explain live-vs-replay |

Paths for 4–6, all under the **vendored, unmodified** subject:

```
case-studies/spec-driven-petclinic/fixtures/petclinic/appointment-scheduling-spec-with-usecases/
  spec/smart-appointment-scheduling/rules.md
  src/main/java/org/springframework/samples/petclinic/scheduling/service/StaffFallbackService.java
  src/main/java/org/springframework/samples/petclinic/scheduling/service/LifecycleProcessor.java
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

**Tab 1 · `EarsSliceTest.java`** → `theCancellationSliceIsFullyEstablished`

Point at each line as you say its sentence. **This is the most important 30 seconds in the talk.**

```java
EarsCriterion.from(CRITERIA)     →  "These are Anton's requirements, written before the code."
EarsJudge.create(...)            →  "This is the Agent Judge API."
JudgeAssertions.assertStatus(...)→  "And this is JUnit."
```

**Click the gutter arrow.** Expect green, and the runner showing:

```
6 requirements → PASS
```

Don't linger in the runner. The code is the visual.

## B3 · Explain the recording — 30 seconds, no more

Do this **once**, here, before anyone wonders how 52 requirements got evaluated in two seconds.

> **"These evaluations originally ran live through AgentClient. They take long enough that I'm not
> going to make you watch an AI think on stage. I committed the verbatim responses from those live
> runs and I replay them here."**
>
> **"Same `EarsJudge`, same parser, same classifier, same JUnit assertion. The only thing I swap is
> the `JudgeModel` — live AgentClient versus a recorded response."**

Point at:

```java
JudgeBackends.forRecording(workspace, "ears-uc6-cancellation")
```

Then move on. Tab 8 only if someone asks.

## B4 · Module 03 — scale to the whole document

**Tab 2 · `EarsUseCaseTest.java`** → `theCompleteSpecificationCannotBeEstablished`

> **"Same judge. This time I'm not selecting six — I'm running the whole document."**

**Run it.** Green, and the runner reads:

```
52 requirements → ABSTAIN: UC6-AC41 could not be established
```

### 🎙️ Now handle the green-versus-ABSTAIN point. Do not skip this.

> **"Green here means the evaluator returned the result I expected from this recorded case study.
> It is not saying the application is acceptable. In a real merge gate my policy would require
> PASS — that's `assertPass`."**

Then scroll to `theRosterIsCompleteAndTheOutcomeIsNotAccidental` and show:

```java
assertEquals(52, judgment.checks().size());
assertEquals(51L, established);
assertEquals(List.of("UC6-AC41"), unestablished);
assertNull(judgment.score());
```

### 🎙️ The line to land. Pause after it.

> **"PASS means I established every required criterion.
> I don't turn 51 out of 52 into 98% and call it done."**

### 🎙️ Then the transition nothing on screen gives you

> **"Behaviour isn't the only thing Anton specified."**

## B5 · Module 04 — same shape, different specification

**Tab 3 · `Rfc2119RulesTest.java`** → `theArchitecturalDesignIsViolated`

Point at the symmetry with Module 02 — that is the whole point of this tab:

```java
Rfc2119Constraint.from(RULES)
Rfc2119Judge.create(...)
JudgeAssertions.assertStatus(JudgmentStatus.FAIL, ...)
```

> **"Same implementation. Another document, written before the code by the same author."**

**Run it.** Green, runner reads:

```
13 architectural MUSTs → FAIL: 8 violated
```

Then show `theViolationsAreTheOnesTheDocumentNames` for the identifiers:

```
RULE-1  RULE-2  RULE-4  RULE-5  RULE-8  RULE-10  RULE-11  RULE-12
```

### 🎙️ Headline the engineering, not the arithmetic

**Do not** make `5 PASS / 8 FAIL` the headline.

> **"The behavioural spec mostly held. The architecture evaluation found eight violations —
> in locking, transaction boundaries, time handling, authorization, what's allowed across a
> boundary, idempotency, and build consistency."**
>
> **"The build was green. 290 tests passed. And the specification still surfaced these."**

> ⚠️ Say the themes as a *list of areas*, not as a claim that each maps to exactly one rule.
> That mapping has not been verified rule by rule.

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

**Run it.** Green, runner reads:

```
RULE-4 investigation → CONFIRMED and REACHABLE
```

> **"The first judge found the violation. The investigation found the reachable counterparty path."**

Optional, and it is what makes the second tier more than an echo:

> **"It was allowed to de-escalate this. It didn't."**

> ⛔ **Do NOT say the deadlock was reproduced.** The recording reports an H2 reproduction; that was
> deliberately not hand-verified and the talk does not need it. Omit unless asked.
>
> The line-number movement (`:247` → `:248`) is a **supporting observation, not the headline.**
> The headline is the opposing reachable lock orders.

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

Leave Module 05's green run on screen.

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

| Module | Runner must show | Verdict |
|---|---|---|
| 02 | `6 requirements → PASS` | PASS |
| 03 | `52 requirements → ABSTAIN: UC6-AC41 could not be established` | ABSTAIN |
| 04 | `13 architectural MUSTs → FAIL: 8 violated` | FAIL |
| 05 | `RULE-4 investigation → CONFIRMED and REACHABLE` | CONFIRMED |

**All four JUnit tests are green.** Green means the evaluator returned the expected result — it does
*not* mean the application is acceptable. That distinction is B4's job to explain.

**Normal, not errors:** a `spring-javaformat` line about `AccountBootstrapRunner.java` if the
candidate re-materializes (harmless provenance — let it finish, don't explain it unless asked).

---

# Timing

| | |
|---|---|
| Module 01 | spoken only, ~30s |
| Module 02 + recording explanation | ~2½ min |
| Module 03 | ~2 min |
| Module 04 | ~2 min |
| RULE-4 + the two code paths | ~2 min |
| Module 05 | ~2 min |
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
tag     conference-library-consuming
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
