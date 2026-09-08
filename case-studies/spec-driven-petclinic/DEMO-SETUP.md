# DEMO-SETUP — operator checklist

Execute top to bottom immediately before recording. No interpretation required.

Narrative explanation of *why* lives in `README.md` § Before the demo. **This file is the
authoritative command sequence.** If the two ever disagree, this one is right.

Frozen conference checkpoint: **`2b1e7c5`** — "Module 05: a failed requirement is an address, not
yet a consequence". Also tagged `conference-known-good-2b1e7c5`.

**The conference path is modules 01–05.** Module 05 was promoted from optional to part of the path
by the Project Owner's `05-AJT` scope decision; module 06 is deliberately not implemented and is a
spoken conclusion only.

Modules 01–04's verdicts were frozen at `b72e74d` and have not moved since. Everything after it is
additive or presentation-only: one space of alignment on three lines of Module 02, this document,
and module 05, which changes nothing in the four modules before it. **Modules 01, 03 and 04 replay
byte-identically to `b72e74d`.**

---

## Before recording

```
[ ]  1. Verify the conference checkpoint is an ancestor of HEAD
[ ]  2. Confirm git status is clean
[ ]  3. Confirm ANTHROPIC_API_KEY is unset
[ ]  4. Confirm offline Maven mode is usable
[ ]  5. Pre-materialize the PetClinic candidate      <-- do not skip
[ ]  6. Confirm materialization completed
[ ]  7. Warm the candidate build
[ ]  8. Run Module 01 — confirm PASS
[ ]  9. Run Module 02 — confirm 6 PASS / Overall PASS
[ ] 10. Run Module 03 — confirm 51 PASS / 0 FAIL / 1 ABSTAIN, binding = UC6-AC41
[ ] 11. Run Module 04 — confirm 5 PASS / 8 FAIL / Overall FAIL
[ ] 12. Run Module 05 — confirm RULE-4 CONFIRMED / REACHABLE
[ ] 13. Return terminal to the repository root
[ ] 14. Clear the terminal
```

### 1–4. Preconditions

```bash
cd ~/projects/agent-judge-tutorial

git merge-base --is-ancestor ea5e12d HEAD && echo "checkpoint OK" || echo "STOP: not on the conference path"
git status --porcelain                       # expect NO output
echo "ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY:-<unset>}"   # expect <unset>
./mvnw -o -f case-studies/spec-driven-petclinic/pom.xml install -DskipTests -q && echo "offline build OK"
```

If `ANTHROPIC_API_KEY` is set: `unset ANTHROPIC_API_KEY`. Every module replays a committed
recording; no key and no network are needed, and an unset key proves it on stage.

### 5–6. Pre-materialize — the step that must not be skipped

```bash
( cd case-studies/spec-driven-petclinic/fixtures/petclinic && ./materialize-large-candidate.sh )
```

**Expect this on stderr, here, before recording:**

```
note: applied spring-javaformat to 1 file(s) the generated code left non-conforming
      src/main/java/org/springframework/samples/petclinic/security/AccountBootstrapRunner.java
```

That message is true provenance and it is **not** what the case study teaches. Seeing it *now* is
the whole point: on a cold clone it appears during Module 01, immediately before the first output of
the talk. It is not suppressed in code, because suppressing that stream would also hide genuine
materialization failures.

Confirm the script printed the candidate path and exited 0.

### 7. Warm the build

```bash
( cd case-studies/spec-driven-petclinic/fixtures/petclinic/build/large-candidate && ./mvnw -o -q test )
```

Module 01 runs a real build. Warming means the stage run is the build, not a first compile.

### 8–12. The conference sequence

Run each once now, and confirm the expected result before recording.

```bash
cd ~/projects/agent-judge-tutorial

# 01  Does it build?
./mvnw -q -o -f case-studies/spec-driven-petclinic/pom.xml exec:java -pl module-01-build

# 02  Run six requirements
./mvnw -q -o -f case-studies/spec-driven-petclinic/pom.xml exec:java -pl module-02-ears-slice

# 03  Run all 52
./mvnw -q -o -f case-studies/spec-driven-petclinic/pom.xml exec:java -pl module-03-ears-usecase

# 04  Run architectural MUSTs
./mvnw -q -o -f case-studies/spec-driven-petclinic/pom.xml exec:java -pl module-04-rfc2119-rules
```

