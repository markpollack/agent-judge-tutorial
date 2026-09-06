# Rubric stability: the judge is now repeatable, and it is measured

## The problem

Left to choose its own criteria, the architectural-conformance judge disagreed with itself. It
agreed on the **facts** every run and disagreed on how to **carve** them:

| | |
|---|---|
| criteria per run | 5, 6, 5 |
| criterion names shared by all three runs | **0** |
| distinct names across three runs | 16 |
| verdicts on identical input | 2 PASS, 2 FAIL (n=4) |

One fact, three carvings, two outcomes. No `ClinicServiceTests` case for the new finder became
`persistence-test-parity` and **failed** in one run, was absorbed into `test-coverage` and **passed**
in another, and into `test-convention` and **passed** in a third.

The verdict followed the carving, not the evidence. This was a design error of mine: I asked the
model to invent the criteria, apply them, and roll them up, and only the first of those is unstable.

## The fix, in two parts

**1. Supply the criteria.** Five named conformance criteria, fixed in `CRITERIA`, derived from what
the runs converged on semantically. The model assesses exactly those, and is told not to add, merge,
or skip one.

**2. Take the verdict away from the model.** The prompt says outright: *"Do NOT state an overall
verdict. You assess each criterion; deciding what the set of assessments means is not your job."*
The classifier builds one `Judgment` per criterion and rolls them up with `AllMustPassStrategy`.

That second half is module 05's lesson applied one level down. The model estimates; the gate decides.
The gate is deterministic, inspectable, and in Java.

## Measured result

Five consecutive live runs, same fixture, same prompt:

```
  run 1: verdict=FAIL  failing=1  [repository-query,controller-handler,search-form,web-tests,persistence-tests]
  run 2: verdict=FAIL  failing=1  [repository-query,controller-handler,search-form,web-tests,persistence-tests]
  run 3: verdict=FAIL  failing=1  [repository-query,controller-handler,search-form,web-tests,persistence-tests]
  run 4: verdict=FAIL  failing=1  [repository-query,controller-handler,search-form,web-tests,persistence-tests]
  run 5: verdict=FAIL  failing=1  [repository-query,controller-handler,search-form,web-tests,persistence-tests]
```

| | before | after |
|---|---|---|
| criteria set | 5/6/5, zero overlap | identical, 5 of 5 runs |
| verdict on identical input | 2 PASS / 2 FAIL | 5 FAIL / 5 |
| runtime | 24 to 117s | about 24s |

**5 of 5 identical, including which criterion binds.** This is live-demo safe in a way the previous
version was not.

⚠️ Honest limits. n=5 is small, five identical runs do not prove determinism, and the fixture is one
change. What the measurement supports is that the instability had a cause, the cause was the moving
criteria set, and removing it removed the observed variance.

## Roster guard, watched red

A criterion the model skips is not a criterion that passed. If any of the five is missing from the
reply the classifier returns ERROR naming the missing ones, rather than rolling up four.

Verified by deleting the `persistence-tests` line from the recording: the module returned **ERROR**,
not a PASS over the remaining four. Without that guard an omission silently shrinks the denominator
and improves the result.

## The tension this exposes, unresolved

Module 04's premise is that the judge investigates and finds the convention **itself**, so it cannot
be handed a fixed criteria set without destroying what the module is for. Module 04 is therefore
still free to carve, and still unstable.

That is arguably correct rather than a defect, and it suggests the two modules teach two phases:

```
  discovery    once, offline, unstable by nature   ->  propose the rubric
  assessment   repeatable, stable                  ->  judge against the frozen rubric
```

Which is the ArchUnit story one notch less deterministic: the first time AI discovers the rule, after
that the rule is fixed and only the assessment is AI. **But the current module order teaches
assessment first and discovery second**, which is backwards relative to that lesson. Recorded as a
consolidation question for Step 5.0 rather than resolved here.

## Product follow-up, unchanged and reinforced

F2 remains the highest-value gap. `ClaudeAgentOptions` supports `jsonSchema`;
`AgentClientJudgeModel` does not expose it. A schema would enforce the fixed criteria set at the
protocol level instead of by asking politely in a prompt and parsing free text with a tolerant
regex. This fix works, and it works by persuasion.
