# Step 2.3: Module 05, the definition of done for the small change

## The output

```
  REQUIREMENT          ORACLE     STATUS
  -------------------- ---------- ------
  build-and-tests      known      PASS
  coverage-preserved   measured   PASS
  follows-conventions  judgment   FAIL

  done: false
```

Three requirements, three kinds of oracle, every row visible, 21.1s. The question that follows the
table needs no aggregation rule to answer: would you merge this?

## The failing criterion is real, and that matters

`follows-conventions` fails because the agent did not add a `ClinicServiceTests` case for its new
repository finder and omitted the whitespace handler tests whose behaviour it copied. Those are
findings module 04 produced by reading the codebase, cited with file and line, and I verified them.

Nothing here is a manufactured failure. The alternative, arranging for a green table and describing
what a failure would look like, would have been much weaker, and it would have been dishonest about
a change that genuinely has a gap.

It also means the arc lands correctly: modules 01 and 02 pass, and the criterion that fails is the
one no exit code could have answered.

## Composition, not voting

`isDone` is a conjunction over `status() == PASS`, guarded on emptiness. Three deliberate
properties:

1. **Only PASS counts.** ABSTAIN and ERROR are not quiet successes. A judge that could not run has
   not approved anything.
2. **No aggregation.** The module prints `2 of 3 passed = 0.67` only to reject it, and says why: a
   mean is compensatory, and "the tests pass" cannot offset "it does not follow the conventions it
   was asked to follow".
3. **The emptiness guard is load-bearing.** `allMatch` over an empty collection is `true`, so a
   definition of done that lost its requirements would report done. The module runs that case and
   shows it returning false.

## Findings

1. **No `ConjunctiveStrategy` exists in Agent Judge 0.15.2**, so the composition is eight lines of
   tutorial code. That is fine for teaching, and it is the second time this gap has been recorded.
   A criteria-set abstraction that keeps every requirement visible and refuses to vote is the
   clearest product follow-up from this work.

2. **The three requirements have genuinely different costs**: 18s of real build, an instant read of
   an already-produced report, and a replayed judgment that costs 105s live. A definition of done is
   not a uniform thing, and cost-tiering it is what a cascade would be for.

3. **The recorded architecture judgment makes this module deterministic.** Live, `follows-conventions`
   flips about one run in three, which would make the headline table unstable on stage. Recorded
   replay of a real run keeps the demo honest and repeatable at once.

## Verification

| Exit criterion | Result |
|---|---|
| One failed requirement causes overall not-ready while staying visible | Yes |
| No numeric averaging across unrelated criteria | Correct, shown and rejected |
| Each individual status and its evidence preserved | Yes, reasoning and failed checks printed |
| Closing transition to the large candidate | Yes |
| Output fits 80 columns | Yes, 0 over |
| Runtime | 21.1s |
| Integration test | Passes, 10 of 10 required strings |

---

## Amendment: the hand-written conjunction is now a library strategy

`agent-judge` added `AllMustPassStrategy` after this module demonstrated the gap by writing the
conjunction by hand. The module now uses it, and the module got better rather than shorter.

`ConjunctiveStrategy`, added first, did **not** close the gap. It is threshold-based, and all three
requirements here carry no score: `BuildSuccessJudge`, `CoveragePreservationJudge` and the
architectural classifier all return status only. Applying it would fall through `effectiveScore()`
to 1.0 for PASS and 0.0 for FAIL and compare that to a bar, which is the duplicate-a-Boolean
anti-pattern this tutorial rejects three modules earlier. What a definition of done needs is
conjunction over **status**, which is what `AllMustPassStrategy` does.

### The demonstration is stronger than the workaround was

The same three judges now run through two aggregation rules:

```
  MajorityVotingStrategy   PASS
  AllMustPassStrategy      FAIL
```

Same judges, same evidence, opposite answers, and only the combining rule differs. That is a
sharper statement of the module's thesis than the `2 of 3 = 0.67` arithmetic alone, because it is
two real library strategies disagreeing rather than a number being described as wrong.

### The empty case moved into the library, and the demonstration had to change with it

The guard is now `AllMustPassStrategy`'s. Demonstrating it needs care:

| Call | Result |
|---|---|
| `aggregate(List.of(), ...)` | **throws** `Cannot aggregate empty judgment list` |
| `aggregate([ABSTAIN, ABSTAIN], ...)` | `ABSTAIN` |

Both are defensible. Zero judges configured is a programming error; a panel that abstained its way
to an empty eligible population is a runtime condition. The module demonstrates the second, which is
the realistic one, and the Javadoc sentence "with nothing eligible the result is ABSTAIN, never
PASS" is about the second case rather than the first. Worth knowing before someone reads that
sentence and calls the first.

### Verified

Full reactor green, 15 unit tests, module 05 integration passing on 12 required strings, output
still 0 lines over 80 columns.