```bash
# 05  Investigate one failure
./mvnw -q -o -f case-studies/spec-driven-petclinic/pom.xml exec:java -pl module-05-investigation
```

Expect `CONFIRMED` / `REACHABLE`, four cited locations, and a closing block showing that the
investigation moved the address from `StaffFallbackService.java:247` to `:248`. Replays offline in
under two seconds, like modules 02–04.

**If time runs short, module 05 is the one to drop.** Modules 01–04 stand alone and its absence
costs the arc nothing structural — but it is the module that keeps module 04's closing promise, so
drop it only under real time pressure.

### 13–14. Reset

```bash
cd ~/projects/agent-judge-tutorial
clear
```

---

## Expected results — check every line

```
module 01   build   PASS
module 02   6 of 6                          Overall: PASS
module 03   51 PASS · 0 FAIL · 1 ABSTAIN    Overall: ABSTAIN    binding: UC6-AC41
module 04   5 PASS · 8 FAIL                 Overall: FAIL

module 05   RULE-4  CONFIRMED · REACHABLE      address moved :247 -> :248
```

Module 04's eight failures, in display order:
`RULE-1 · RULE-2 · RULE-4 · RULE-5 · RULE-8 · RULE-10 · RULE-11 · RULE-12`

**If any line differs, do not present. Return to `2b1e7c5`.**

## Expected timings

| Step | Measured |
|---|---|
| `install -DskipTests` | 2.3s |
| `materialize-large-candidate.sh` | 3.1s |
| warm `mvnw -o -q test` | 39.0s |
| module 01 (warm) | **35–45s** |
| module 02 | 1.8s |
| module 03 | 1.9s |
| module 04 | 1.8s |

Measured 2026-09-07, warm, offline, key unset; module 01 re-measured 2026-09-08 at 44.4s, hence the
range. Module 01 is slow because it runs a **real** Maven build and the candidate's real test suite
— that is the point of it. Narrate over it; the line *"this takes about a minute"* is already
printed.

---

## The four transitions

```
01 → 02   It builds. But the build has no opinion about whether it did what was asked.

02 → 03   Those six looked good. Now run the entire specification.

03 → 04   Behaviour isn't the only thing Anton specified.

04 close  Same generated system. Another document written before the code.
          A different answer.
```

**Three of these four are printed on both sides of the seam; one is not.** Know which, because it
changes what you have to say out loud.

| Transition | Printed by the module that ends | Printed by the module that starts |
|---|---|---|
| 01 → 02 | yes — *"Somebody did write down what was asked for… Module 02 reads it."* | yes |
| 02 → 03 | yes — *"Six is readable. Module 03 runs all 52."* | yes |
| 03 → 04 | **no** | yes — *"The behavioural specification mostly held. But the same author also wrote architectural requirements…"* |
| 04 close | yes | — |

**Module 03 deliberately prints no forward pointer.** It ends on

```
      PASS means I established every required criterion. I don't turn
      51 out of 52 into 98% and call it done.
```

which is the strongest line in the arc, and it is meant to land on silence. Do not expect a bridge
to Module 04 on screen — **say the 03 → 04 transition out loud**, then run Module 04, whose opening
lines pick it up. This is the one seam where a missed line is not recovered by the terminal.

---

## If something goes wrong

**Module 01 materializes again on stage.**
Let it finish. Do not explain the formatting note unless asked. It costs about six seconds.

**A module tries to run live.**
Stop and re-run the exact offline command from § 8–12 above. Every module replays a committed
recording; live is opt-in via `AGENT_JUDGE_TUTORIAL_AGENT=live`, and a live UC6 audit takes about
fourteen minutes — never a stage activity.

**Terminal state is uncertain.**
`cd ~/projects/agent-judge-tutorial`, then use the exact command from § 8–12.

**Any module output differs from the expected results above.**
Do not improvise a fix while recording. Stop and return to `2b1e7c5`.
